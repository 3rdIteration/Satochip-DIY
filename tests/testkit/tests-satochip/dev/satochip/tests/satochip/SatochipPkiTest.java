package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * PKI personalization ops that are permitted before {@code setup()} (which sets personalizationDone).
 */
class SatochipPkiTest extends AbstractSatochipTest {

    @BeforeEach
    void loginWithFactoryPin() {
        verifyPinOk(DEFAULT_PIN);
    }

    @Test
    void exportPkiPubkey() {
        Response r = sc.send(0x98, 0x00, 0x00).requireSuccess();
        assertEquals(65, r.data.length);
    }

    @Test
    void signCsr() {
        byte[] hash = new byte[32];
        Arrays.fill(hash, (byte) 0x11);
        Response r = sc.send(0x94, 0x00, 0x00, hash).requireSuccess();
        assertTrue(r.data.length >= 68 && r.data.length <= 74, "unexpected signature length " + r.data.length);
    }

    @Test
    void importPkiCertificate() {
        byte[] cert = new byte[40];
        for (int i = 0; i < cert.length; i++) {
            cert[i] = (byte) (i + 1);
        }
        sc.send(0x92, 0x00, 0x01, new byte[]{0x00, (byte) cert.length}).requireSuccess();

        byte[] proc = new byte[4 + cert.length];
        proc[3] = (byte) cert.length;
        System.arraycopy(cert, 0, proc, 4, cert.length);
        sc.send(0x92, 0x00, 0x02, proc).requireSuccess();
    }

    @Test
    void lockPkiFreezesPersonalization() {
        sc.send(0x99, 0x00, 0x00).requireSuccess();
        // Once personalizationDone is set pre-setup, the setup gate blocks everything else.
        assertEquals(SW_SETUP_NOT_DONE, sc.send(0x94, 0x00, 0x00, new byte[32]).sw);
    }
}
