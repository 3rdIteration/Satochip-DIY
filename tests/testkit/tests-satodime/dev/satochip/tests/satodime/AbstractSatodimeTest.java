package dev.satochip.tests.satodime;

import dev.satochip.tests.common.Card;
import dev.satochip.tests.common.CryptoUtil;
import dev.satochip.tests.common.Hex;
import dev.satochip.tests.common.Response;
import dev.satochip.tests.common.SecureChannel;
import org.junit.jupiter.api.BeforeEach;

/**
 * Common fixture for the Satodime applet (official and THD-89).
 */
public abstract class AbstractSatodimeTest {

    public static final String APPLET_CLASS = "org.satodime.applet.Satodime";
    public static final String AID = "5361746f44696d6500";

    public static final int SW_INVALID_PARAMETER = 0x9C0F;
    public static final int SW_INCORRECT_P1 = 0x9C10;
    public static final int SW_INCORRECT_P2 = 0x9C11;
    public static final int SW_SETUP_ALREADY_DONE = 0x9C07;
    public static final int SW_SETUP_NOT_DONE = 0x9C04;
    public static final int SW_INCORRECT_KEYSLOT_STATE = 0x9C52;
    public static final int SW_INCORRECT_UNLOCK_COUNTER = 0x9C50;
    public static final int SW_INCORRECT_UNLOCK_CODE = 0x9C51;
    public static final int SW_PKI_ALREADY_LOCKED = 0x9C40;

    public static final int UNLOCK_COUNTER = 4;
    public static final int UNLOCK_CODE = 20;
    public static final int ENTROPY = 32;
    public static final int KEYS = 3;

    protected Card card;
    protected SecureChannel sc;

    protected byte[] unlockCounter;
    protected byte[] unlockSecret;

    @BeforeEach
    void bootApplet() {
        card = new Card();
        card.install(AID, APPLET_CLASS, new byte[]{(byte) KEYS});
        card.select(AID).requireSuccess();
        sc = SecureChannel.establish(card);
    }

    /** Performs setup and stores the returned unlock counter/secret. */
    protected Response setup() {
        Response r = sc.send(0x2A, 0x00, 0x00, new byte[0]).requireSuccess();
        if (r.data.length >= UNLOCK_COUNTER + UNLOCK_CODE) {
            unlockCounter = Hex.slice(r.data, 0, UNLOCK_COUNTER);
            unlockSecret = Hex.slice(r.data, UNLOCK_COUNTER, UNLOCK_CODE);
        }
        return r;
    }

    /** Zero unlock counter + zero code, accepted on the USB/default media. */
    protected static byte[] unlockTail(int totalLength) {
        return new byte[totalLength];
    }

    /** Builds the unlock-code HMAC used on the contactless media. */
    protected byte[] nfcCode(int ins, int p1, int p2, int dataLength, byte[] counter) {
        byte[] header = new byte[]{
                (byte) Card.CLA, (byte) ins, (byte) p1, (byte) p2, (byte) dataLength};
        return CryptoUtil.hmacSha1(unlockSecret, CryptoUtil.concat(header, counter));
    }
}
