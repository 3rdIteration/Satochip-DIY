package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatochipPinTest extends ReadySatochipTest {

    @Test
    void wrongPinThenCorrectPin() {
        Response wrong = verifyPin("WrongPin".getBytes());
        assertTrue((wrong.sw & 0xFFF0) == 0x63C0, "expected PIN failed, got " + Integer.toHexString(wrong.sw));
        verifyPinOk(PIN);
    }

    @Test
    void wrongP2Rejected() {
        Response r = sc.send(0x42, 0x00, 0x01, PIN);
        assertEquals(SW_INCORRECT_P2, r.sw);
    }

    @Test
    void pinLocksAfterThreeFailuresAndUnblocksWithPuk() {
        for (int i = 0; i < 5; i++) {
            verifyPin("WrongPin".getBytes());
        }
        assertEquals(SW_IDENTITY_BLOCKED, verifyPin(PIN).sw);

        Response wrongPuk = sc.send(0x46, 0x00, 0x00, "WrongPuk".getBytes());
        assertTrue((wrongPuk.sw & 0xFFF0) == 0x63C0, "expected PUK failed, got " + Integer.toHexString(wrongPuk.sw));

        sc.send(0x46, 0x00, 0x00, PUK).requireSuccess();
        verifyPinOk(PIN);
    }

    @Test
    void unblockOnNonBlockedPinIsRejected() {
        assertEquals(0x9C03, sc.send(0x46, 0x00, 0x00, PUK).sw);
    }

    @Test
    void changePin() {
        byte[] data = new byte[2 + PIN.length + PIN2.length];
        data[0] = (byte) PIN.length;
        System.arraycopy(PIN, 0, data, 1, PIN.length);
        data[1 + PIN.length] = (byte) PIN2.length;
        System.arraycopy(PIN2, 0, data, 2 + PIN.length, PIN2.length);
        sc.send(0x44, 0x00, 0x00, data).requireSuccess();

        // old PIN no longer valid
        assertTrue((verifyPin(PIN).sw & 0xFFF0) == 0x63C0);
        verifyPinOk(PIN2);

        // change back
        byte[] back = new byte[2 + PIN2.length + PIN.length];
        back[0] = (byte) PIN2.length;
        System.arraycopy(PIN2, 0, back, 1, PIN2.length);
        back[1 + PIN2.length] = (byte) PIN.length;
        System.arraycopy(PIN, 0, back, 2 + PIN2.length, PIN.length);
        sc.send(0x44, 0x00, 0x00, back).requireSuccess();
        verifyPinOk(PIN);
    }

    @Test
    void logoutAllRequiresReauthentication() {
        sc.send(0x60, 0x00, 0x00).requireSuccess();
        Response r = sc.send(0x3D, 0x00, 0x01, new byte[0]);
        assertEquals(SW_UNAUTHORIZED, r.sw);
    }
}
