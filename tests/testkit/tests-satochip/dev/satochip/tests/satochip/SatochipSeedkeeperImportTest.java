package dev.satochip.tests.satochip;

import dev.satochip.tests.common.EcUtil;
import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import java.security.interfaces.ECPublicKey;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Regression coverage for the Seedkeeper -> Satochip secure import path (INS 0xAA / 0xAB / 0xAC).
 * The THD-89 build used to leave {@code trusted_pubkey} unallocated, making 0xAA throw 0x6F00.
 */
class SatochipSeedkeeperImportTest extends ReadySatochipTest {

    private static byte[] validPubkey() {
        return EcUtil.encodeUncompressed((ECPublicKey) EcUtil.generateKeyPair().getPublic());
    }

    @Test
    void exportTrustedPubkeyBeforeImportFails() {
        assertEquals(SW_SECURE_IMPORT_NO_TRUSTEDPUBKEY, sc.send(0xAB, 0x00, 0x00).sw);
    }

    @Test
    void importTrustedPubkeyThenExport() {
        byte[] pub = validPubkey();
        byte[] data = new byte[2 + pub.length];
        data[0] = 0x00;
        data[1] = (byte) pub.length;
        System.arraycopy(pub, 0, data, 2, pub.length);

        Response r = sc.send(0xAA, 0x00, 0x00, data).requireSuccess();
        assertArrayEquals(pub, Hex.slice(r.data, 2, 65), "importTrustedPubkey should echo the pubkey");

        Response exported = sc.send(0xAB, 0x00, 0x00).requireSuccess();
        assertArrayEquals(pub, Hex.slice(exported.data, 2, 65));
    }

    @Test
    void importTrustedPubkeyRejectsWrongPrefix() {
        byte[] pub = validPubkey();
        pub[0] = 0x02; // compressed prefix
        byte[] data = new byte[2 + pub.length];
        data[1] = (byte) pub.length;
        System.arraycopy(pub, 0, data, 2, pub.length);
        assertEquals(SW_INVALID_PARAMETER, sc.send(0xAA, 0x00, 0x00, data).sw);
    }

    @Test
    void importTrustedPubkeyRejectsWrongSize() {
        byte[] data = new byte[]{0x00, 0x20}; // claims 32 bytes
        assertEquals(SW_INVALID_PARAMETER, sc.send(0xAA, 0x00, 0x00, data).sw);
    }

    @Test
    void importEncryptedSecretWithoutTrustedPubkeyFails() {
        byte[] bogus = new byte[40];
        assertEquals(SW_SECURE_IMPORT_NO_TRUSTEDPUBKEY, sc.send(0xAC, 0x00, 0x00, bogus).sw);
    }
}
