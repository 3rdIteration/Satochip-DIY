package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedkeeperPkiTest extends AbstractSeedkeeperTest {

    @Test
    void exportPkiPubkey() {
        Response r = sc.send(0x98, 0x00, 0x00).requireSuccess();
        assertEquals(65, r.data.length);
    }

    @Test
    void signCsrBeforeSetup() {
        byte[] hash = new byte[32];
        Arrays.fill(hash, (byte) 0x55);
        Response r = sc.send(0x94, 0x00, 0x00, hash).requireSuccess();
        assertTrue(r.data.length >= 68 && r.data.length <= 74);
    }
}
