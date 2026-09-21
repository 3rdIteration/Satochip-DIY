package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedkeeperChallengeResponseTest extends ReadySeedkeeperTest {

    @Test
    void challengeResponse() {
        byte[] challenge = new byte[32];
        Arrays.fill(challenge, (byte) 0x66);
        Response r = sc.send(0x9A, 0x00, 0x00, challenge).requireSuccess();
        assertTrue(r.data.length >= 32 + 2 + 68);
    }
}
