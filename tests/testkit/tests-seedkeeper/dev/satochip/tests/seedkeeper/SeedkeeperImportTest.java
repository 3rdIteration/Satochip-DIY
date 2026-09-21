package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SeedkeeperImportTest extends ReadySeedkeeperTest {

    @Test
    void importPlainMasterseedAndExport() {
        byte[] seed = new byte[32];
        for (int i = 0; i < seed.length; i++) {
            seed[i] = (byte) (i + 1);
        }
        byte[] body = new byte[1 + seed.length];
        body[0] = (byte) seed.length;
        System.arraycopy(seed, 0, body, 1, seed.length);
        int paddedSize = 48;

        byte[] header = new byte[SECRET_HEADER_SIZE];
        header[0] = SECRET_TYPE_MASTER_SEED;
        header[1] = 0x01; // origin
        header[2] = EXPORT_ALLOWED;
        header[12] = 0x00; // label size

        byte[] init = new byte[SECRET_HEADER_SIZE + 2];
        System.arraycopy(header, 0, init, 0, SECRET_HEADER_SIZE);
        init[SECRET_HEADER_SIZE] = (byte) (paddedSize >> 8);
        init[SECRET_HEADER_SIZE + 1] = (byte) paddedSize;
        sc.send(0xA1, EXPORT_ALLOWED, OP_INIT, init).requireSuccess();

        byte[] fin = new byte[2 + body.length];
        fin[0] = (byte) (body.length >> 8);
        fin[1] = (byte) body.length;
        System.arraycopy(body, 0, fin, 2, body.length);
        Response result = sc.send(0xA1, EXPORT_ALLOWED, OP_FINALIZE, fin).requireSuccess();
        assertEquals(2 + 4, result.data.length);
        byte[] id = Hex.slice(result.data, 0, 2);

        // export it back in plaintext and compare
        sc.send(0xA2, EXPORT_ALLOWED, OP_INIT, id).requireSuccess();
        Response chunk = sc.send(0xA2, EXPORT_ALLOWED, OP_PROCESS, new byte[0]).requireSuccess();
        assertEquals(1 + seed.length, Hex.getShort(chunk.data, 0));
        assertArrayEquals(body, Hex.slice(chunk.data, 2, body.length));
    }
}
