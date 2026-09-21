package dev.satochip.tests.seedkeeper;

import dev.satochip.tests.common.Card;
import dev.satochip.tests.common.Response;
import dev.satochip.tests.common.SecureChannel;
import org.junit.jupiter.api.BeforeEach;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Common fixture for the Seedkeeper applet (official and THD-89).
 */
public abstract class AbstractSeedkeeperTest {

    public static final String APPLET_CLASS = "org.seedkeeper.applet.SeedKeeper";
    public static final String AID = "536565644b656570657200";

    public static final byte[] PIN = "12345678".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] PIN2 = "abcdefgh".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] PUK = "87654321".getBytes(StandardCharsets.US_ASCII);
    public static final byte[] DEFAULT_PIN = "Muscle00".getBytes(StandardCharsets.US_ASCII);

    public static final int SW_UNAUTHORIZED = 0x9C06;
    public static final int SW_OBJECT_NOT_FOUND = 0x9C08;
    public static final int SW_IDENTITY_BLOCKED = 0x9C0C;
    public static final int SW_INVALID_PARAMETER = 0x9C0F;
    public static final int SW_INCORRECT_P1 = 0x9C10;
    public static final int SW_INCORRECT_P2 = 0x9C11;
    public static final int SW_SEQUENCE_END = 0x9C12;
    public static final int SW_SETUP_ALREADY_DONE = 0x9C07;
    public static final int SW_PKI_ALREADY_LOCKED = 0x9C40;
    public static final int SW_NFC_DISABLED = 0x9C48;
    public static final int SW_NFC_BLOCKED = 0x9C49;

    public static final int SECRET_HEADER_SIZE = 13;
    public static final int SECRET_TYPE_MASTER_SEED = 0x10;
    public static final int SECRET_TYPE_DATA = 0xC0;
    public static final int EXPORT_ALLOWED = 1;
    public static final int EXPORT_SECUREONLY = 2;
    public static final int OP_INIT = 1;
    public static final int OP_PROCESS = 2;
    public static final int OP_FINALIZE = 3;

    protected Card card;
    protected SecureChannel sc;

    @BeforeEach
    void bootApplet() {
        card = new Card();
        card.install(AID, APPLET_CLASS, new byte[]{0x0F, (byte) 0xFF});
        card.select(AID).requireSuccess();
        sc = SecureChannel.establish(card);
    }

    protected Response setupDefault() {
        return sc.send(0x2A, 0x00, 0x00, setupData(PIN, PUK, DEFAULT_PIN, 0x0000));
    }

    protected static byte[] setupData(byte[] pin, byte[] puk, byte[] defaultPin, int optionFlags) {
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
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write(0);
        out.write((optionFlags >> 8) & 0xFF);
        out.write(optionFlags & 0xFF);
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
