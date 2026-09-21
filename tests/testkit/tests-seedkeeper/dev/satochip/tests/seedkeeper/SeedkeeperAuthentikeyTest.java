package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedkeeperAuthentikeyTest extends ReadySeedkeeperTest {

    @Test
    void exportAuthentikeyAfterSetup() {
        Response r = sc.send(0xAD, 0x00, 0x00).requireSuccess();
        assertTrue(r.data.length >= 2 + 32 + 2 + 68);
        assertEquals(0x0020, ((r.data[0] & 0xFF) << 8) | (r.data[1] & 0xFF));
    }

    @Test
    void deprecatedAuthentikeyAlias() {
        Response r = sc.send(0x73, 0x00, 0x00).requireSuccess();
        assertTrue(r.data.length >= 2 + 32);
    }
}
