package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SatochipBip32Test extends ReadySatochipTest {

    private static final byte[] SEED = new byte[32];

    static {
        for (int i = 0; i < SEED.length; i++) {
            SEED[i] = (byte) (i + 1);
        }
    }

    private Response importSeed() {
        return sc.send(0x6C, SEED.length, 0x00, SEED);
    }

    @Test
    void importSeedReturnsAuthentikey() {
        Response r = importSeed().requireSuccess();
        assertTrue(r.data.length >= 2 + 32 + 2 + 68, "unexpected authentikey response length " + r.data.length);
        assertEquals(0x0020, Hex.getShort(r.data, 0), "coordx size");
    }

    @Test
    void importSeedTwiceRejected() {
        importSeed().requireSuccess();
        assertEquals(SW_BIP32_INITIALIZED_SEED, importSeed().sw);
    }

    @Test
    void bip32AuthentikeyMatchesImportedOne() {
        Response imported = importSeed().requireSuccess();
        byte[] importedX = Hex.slice(imported.data, 2, 32);

        Response auth = sc.send(0x73, 0x00, 0x00).requireSuccess();
        byte[] authX = Hex.slice(auth.data, 2, 32);
        assertArrayEquals(importedX, authX);
    }

    @Test
    void authentikeyBeforeSeedIsUnavailable() {
        assertEquals(SW_BIP32_UNINITIALIZED_SEED, sc.send(0x73, 0x00, 0x00).sw);
    }

    @Test
    void deriveExtendedKey() {
        importSeed().requireSuccess();
        byte[] path = new byte[]{0x00, 0x00, 0x00, 0x00};
        Response r = sc.send(0x6D, 0x01, 0x00, path).requireSuccess();
        assertTrue(r.data.length > 32 + 2 + 32, "response too short: " + r.data.length);
        // chaincode must not be all zero
        byte[] chaincode = Hex.slice(r.data, 0, 32);
        assertFalse(isAllZero(chaincode), "chaincode should be non-zero");
        assertEquals(0x0020, Hex.getShort(r.data, 32), "coordx size");
    }

    @Test
    void deriveExtendedKeyAtDepthZero() {
        importSeed().requireSuccess();
        Response r = sc.send(0x6D, 0x00, 0x00, new byte[0]).requireSuccess();
        assertTrue(r.data.length > 66);
    }

    @Test
    void signMessageWithExtendedKey() {
        importSeed().requireSuccess();
        sc.send(0x6D, 0x01, 0x00, new byte[]{0x00, 0x00, 0x00, 0x00}).requireSuccess();

        byte[] message = "hello".getBytes();
        byte[] init = new byte[]{0x00, 0x00, 0x00, (byte) message.length};
        sc.send(0x6E, 0xFF, 0x01, init).requireSuccess();

        byte[] finalize_ = new byte[2 + message.length];
        finalize_[0] = 0x00;
        finalize_[1] = (byte) message.length;
        System.arraycopy(message, 0, finalize_, 2, message.length);
        Response r = sc.send(0x6E, 0xFF, 0x03, finalize_).requireSuccess();
        assertTrue(r.data.length >= 68 && r.data.length <= 74, "unexpected signature length " + r.data.length);
    }

    @Test
    void resetSeedClearsAuthentikey() {
        importSeed().requireSuccess();
        sc.send(0x77, PIN.length, 0x00, PIN).requireSuccess();
        // resetBIP32Seed logs the user out, so re-verify the PIN before querying the authentikey
        verifyPinOk(PIN);
        assertEquals(SW_BIP32_UNINITIALIZED_SEED, sc.send(0x73, 0x00, 0x00).sw);
    }

    @Test
    void resetSeedWithWrongPinFails() {
        importSeed().requireSuccess();
        byte[] wrong = "WrongPin".getBytes();
        Response r = sc.send(0x77, wrong.length, 0x00, wrong);
        assertTrue((r.sw & 0xFFF0) == 0x63C0);
    }

    private static boolean isAllZero(byte[] data) {
        byte[] copy = Arrays.copyOf(data, data.length);
        for (byte b : copy) {
            if (b != 0) {
                return false;
            }
        }
        return true;
    }
}
