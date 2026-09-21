package dev.satochip.tests.integration;

import dev.satochip.tests.common.Card;
import dev.satochip.tests.common.CryptoUtil;
import dev.satochip.tests.common.EcUtil;
import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import dev.satochip.tests.common.SecureChannel;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * End-to-end Seedkeeper -> Satochip secure import.
 *
 * <p>This exercises the pairing handshake (authentikey exchange), the Seedkeeper secure export
 * (INS 0xA8) and the Satochip trusted-pubkey + encrypted-secret import (INS 0xAA / 0xAC). It is a
 * full regression for the THD-89 {@code trusted_pubkey} allocation bug.
 */
class SeedkeeperSatochipPairingTest {

    private static final String SATO_AID = "5361746F4368697000";
    private static final String SEED_AID = "536565644b656570657200";
    private static final String SATO_CLASS = "org.satochip.applet.CardEdge";
    private static final String SEED_CLASS = "org.seedkeeper.applet.SeedKeeper";

    private static final byte[] PIN = "12345678".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] PUK = "87654321".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] DEFAULT_PIN = "Muscle00".getBytes(StandardCharsets.US_ASCII);

    private static final int EXPORT_ALLOWED = 1;
    private static final int EXPORT_SECUREONLY = 2;
    private static final int OP_INIT = 1;
    private static final int OP_FINALIZE = 3;
    private static final int SECRET_HEADER_SIZE = 13;
    private static final int TYPE_MASTER_SEED = 0x10;
    private static final int TYPE_PUBKEY = 0x70;

    @Test
    void seedkeeperSecureImportIntoOfficialOrThdSatochip() {
        Card card = new Card();
        card.install(SATO_AID, SATO_CLASS, null);
        card.install(SEED_AID, SEED_CLASS, new byte[]{0x0F, (byte) 0xFF});

        // --- Seedkeeper: setup, PIN, authentikey ---
        card.select(SEED_AID).requireSuccess();
        SecureChannel scK = SecureChannel.establish(card);
        scK.send(0x2A, 0x00, 0x00, setupData()).requireSuccess();
        scK.send(0x42, 0x00, 0x00, PIN).requireSuccess();
        byte[] seedAuth = recoverPubkey(scK.send(0xAD, 0x00, 0x00).requireSuccess().data);

        // --- Satochip: setup, PIN, authentikey ---
        card.select(SATO_AID).requireSuccess();
        SecureChannel scS = SecureChannel.establish(card);
        scS.send(0x2A, 0x00, 0x00, setupData()).requireSuccess();
        scS.send(0x42, 0x00, 0x00, PIN).requireSuccess();
        byte[] satoAuth = recoverPubkey(scS.send(0xAD, 0x00, 0x00).requireSuccess().data);

        // --- Seedkeeper: register Satochip authentikey, generate and export a seed ---
        card.select(SEED_AID).requireSuccess();
        scK = SecureChannel.establish(card);
        scK.send(0x42, 0x00, 0x00, PIN).requireSuccess();

        byte[] pubkeyId = importPubkey(scK, satoAuth);
        byte[] seedId = generateMasterseed(scK, 32);
        byte[] blob = scK.send(0xA8, 0x00, 0x00, CryptoUtil.concat(seedId, pubkeyId))
                .requireSuccess().data;

        // --- Satochip: import Seedkeeper authentikey and the encrypted seed ---
        card.select(SATO_AID).requireSuccess();
        scS = SecureChannel.establish(card);
        scS.send(0x42, 0x00, 0x00, PIN).requireSuccess();

        // This is the operation that used to fail with 0x6F00 on the THD-89 build.
        scS.send(0xAA, 0x00, 0x00, trustedPubkeyData(seedAuth)).requireSuccess();

        byte[] payload = reformatForSatochip(blob);
        scS.send(0xAC, 0x00, 0x00, payload).requireSuccess();

        Response status = card.send(0x3C, 0x00, 0x00).requireSuccess();
        assertEquals(1, status.data[9] & 0xFF, "Satochip should be seeded after secure import");
    }

    private static byte[] setupData() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(DEFAULT_PIN.length);
        out.write(DEFAULT_PIN, 0, DEFAULT_PIN.length);
        out.write(5);
        out.write(3);
        out.write(PIN.length);
        out.write(PIN, 0, PIN.length);
        out.write(PUK.length);
        out.write(PUK, 0, PUK.length);
        out.write(5);
        out.write(3);
        out.write(PIN.length);
        out.write(PIN, 0, PIN.length);
        out.write(PUK.length);
        out.write(PUK, 0, PUK.length);
        for (int i = 0; i < 7; i++) {
            out.write(0);
        }
        out.write(0);
        out.write(0);
        return out.toByteArray();
    }

    private static byte[] importPubkey(SecureChannel scK, byte[] pubkey) {
        byte[] body = new byte[1 + pubkey.length];
        body[0] = 65;
        System.arraycopy(pubkey, 0, body, 1, pubkey.length);
        int padded = 80;

        byte[] header = new byte[SECRET_HEADER_SIZE];
        header[0] = (byte) TYPE_PUBKEY;
        header[1] = 0x01;
        header[2] = (byte) EXPORT_SECUREONLY;
        header[12] = 0x00;
        byte[] init = CryptoUtil.concat(header, Hex.setShort(padded));
        scK.send(0xA1, EXPORT_ALLOWED, OP_INIT, init).requireSuccess();

        byte[] fin = CryptoUtil.concat(Hex.setShort(body.length), body);
        Response r = scK.send(0xA1, EXPORT_ALLOWED, OP_FINALIZE, fin).requireSuccess();
        return Hex.slice(r.data, 0, 2);
    }

    private static byte[] generateMasterseed(SecureChannel scK, int size) {
        Response r = scK.send(0xA0, size, EXPORT_SECUREONLY, new byte[]{0x00}).requireSuccess();
        return Hex.slice(r.data, 0, 2);
    }

    private static byte[] recoverPubkey(byte[] authResponse) {
        byte[] signedMessage = Hex.slice(authResponse, 0, 34);
        byte[] x = Hex.slice(authResponse, 2, 32);
        int sigSize = Hex.getShort(authResponse, 34);
        byte[] sig = Hex.slice(authResponse, 36, sigSize);
        return EcUtil.encodeUncompressed(EcUtil.recoverFromXAndSignature(x, signedMessage, sig));
    }

    private static byte[] trustedPubkeyData(byte[] pubkey) {
        return CryptoUtil.concat(Hex.setShort(pubkey.length), pubkey);
    }

    /**
     * Seedkeeper export layout: [id(2) | header(13) | IV(16) | encSize(2) | enc | hmacSize(2) | hmac].
     * Satochip expects: [header(12) | IV(16) | encSize(2) | enc | hmacSize(1)=20 | hmac].
     * The label_size byte is dropped from the header and the hmac size shrinks to one byte.
     */
    private static byte[] reformatForSatochip(byte[] blob) {
        byte[] header12 = Hex.slice(blob, 2, 12);
        byte[] iv = Hex.slice(blob, 15, 16);
        int encSize = Hex.getShort(blob, 31);
        byte[] enc = Hex.slice(blob, 33, encSize);
        byte[] hmac = Hex.slice(blob, 35 + encSize, 20);
        return CryptoUtil.concat(header12, iv, Hex.setShort(encSize), enc, new byte[]{20}, hmac);
    }
}
