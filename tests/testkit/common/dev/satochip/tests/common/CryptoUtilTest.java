package dev.satochip.tests.common;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Known-answer tests for the crypto helpers, so a harness regression cannot silently
 * invalidate every secure-channel based test.
 */
class CryptoUtilTest {

    @Test
    void hmacSha1Rfc2202() {
        byte[] key = new byte[20];
        for (int i = 0; i < key.length; i++) {
            key[i] = 0x0b;
        }
        byte[] data = "Hi There".getBytes(StandardCharsets.US_ASCII);
        assertEquals("B617318655057264E28BC0B6FB378C8EF146BE00", Hex.encode(CryptoUtil.hmacSha1(key, data)));
    }

    @Test
    void hmacSha256Rfc4231() {
        byte[] key = new byte[20];
        for (int i = 0; i < key.length; i++) {
            key[i] = 0x0b;
        }
        byte[] data = "Hi There".getBytes(StandardCharsets.US_ASCII);
        assertEquals("B0344C61D8DB38535CA8AFCEAF0BF12B881DC200C9833DA726E9376C2E32CFF7",
                Hex.encode(CryptoUtil.hmacSha256(key, data)));
    }

    @Test
    void sha256Abc() {
        assertEquals("BA7816BF8F01CFEA414140DE5DAE2223B00361A396177A9CB410FF61F20015AD",
                Hex.encode(CryptoUtil.sha256("abc".getBytes(StandardCharsets.US_ASCII))));
    }

    @Test
    void pkcs7RoundTrip() {
        byte[] data = new byte[20];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        byte[] padded = CryptoUtil.pkcs7Pad(data, 16);
        assertEquals(32, padded.length);
        assertArrayEquals(data, CryptoUtil.pkcs7Unpad(padded));
    }

    @Test
    void aesCbcRoundTrip() {
        byte[] key = new byte[16];
        byte[] iv = new byte[16];
        for (int i = 0; i < 16; i++) {
            key[i] = (byte) i;
            iv[i] = (byte) (0x10 + i);
        }
        byte[] data = CryptoUtil.pkcs7Pad("secure channel test".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] ct = CryptoUtil.aesCbcEncrypt(key, iv, data);
        assertArrayEquals(data, CryptoUtil.aesCbcDecrypt(key, iv, ct));
    }

    @Test
    void secp256k1RecoveryRoundTrip() {
        java.security.KeyPair kp = EcUtil.generateKeyPair();
        java.security.interfaces.ECPublicKey pub = (java.security.interfaces.ECPublicKey) kp.getPublic();
        byte[] encoded = EcUtil.encodeUncompressed(pub);
        assertEquals(65, encoded.length);
        assertEquals(0x04, encoded[0] & 0xFF);
        // decoded point must match
        java.security.interfaces.ECPublicKey decoded = EcUtil.decode(encoded);
        assertEquals(pub.getW(), decoded.getW());
    }
}
