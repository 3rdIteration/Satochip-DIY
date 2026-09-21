package dev.satochip.tests.common;

/**
 * Per-module configuration injected as system properties by the Gradle build.
 */
public final class TestConfig {

    private TestConfig() {
    }

    public static String appletClass() {
        return require("applet.class");
    }

    public static String appletAid() {
        return require("applet.aid");
    }

    public static String kind() {
        return require("applet.kind");
    }

    public static String variant() {
        return System.getProperty("applet.variant", "official");
    }

    public static boolean isThd89() {
        return "thd89".equalsIgnoreCase(variant());
    }

    public static boolean isOfficial() {
        return !isThd89();
    }

    private static String require(String key) {
        String value = System.getProperty(key);
        if (value == null) {
            throw new IllegalStateException("Missing system property '" + key + "'");
        }
        return value;
    }
}
