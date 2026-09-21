package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedkeeperSecretTest extends ReadySeedkeeperTest {

    private byte[] generateMasterseed(int size, String label) {
        byte[] lbl = label.getBytes();
        byte[] data = new byte[1 + lbl.length];
        data[0] = (byte) lbl.length;
        System.arraycopy(lbl, 0, data, 1, lbl.length);
        Response r = sc.send(0xA0, size, EXPORT_ALLOWED, data).requireSuccess();
        return Hex.slice(r.data, 0, 2); // id
    }

    @Test
    void generateMasterseedReturnsIdAndFingerprint() {
        byte[] lbl = "seed1".getBytes();
        byte[] data = new byte[1 + lbl.length];
        data[0] = (byte) lbl.length;
        System.arraycopy(lbl, 0, data, 1, lbl.length);
        Response r = sc.send(0xA0, 32, EXPORT_ALLOWED, data).requireSuccess();
        assertEquals(2 + 4, r.data.length);

        // fingerprint must be non-zero
        assertTrue((r.data[2] | r.data[3] | r.data[4] | r.data[5]) != 0);
    }

    @Test
    void generateMasterseedRejectsBadSize() {
        assertEquals(SW_INCORRECT_P1, sc.send(0xA0, 8, EXPORT_ALLOWED, new byte[]{0x00}).sw);
        assertEquals(SW_INCORRECT_P1, sc.send(0xA0, 65, EXPORT_ALLOWED, new byte[]{0x00}).sw);
    }

    @Test
    void listSecretHeaders() {
        generateMasterseed(16, "a");
        generateMasterseed(32, "b");

        int count = 0;
        Response r = sc.send(0xA6, 0x00, OP_INIT, new byte[0]);
        while (r.isSuccess()) {
            count++;
            r = sc.send(0xA6, 0x00, OP_PROCESS, new byte[0]);
        }
        assertEquals(SW_SEQUENCE_END, r.sw);
        assertEquals(2, count);
    }

    @Test
    void exportPlainRoundTrip() {
        byte[] id = generateMasterseed(32, "seed");

        Response init = sc.send(0xA2, EXPORT_ALLOWED, OP_INIT, id).requireSuccess();
        assertArrayEquals(id, Hex.slice(init.data, 0, 2));

        Response chunk = sc.send(0xA2, EXPORT_ALLOWED, OP_PROCESS, new byte[0]).requireSuccess();
        int size = Hex.getShort(chunk.data, 0);
        assertEquals(1 + 32, size, "size byte + 32-byte masterseed");
        assertEquals(32, chunk.data[2] & 0xFF, "seed size");
        assertTrue(chunk.data.length > 2 + size + 2, "signature present");
    }

    @Test
    void exportRequiresExportRights() {
        // generate with export forbidden (rights = 0)
        Response r = sc.send(0xA0, 32, 0, new byte[]{0x00}).requireSuccess();
        byte[] id = Hex.slice(r.data, 0, 2);
        Response init = sc.send(0xA2, EXPORT_ALLOWED, OP_INIT, id);
        assertEquals(0x9C31, init.sw, "expected SW_EXPORT_NOT_ALLOWED");
    }

    @Test
    void resetSecret() {
        byte[] id = generateMasterseed(32, "to-reset");
        sc.send(0xA5, 0x00, 0x00, id).requireSuccess();
        // exporting a deleted object fails
        assertEquals(SW_OBJECT_NOT_FOUND, sc.send(0xA2, EXPORT_ALLOWED, OP_INIT, id).sw);
    }

    @Test
    void resetUnknownObjectFails() {
        assertEquals(SW_OBJECT_NOT_FOUND, sc.send(0xA5, 0x00, 0x00, new byte[]{0x7F, 0x7F}).sw);
    }

    @Test
    void objectCountIncreasesWithGeneratedSecrets() {
        generateMasterseed(16, "one");
        generateMasterseed(16, "two");
        Response r = sc.send(0xA7, 0x00, 0x00).requireSuccess();
        assertEquals(2, ((r.data[0] & 0xFF) << 8) | (r.data[1] & 0xFF));
    }
}
