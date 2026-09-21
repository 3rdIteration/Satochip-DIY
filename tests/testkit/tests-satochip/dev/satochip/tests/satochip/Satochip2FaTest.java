package dev.satochip.tests.satochip;

import dev.satochip.tests.common.CryptoUtil;
import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Satochip2FaTest extends AbstractSatochipTest {

    private static final byte[] HMAC_KEY = new byte[20];
    private static final byte[] LIMIT = new byte[8];
    private static final byte[] CST_2FA = "id_2FAkey_2FA".getBytes(StandardCharsets.US_ASCII);

    static {
        for (int i = 0; i < HMAC_KEY.length; i++) {
            HMAC_KEY[i] = (byte) (0xA0 + i);
        }
        // limit = 0x0000000000001000
        LIMIT[6] = 0x10;
    }

    @BeforeEach
    void setupWith2Fa() {
        setup(PIN, PUK, DEFAULT_PIN, 0x0800, 0x8000, HMAC_KEY, LIMIT).requireSuccess();
        verifyPinOk(PIN);
    }

    @Test
    void statusReportsTwoFactor() {
        Response r = status().requireSuccess();
        assertEquals(1, r.data[8] & 0xFF, "needs_2FA");
    }

    @Test
    void set2FaKeyAgainRejected() {
        Response r = sc.send(0x79, 0x00, 0x00, new byte[28]);
        assertEquals(0x9C18, r.sw);
    }

    @Test
    void resetTwoFactorWithWrongHmacRejected() {
        assertEquals(0x9C0B, sc.send(0x78, 0x00, 0x00, new byte[20]).sw);
    }

    @Test
    void resetTwoFactorWithCorrectHmac() {
        byte[] id2fa = CryptoUtil.hmacSha1(HMAC_KEY, Hex.slice(CST_2FA, 0, 6));
        byte[] msg = new byte[64];
        Arrays.fill(msg, (byte) 0xAA);
        System.arraycopy(id2fa, 0, msg, 0, 20);
        byte[] hmac = CryptoUtil.hmacSha1(HMAC_KEY, msg);

        sc.send(0x78, 0x00, 0x00, hmac).requireSuccess();
        Response r = status().requireSuccess();
        assertEquals(0, r.data[8] & 0xFF, "needs_2FA should be cleared");
    }

    @Test
    void cryptTransactionRoundTrip() {
        // init encrypt must return IV(16) || id_2FA(20)
        Response init = sc.send(0x76, 0x02, 0x01, new byte[0]).requireSuccess();
        assertEquals(36, init.data.length);

        // finalize with one AES block
        byte[] chunk = new byte[16];
        for (int i = 0; i < chunk.length; i++) {
            chunk[i] = (byte) i;
        }
        byte[] data = new byte[2 + chunk.length];
        data[0] = 0x00;
        data[1] = (byte) chunk.length;
        System.arraycopy(chunk, 0, data, 2, chunk.length);

        Response fin = sc.send(0x76, 0x02, 0x03, data).requireSuccess();
        assertEquals(2 + 16, fin.data.length);
        assertEquals(16, Hex.getShort(fin.data, 0));
    }

    @Test
    void cryptTransactionWithWrongDirectionRejected() {
        Response r = sc.send(0x76, 0x00, 0x01, new byte[0]);
        assertTrue(r.sw != 0x9000);
    }
}
