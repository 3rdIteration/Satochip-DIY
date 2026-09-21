package dev.satochip.tests.satodime;

import dev.satochip.tests.common.CryptoUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * On the contactless media the Satodime enforces the unlock counter + HMAC unlock code.
 */
class SatodimeNfcTest extends ReadySatodimeTest {

    private static final int META_LEN = UNLOCK_COUNTER + UNLOCK_CODE + 2 + 1 + 4 + 34 + 34;

    @BeforeEach
    void switchToContactless() {
        card.changeProtocol("T=CL,TYPE_A,T0");
    }

    @Test
    void wrongCounterRejected() {
        byte[] counter = Arrays.copyOf(unlockCounter, UNLOCK_COUNTER);
        counter[UNLOCK_COUNTER - 1] ^= (byte) 0xFF;
        byte[] data = new byte[META_LEN];
        System.arraycopy(counter, 0, data, 0, UNLOCK_COUNTER);
        assertEquals(SW_INCORRECT_UNLOCK_COUNTER, sc.send(0x52, 0x00, 0x00, data).sw);
    }

    @Test
    void wrongCodeRejected() {
        byte[] data = new byte[META_LEN];
        System.arraycopy(unlockCounter, 0, data, 0, UNLOCK_COUNTER);
        // leave the unlock code as zeros
        assertEquals(SW_INCORRECT_UNLOCK_CODE, sc.send(0x52, 0x00, 0x00, data).sw);
    }

    @Test
    void correctCounterAndCodeAccepted() {
        byte[] data = new byte[META_LEN];
        System.arraycopy(unlockCounter, 0, data, 0, UNLOCK_COUNTER);
        byte[] code = nfcCode(0x52, 0x00, 0x00, data.length, unlockCounter);
        System.arraycopy(code, 0, data, UNLOCK_COUNTER, UNLOCK_CODE);
        sc.send(0x52, 0x00, 0x00, data).requireSuccess();
    }

    @Test
    void codeComputedForDifferentCommandRejected() {
        byte[] data = new byte[META_LEN];
        System.arraycopy(unlockCounter, 0, data, 0, UNLOCK_COUNTER);
        // code valid for INS 0x53 but sent for 0x52
        byte[] code = nfcCode(0x53, 0x00, 0x00, data.length, unlockCounter);
        System.arraycopy(code, 0, data, UNLOCK_COUNTER, UNLOCK_CODE);
        assertEquals(SW_INCORRECT_UNLOCK_CODE, sc.send(0x52, 0x00, 0x00, data).sw);
    }
}
