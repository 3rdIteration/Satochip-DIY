package dev.satochip.tests.satochip;

import dev.satochip.tests.common.Card;
import dev.satochip.tests.common.Response;
import dev.satochip.tests.common.SecureChannel;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Common fixture for the Satochip applet (official and THD-89).
 */
public abstract class AbstractSatochipTest {

    public static final String APPLET_CLASS = "org.satochip.applet.CardEdge";
    public static final String AID = "5361746F4368697000";

    public static final byte[] PIN = "12345678".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] PIN2 = "abcdefgh".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] PUK = "87654321".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] DEFAULT_PIN = "Muscle00".getBytes(StandardCharsets.US_ASCII);

    public static final int SW_UNAUTHORIZED = 0x9C06;
    public static final int SW_SETUP_ALREADY_DONE = 0x9C07;
    public static final int SW_SETUP_NOT_DONE = 0x9C04;
    public static final int SW_INVALID_PARAMETER = 0x9C0F;
    public static final int SW_INCORRECT_P1 = 0x9C10;
    public static final int SW_INCORRECT_P2 = 0x9C11;
    public static final int SW_IDENTITY_BLOCKED = 0x9C0C;
    public static final int SW_BIP32_UNINITIALIZED_SEED = 0x9C14;
    public static final int SW_BIP32_INITIALIZED_SEED = 0x9C17;
    public static final int SW_SECURE_IMPORT_NO_TRUSTEDPUBKEY = 0x9C35;
    public static final int SW_PKI_ALREADY_LOCKED = 0x9C40;
    public static final int SW_FILE_NOT_FOUND = 0x6A82;

    protected Card card;
    protected SecureChannel sc;

    @BeforeEach
    void bootApplet() {
        card = new Card();
        card.install(AID, APPLET_CLASS);
        card.select(AID).requireSuccess();
        sc = SecureChannel.establish(card);
    }

    protected Response setupDefault() {
        return setup(PIN, PUK, DEFAULT_PIN, 0x0800, 0x0000, null, null);
    }

    protected Response setup(byte[] pin, byte[] puk, byte[] defaultPin, int secmem, int optionFlags,
                             byte[] hmacKey, byte[] amountLimit) {
        return sc.send(0x2A, 0x00, 0x00, setupData(pin, puk, defaultPin, secmem, optionFlags, hmacKey, amountLimit));
    }

    protected static byte[] setupData(byte[] pin, byte[] puk, byte[] defaultPin, int secmem, int optionFlags,
                                      byte[] hmacKey, byte[] amountLimit) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(defaultPin.length);
        out.write(defaultPin, 0, defaultPin.length);
        out.write(5);
        out.write(3);
        out.write(pin.length);
        out.write(pin, 0, pin.length);
        out.write(puk.length);
        out.write(puk, 0, puk.length);
        out.write(5);
        out.write(3);
        out.write(pin.length);
        out.write(pin, 0, pin.length);
        out.write(puk.length);
        out.write(puk, 0, puk.length);
        out.write((secmem >> 8) & 0xFF);
        out.write(secmem & 0xFF);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write((optionFlags >> 8) & 0xFF);
        out.write(optionFlags & 0xFF);
        if (hmacKey != null) {
            out.write(hmacKey, 0, hmacKey.length);
            out.write(amountLimit, 0, amountLimit.length);
        }
        return out.toByteArray();
    }

    protected Response verifyPin(byte[] pin) {
        return sc.send(0x42, 0x00, 0x00, pin);
    }

    protected void verifyPinOk(byte[] pin) {
        verifyPin(pin).requireSuccess();
    }

    protected Response status() {
        return card.send(0x3C, 0x00, 0x00);
    }
}
