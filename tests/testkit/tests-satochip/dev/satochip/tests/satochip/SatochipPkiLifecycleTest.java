package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Certificate imported during personalization must be exportable once the applet is set up.
 */
class SatochipPkiLifecycleTest extends AbstractSatochipTest {

    @Test
    void importCertificateThenSetupThenExport() {
        verifyPinOk(DEFAULT_PIN);

        byte[] cert = new byte[48];
        for (int i = 0; i < cert.length; i++) {
            cert[i] = (byte) (0x40 + i);
        }
        sc.send(0x92, 0x00, 0x01, new byte[]{0x00, (byte) cert.length}).requireSuccess();
        byte[] proc = new byte[4 + cert.length];
        proc[3] = (byte) cert.length;
        System.arraycopy(cert, 0, proc, 4, cert.length);
        sc.send(0x92, 0x00, 0x02, proc).requireSuccess();

        setupDefault().requireSuccess();
        verifyPinOk(PIN);

        Response init = sc.send(0x93, 0x00, 0x01).requireSuccess();
        assertEquals(cert.length, ((init.data[0] & 0xFF) << 8) | (init.data[1] & 0xFF));

        Response chunk = sc.send(0x93, 0x00, 0x02,
                new byte[]{0x00, 0x00, 0x00, (byte) cert.length}).requireSuccess();
        assertArrayEquals(cert, chunk.data);
    }
}
