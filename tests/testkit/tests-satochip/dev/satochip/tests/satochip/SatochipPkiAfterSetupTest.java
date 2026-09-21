package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * After setup() the PKI is locked (personalizationDone), but the certificate can still be exported.
 */
class SatochipPkiAfterSetupTest extends ReadySatochipTest {

    @Test
    void signCsrIsLockedAfterSetup() {
        byte[] hash = new byte[32];
        Arrays.fill(hash, (byte) 0x22);
        assertEquals(SW_PKI_ALREADY_LOCKED, sc.send(0x94, 0x00, 0x00, hash).sw);
    }

    @Test
    void importCertificateIsLockedAfterSetup() {
        assertEquals(SW_PKI_ALREADY_LOCKED, sc.send(0x92, 0x00, 0x01, new byte[]{0x00, 0x10}).sw);
    }

    @Test
    void exportPkiPubkeyStillWorks() {
        Response r = sc.send(0x98, 0x00, 0x00).requireSuccess();
        assertEquals(65, r.data.length);
    }
}
