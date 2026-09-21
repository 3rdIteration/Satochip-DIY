package dev.satochip.tests.seedkeeper;

import org.junit.jupiter.api.BeforeEach;

/**
 * Fixture with a fully initialized Seedkeeper and PIN verified.
 */
public abstract class ReadySeedkeeperTest extends AbstractSeedkeeperTest {

    @BeforeEach
    void initializeAndLogin() {
        setupDefault().requireSuccess();
        verifyPinOk(PIN);
    }
}
