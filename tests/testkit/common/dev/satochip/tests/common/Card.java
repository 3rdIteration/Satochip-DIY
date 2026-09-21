package dev.satochip.tests.common;

import com.licel.jcardsim.base.Simulator;
import com.licel.jcardsim.utils.AIDUtil;
import javacard.framework.Applet;

/**
 * Thin wrapper around a jCardsim {@link Simulator} used to install and talk to a single applet.
 */
public final class Card {

    /** CLA used by all Satochip / Satodime / Seedkeeper applets. */
    public static final int CLA = 0xB0;

    private final Simulator simulator;

    public Card() {
        this.simulator = new Simulator();
    }

    public Simulator simulator() {
        return simulator;
    }

    @SuppressWarnings("unchecked")
    public void install(String aidHex, String className, byte[] installData) {
        Class<?> clazz;
        try {
            clazz = Class.forName(className);
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Applet class not found on test classpath: " + className, e);
        }
        byte[] aid = Hex.decode(aidHex);
        byte[] bArray = installParams(aid, installData);
        simulator.installApplet(AIDUtil.create(aidHex), (Class<? extends Applet>) clazz,
                bArray, (short) 0, (byte) bArray.length);
    }

    public void install(String aidHex, String className) {
        install(aidHex, className, null);
    }

    /**
     * Builds the GP-style install parameter block parsed by the Seedkeeper / Satodime
     * install methods: [aidLen | aid | controlLen(=0) | dataLen | data].
     */
    public static byte[] installParams(byte[] aid, byte[] data) {
        int dataLen = data == null ? 0 : data.length;
        byte[] out = new byte[1 + aid.length + 1 + 1 + dataLen];
        int p = 0;
        out[p++] = (byte) aid.length;
        System.arraycopy(aid, 0, out, p, aid.length);
        p += aid.length;
        out[p++] = 0x00;              // control/privilege field length
        out[p++] = (byte) dataLen;    // installation data length
        if (data != null) {
            System.arraycopy(data, 0, out, p, data.length);
        }
        return out;
    }

    public Response select(String aidHex) {
        return Response.parse(simulator.selectAppletWithResult(AIDUtil.create(aidHex)));
    }

    /** Sends a command without secure-channel wrapping. */
    public Response transmit(byte[] command) {
        return Response.parse(simulator.transmitCommand(command));
    }

    public Response send(int ins, int p1, int p2, byte[] data) {
        return transmit(Apdu.case3(CLA, ins, p1, p2, data));
    }

    public Response send(int ins, int p1, int p2) {
        return transmit(Apdu.case3(CLA, ins, p1, p2, null));
    }

    /** Switch the simulated transport media, e.g. "T=CL,TYPE_A,T0" for contactless. */
    public void changeProtocol(String protocol) {
        simulator.changeProtocol(protocol);
    }

    public void reset() {
        simulator.reset();
    }
}
