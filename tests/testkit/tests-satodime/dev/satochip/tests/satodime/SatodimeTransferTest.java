package dev.satochip.tests.satodime;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class SatodimeTransferTest extends ReadySatodimeTest {

    @Test
    void transferResetsSetup() {
        byte[] originalSecret = unlockSecret.clone();

        sc.send(0x5A, 0x00, 0x00, new byte[UNLOCK_COUNTER + UNLOCK_CODE]).requireSuccess();

        Response status = card.send(0x3C, 0x00, 0x00).requireSuccess();
        assertEquals(0, status.data[10] & 0xFF, "setupDone should be cleared after transfer");

        Response newSetup = setup().requireSuccess();
        assertEquals(24, newSetup.data.length);
        assertNotEquals(dev.satochip.tests.common.Hex.encode(originalSecret),
                dev.satochip.tests.common.Hex.encode(unlockSecret), "unlock secret should be regenerated");
    }
}
