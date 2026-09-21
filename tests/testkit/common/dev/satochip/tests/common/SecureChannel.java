package dev.satochip.tests.common;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.security.interfaces.ECPublicKey;

/**
 * Client side of the Satochip / Satodime / Seedkeeper secure channel.
 *
 * <p>Handshake (INS 0x81) returns the card ephemeral public key as X coordinate plus a
 * self-signature. Session keys are derived exactly like the applets do:
 * {@code Z = X(shared point)}, {@code macKey = HMAC-SHA1(Z, "sc_mac")},
 * {@code sessionKey = HMAC-SHA1(Z, "sc_key")[0..16]}.
 *
 * <p>Wrapped commands (INS 0x82) are framed as
 * {@code IV(16) | ciphertext_size(2) | AES-CBC-PKCS7(cmd) | mac_size(2)=20 | HMAC-SHA1(IV|size|ct)}.
 * Responses are {@code IV(16) | ciphertext_size(2) | AES-CBC-PKCS7(data)} with no MAC.
 */
public final class SecureChannel {

    private static final byte[] CST_SC = "sc_keysc_mac".getBytes(StandardCharsets.US_ASCII);

    private final Card card;
    private final byte[] macKey;
    private final byte[] sessionKey;
    private final SecureRandom random = new SecureRandom();
    private int counter = 1;

    private SecureChannel(Card card, byte[] macKey, byte[] sessionKey) {
        this.card = card;
        this.macKey = macKey;
        this.sessionKey = sessionKey;
    }

    public static SecureChannel establish(Card card) {
        KeyPair kp = EcUtil.generateKeyPair();
        byte[] clientPub = EcUtil.encodeUncompressed((ECPublicKey) kp.getPublic());

        Response r = card.transmit(Apdu.case3(Card.CLA, 0x81, 0x00, 0x00, clientPub));
        r.requireSuccess();
        byte[] resp = r.data();
        if (resp.length < 36) {
            throw new IllegalStateException("Secure channel response too short: " + r);
        }

        byte[] signedMessage = Hex.slice(resp, 0, 34); // 0x0020 || X
        byte[] x = Hex.slice(resp, 2, 32);
        int sigSize = Hex.getShort(resp, 34);
        if (resp.length < 36 + sigSize) {
            throw new IllegalStateException("Truncated self-signature: " + r);
        }
        byte[] selfSig = Hex.slice(resp, 36, sigSize);

        ECPublicKey cardEphemeral = EcUtil.recoverFromXAndSignature(x, signedMessage, selfSig);
        byte[] z = EcUtil.ecdhX(kp.getPrivate(), cardEphemeral);

        byte[] macKey = CryptoUtil.hmacSha1(z, Hex.slice(CST_SC, 6, 6));   // "sc_mac"
        byte[] session = CryptoUtil.hmacSha1(z, Hex.slice(CST_SC, 0, 6)); // "sc_key"
        return new SecureChannel(card, macKey, Hex.slice(session, 0, 16));
    }

    /** Wraps and sends an inner command, returning the decrypted inner response. */
    public Response send(int ins, int p1, int p2, byte[] data) {
        Response outer = card.transmit(Apdu.case3(Card.CLA, 0x82, 0x00, 0x00, frame(ins, p1, p2, data)));
        if (outer.sw != 0x9000) {
            return outer;
        }
        return decrypt(outer.data());
    }

    /** Builds a fully framed 0x82 payload (for negative / tampering tests). */
    public byte[] frame(int ins, int p1, int p2, byte[] data) {
        byte[] inner = Apdu.command(Card.CLA, ins, p1, p2, data);
        byte[] padded = CryptoUtil.pkcs7Pad(inner, 16);

        byte[] iv = new byte[16];
        random.nextBytes(iv);
        iv[12] = (byte) (counter >>> 24);
        iv[13] = (byte) (counter >>> 16);
        iv[14] = (byte) (counter >>> 8);
        iv[15] = (byte) counter;
        counter += 2;

        byte[] ct = CryptoUtil.aesCbcEncrypt(sessionKey, iv, padded);
        byte[] macInput = CryptoUtil.concat(iv, Hex.setShort(ct.length), ct);
        byte[] mac = CryptoUtil.hmacSha1(macKey, macInput);
        return CryptoUtil.concat(macInput, Hex.setShort(20), mac);
    }

    public Response send(int ins, int p1, int p2) {
        return send(ins, p1, p2, new byte[0]);
    }

    /** Sends an already-framed 0x82 payload (used by negative tests). */
    public Response sendRawFrame(byte[] frame) {
        return card.transmit(Apdu.case3(Card.CLA, 0x82, 0x00, 0x00, frame));
    }

    public byte[] macKey() {
        return macKey;
    }

    public byte[] sessionKey() {
        return sessionKey;
    }

    private Response decrypt(byte[] data) {
        if (data.length == 0) {
            return new Response(new byte[0], 0x9000);
        }
        if (data.length < 18) {
            throw new IllegalStateException("Secure channel response too short: " + Hex.encode(data));
        }
        byte[] iv = Hex.slice(data, 0, 16);
        int size = Hex.getShort(data, 16);
        byte[] ct = Hex.slice(data, 18, size);
        byte[] plain = CryptoUtil.aesCbcDecrypt(sessionKey, iv, ct);
        return new Response(CryptoUtil.pkcs7Unpad(plain), 0x9000);
    }
}
