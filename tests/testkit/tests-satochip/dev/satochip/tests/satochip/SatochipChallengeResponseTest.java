package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SatochipChallengeResponseTest extends ReadySatochipTest {

    @Test
    void challengeResponseReturnsSignedNonce() {
        byte[] challenge = new byte[32];
        for (int i = 0; i < challenge.length; i++) {
            challenge[i] = (byte) (0x30 + i);
        }
        Response r = sc.send(0x9A, 0x00, 0x00, challenge).requireSuccess();
        assertTrue(r.data.length >= 32 + 2 + 68, "unexpected length " + r.data.length);
        int sigSize = ((r.data[32] & 0xFF) << 8) | (r.data[33] & 0xFF);
        assertTrue(sigSize >= 68 && sigSize <= 74);
    }

    @Test
    void challengeResponseRejectsShortChallenge() {
        assertTrue(sc.send(0x9A, 0x00, 0x00, new byte[16]).sw != 0x9000);
    }
}
