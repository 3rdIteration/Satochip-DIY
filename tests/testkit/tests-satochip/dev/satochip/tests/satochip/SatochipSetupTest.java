package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatochipSetupTest extends AbstractSatochipTest {

    @Test
    void setupSucceedsAndIsIdempotentGuard() {
        setupDefault().requireSuccess();
        Response again = setup(PIN, PUK, DEFAULT_PIN, 0x0800, 0x0000, null, null);
        assertEquals(SW_SETUP_ALREADY_DONE, again.sw);
    }

    @Test
    void setupWithWrongDefaultPinFails() {
        byte[] wrongDefault = "WrongPin".getBytes();
        Response r = setup(PIN, PUK, wrongDefault, 0x0800, 0x0000, null, null);
        assertTrue((r.sw & 0xFFF0) == 0x63C0, "expected PIN failed SW, got " + Integer.toHexString(r.sw));
    }

    @Test
    void setupWithTooShortPinFails() {
        byte[] shortPin = "123".getBytes();
        Response r = setup(shortPin, PUK, DEFAULT_PIN, 0x0800, 0x0000, null, null);
        assertEquals(SW_INVALID_PARAMETER, r.sw);
    }

    @Test
    void setUpWithTwoFactorOption() {
        byte[] hmacKey = new byte[20];
        for (int i = 0; i < 20; i++) {
            hmacKey[i] = (byte) i;
        }
        byte[] limit = new byte[8];
        Response r = setup(PIN, PUK, DEFAULT_PIN, 0x0800, 0x8000, hmacKey, limit);
        r.requireSuccess();
        Response status = status().requireSuccess();
        assertEquals(1, status.data[8], "needs_2FA should be set");
    }
}
