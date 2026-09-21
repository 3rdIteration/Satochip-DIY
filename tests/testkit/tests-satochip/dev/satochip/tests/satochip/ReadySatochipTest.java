package dev.satochip.tests.satochip;

import org.junit.jupiter.api.BeforeEach;

/**
 * Fixture with a fully initialized Satochip and PIN[0] verified.
 */
public abstract class ReadySatochipTest extends AbstractSatochipTest {

    @BeforeEach
    void initializeAndLogin() {
        setupDefault().requireSuccess();
        verifyPinOk(PIN);
    }
}
