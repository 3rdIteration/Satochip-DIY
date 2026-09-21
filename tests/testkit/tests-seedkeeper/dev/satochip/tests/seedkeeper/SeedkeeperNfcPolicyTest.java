package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class SeedkeeperNfcPolicyTest extends ReadySeedkeeperTest {

    @Test
    void defaultPolicyIsEnabled() {
        Response r = status().requireSuccess();
        assertEquals(0, r.data[12] & 0xFF);
    }

    @Test
    void disableNfcPolicy() {
        sc.send(0x3E, 0x01, 0x00, new byte[0]).requireSuccess();
        Response r = status().requireSuccess();
        assertEquals(1, r.data[12] & 0xFF);
    }

    @Test
    void blockedPolicyCannotBeChanged() {
        sc.send(0x3E, 0x02, 0x00, new byte[0]).requireSuccess();
        assertEquals(SW_NFC_BLOCKED, sc.send(0x3E, 0x00, 0x00, new byte[0]).sw);
    }

    @Test
    void invalidPolicyRejected() {
        assertEquals(SW_INCORRECT_P1, sc.send(0x3E, 0x03, 0x00, new byte[0]).sw);
    }

    @Test
    void policyChangeOnlyFromContact() {
        card.changeProtocol("T=CL,TYPE_A,T0");
        assertEquals(SW_NFC_DISABLED, sc.send(0x3E, 0x01, 0x00, new byte[0]).sw);
    }

    @Test
    void disabledNfcBlocksContactlessSelection() {
        sc.send(0x3E, 0x01, 0x00, new byte[0]).requireSuccess();
        card.changeProtocol("T=CL,TYPE_A,T0");
        Response selected = card.select(AID);
        assertFalse(selected.isSuccess(), "selection over contactless must fail when NFC is disabled");
    }
}
