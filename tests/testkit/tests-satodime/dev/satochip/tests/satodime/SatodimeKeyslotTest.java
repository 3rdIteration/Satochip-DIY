package dev.satochip.tests.satodime;

import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatodimeKeyslotTest extends ReadySatodimeTest {

    private static final int KEYSLOT_STATUS_SIZE = 1 + 1 + 1 + 4 + 34 + 34 + 66;

    private Response seal(int slot) {
        return sc.send(0x57, slot, 0x00, new byte[UNLOCK_COUNTER + UNLOCK_CODE + ENTROPY]);
    }

    private Response unseal(int slot) {
        return sc.send(0x58, slot, 0x00, new byte[UNLOCK_COUNTER + UNLOCK_CODE]);
    }

    private Response resetKey(int slot) {
        return sc.send(0x59, slot, 0x00, new byte[UNLOCK_COUNTER + UNLOCK_CODE]);
    }

    private byte[] slotStatus(int slot) {
        return sc.send(0x51, slot, 0x00).requireSuccess().data;
    }

    @Test
    void initialSlotIsUninitialized() {
        byte[] s = slotStatus(0);
        assertEquals(KEYSLOT_STATUS_SIZE, s.length);
        assertEquals(0, s[0] & 0xFF, "state uninitialized");
    }

    @Test
    void pubkeyUnavailableBeforeSeal() {
        assertEquals(SW_INCORRECT_KEYSLOT_STATE, sc.send(0x55, 0x00, 0x00).sw);
    }

    @Test
    void sealUnsealResetLifecycle() {
        Response sealed = seal(0).requireSuccess();
        assertEquals(65, Hex.getShort(sealed.data, 0), "pubkey size");
        byte[] sealedPub = Hex.slice(sealed.data, 2, 65);
        assertEquals(1, slotStatus(0)[0] & 0xFF, "state sealed");

        Response pub = sc.send(0x55, 0x00, 0x00).requireSuccess();
        assertArrayEquals(sealedPub, Hex.slice(pub.data, 2, 65));

        // private key is not available while sealed
        assertEquals(SW_INCORRECT_KEYSLOT_STATE,
                sc.send(0x56, 0x00, 0x00, new byte[UNLOCK_COUNTER + UNLOCK_CODE]).sw);

        Response unsealed = unseal(0).requireSuccess();
        assertTrue(unsealed.data.length > 2 + 96, "privkey bundle too short");
        assertEquals(2, slotStatus(0)[0] & 0xFF, "state unsealed");

        Response priv = sc.send(0x56, 0x00, 0x00, new byte[UNLOCK_COUNTER + UNLOCK_CODE]).requireSuccess();
        assertTrue(priv.data.length > 2 + 96);

        resetKey(0).requireSuccess();
        assertEquals(0, slotStatus(0)[0] & 0xFF, "state reset to uninitialized");
    }

    @Test
    void setMetadata() {
        seal(0).requireSuccess();
        byte[] meta = new byte[UNLOCK_COUNTER + UNLOCK_CODE + 2 + 1 + 4 + 34 + 34];
        int off = UNLOCK_COUNTER + UNLOCK_CODE + 2; // skip RFU
        meta[off] = 0x01; // asset = coin
        meta[off + 1] = 0x00;
        meta[off + 2] = 0x00;
        meta[off + 3] = 0x00;
        meta[off + 4] = 0x3C; // slip44 = 60
        sc.send(0x52, 0x00, 0x00, meta).requireSuccess();

        byte[] s = slotStatus(0);
        assertEquals(0x01, s[2] & 0xFF, "asset");
        assertEquals(0x3C, s[6] & 0xFF, "slip44 low byte");
    }

    @Test
    void setData() {
        seal(0).requireSuccess();
        byte[] data = new byte[UNLOCK_COUNTER + UNLOCK_CODE + 66];
        data[UNLOCK_COUNTER + UNLOCK_CODE] = 0x7A;
        sc.send(0x52, 0x00, 0x01, data).requireSuccess();

        byte[] s = slotStatus(0);
        assertEquals(0x7A, s[1 + 1 + 1 + 4 + 34 + 34] & 0xFF, "data[0]");
    }

    @Test
    void outOfRangeSlotRejected() {
        assertEquals(SW_INCORRECT_P1, sc.send(0x51, KEYS, 0x00).sw);
        assertEquals(SW_INCORRECT_P1, seal(KEYS).sw);
    }

    @Test
    void sealTwiceRejected() {
        seal(0).requireSuccess();
        assertEquals(SW_INCORRECT_KEYSLOT_STATE, seal(0).sw);
    }
}
