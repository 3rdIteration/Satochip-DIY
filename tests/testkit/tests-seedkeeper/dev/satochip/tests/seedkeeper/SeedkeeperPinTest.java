package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedkeeperPinTest extends ReadySeedkeeperTest {

    @Test
    void wrongPinThenCorrect() {
        Response wrong = verifyPin("WrongPin".getBytes());
        assertTrue((wrong.sw & 0xFFF0) == 0x63C0, "expected PIN failed, got " + Integer.toHexString(wrong.sw));
        verifyPinOk(PIN);
    }

    @Test
    void pinLocksAndUnblocksWithPuk() {
        for (int i = 0; i < 5; i++) {
            verifyPin("WrongPin".getBytes());
        }
        assertEquals(SW_IDENTITY_BLOCKED, verifyPin(PIN).sw);
        sc.send(0x46, 0x00, 0x00, PUK).requireSuccess();
        verifyPinOk(PIN);
    }

    @Test
    void changePin() {
        byte[] data = new byte[2 + PIN.length + PIN2.length];
        data[0] = (byte) PIN.length;
        System.arraycopy(PIN, 0, data, 1, PIN.length);
        data[1 + PIN.length] = (byte) PIN2.length;
        System.arraycopy(PIN2, 0, data, 2 + PIN.length, PIN2.length);
        sc.send(0x44, 0x00, 0x00, data).requireSuccess();

        assertTrue((verifyPin(PIN).sw & 0xFFF0) == 0x63C0);
        verifyPinOk(PIN2);
    }

    @Test
    void logoutRequiresReauth() {
        sc.send(0x60, 0x00, 0x00).requireSuccess();
        assertEquals(SW_UNAUTHORIZED, sc.send(0x3D, 0x00, 0x01, new byte[0]).sw);
        assertEquals(SW_UNAUTHORIZED, sc.send(0xA7, 0x00, 0x00).sw);
    }
}
