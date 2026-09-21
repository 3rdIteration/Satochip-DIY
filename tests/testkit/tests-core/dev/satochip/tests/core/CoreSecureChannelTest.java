package dev.satochip.tests.core;

import dev.satochip.tests.common.Apdu;
import dev.satochip.tests.common.Card;
import dev.satochip.tests.common.Response;
import dev.satochip.tests.common.SecureChannel;
import dev.satochip.tests.common.TestConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Transport-level tests that hold for all three applets (Satochip, Satodime, Seedkeeper),
 * and for both the official and THD-89 forks.
 */
class CoreSecureChannelTest {

    private static Card boot() {
        Card card = new Card();
        card.install(TestConfig.appletAid(), TestConfig.appletClass());
        card.select(TestConfig.appletAid()).requireSuccess();
        return card;
    }

    @Test
    void handshakeSucceeds() {
        Card card = boot();
        assertNotNull(SecureChannel.establish(card));
    }

    @Test
    void getStatusIsAvailableInTheClear() {
        Card card = boot();
        Response r = card.send(0x3C, 0x00, 0x00).requireSuccess();
        assertTrue(r.data.length >= 11, "status should be at least 11 bytes, got " + r.data.length);
    }

    @Test
    void clearCommandRequiresSecureChannel() {
        Card card = boot();
        Response r = card.send(0x00, 0x00, 0x00);
        assertEquals(0x9C20, r.sw, "expected SW_SECURE_CHANNEL_REQUIRED");
    }

    @Test
    void processSecureChannelBeforeInitFails() {
        Card card = boot();
        Response r = card.transmit(Apdu.case3(Card.CLA, 0x82, 0x00, 0x00, new byte[20]));
        assertEquals(0x9C21, r.sw, "expected SW_SECURE_CHANNEL_UNINITIALIZED");
    }

    @Test
    void wrongClaRejected() {
        Card card = boot();
        Response r = card.transmit(Apdu.case1(0x00, 0x3C, 0x00, 0x00));
        assertEquals(0x6E00, r.sw, "expected SW_CLA_NOT_SUPPORTED");
    }

    @Test
    void wrappedUnknownInsFails() {
        Card card = boot();
        SecureChannel sc = SecureChannel.establish(card);
        Response r = sc.send(0x00, 0x00, 0x00);
        assertNotEquals(0x9000, r.sw);
    }

    @Test
    void tamperedMacIsRejected() {
        Card card = boot();
        SecureChannel sc = SecureChannel.establish(card);
        byte[] frame = sc.frame(0x00, 0x00, 0x00, new byte[0]);
        frame[frame.length - 1] ^= 0x01;
        assertEquals(0x9C23, sc.sendRawFrame(frame).sw, "expected SW_SECURE_CHANNEL_WRONG_MAC");
    }

    @Test
    void truncatedFrameIsRejected() {
        Card card = boot();
        SecureChannel sc = SecureChannel.establish(card);
        assertEquals(0x6700, sc.sendRawFrame(new byte[10]).sw);
    }
}
