package dev.satochip.tests.satodime;

import org.junit.jupiter.api.BeforeEach;

/**
 * Fixture with a fully initialized Satodime.
 */
public abstract class ReadySatodimeTest extends AbstractSatodimeTest {

    @BeforeEach
    void initialize() {
        setup().requireSuccess();
    }
}
