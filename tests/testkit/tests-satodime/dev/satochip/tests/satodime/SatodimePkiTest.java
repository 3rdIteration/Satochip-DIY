package dev.satochip.tests.satodime;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatodimePkiTest extends AbstractSatodimeTest {

    @Test
    void exportPkiPubkey() {
        Response r = sc.send(0x98, 0x00, 0x00).requireSuccess();
        assertEquals(65, r.data.length);
    }

    @Test
    void signCsr() {
        byte[] hash = new byte[32];
        Arrays.fill(hash, (byte) 0x33);
        Response r = sc.send(0x94, 0x00, 0x00, hash).requireSuccess();
        assertTrue(r.data.length >= 68 && r.data.length <= 74);
    }

    @Test
    void importAndLockCertificate() {
        byte[] cert = new byte[32];
        for (int i = 0; i < cert.length; i++) {
            cert[i] = (byte) (i + 7);
        }
        sc.send(0x92, 0x00, 0x01, new byte[]{0x00, (byte) cert.length}).requireSuccess();
        byte[] proc = new byte[4 + cert.length];
        proc[3] = (byte) cert.length;
        System.arraycopy(cert, 0, proc, 4, cert.length);
        sc.send(0x92, 0x00, 0x02, proc).requireSuccess();

        sc.send(0x99, 0x00, 0x00).requireSuccess();
        assertEquals(SW_PKI_ALREADY_LOCKED, sc.send(0x94, 0x00, 0x00, new byte[32]).sw);
    }

    @Test
    void challengeResponse() {
        byte[] challenge = new byte[32];
        Arrays.fill(challenge, (byte) 0x44);
        Response r = sc.send(0x9A, 0x00, 0x00, challenge).requireSuccess();
        assertTrue(r.data.length >= 32 + 2 + 68);
    }
}
