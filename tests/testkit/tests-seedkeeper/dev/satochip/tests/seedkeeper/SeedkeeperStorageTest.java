package dev.satochip.tests.seedkeeper;

import org.junit.jupiter.api.Test;
import org.seedkeeper.applet.Logger;
import org.seedkeeper.applet.MemoryManager;
import org.seedkeeper.applet.ObjectManager;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * White-box tests for the Seedkeeper storage layer (shared by both forks).
 */
class SeedkeeperStorageTest {

    @Test
    void memoryManagerAllocWriteFree() {
        MemoryManager mm = new MemoryManager((short) 256);
        short a = mm.alloc((short) 32);
        short b = mm.alloc((short) 16);
        assertNotEquals(MemoryManager.NULL_OFFSET, a);
        assertNotEquals(MemoryManager.NULL_OFFSET, b);
        assertNotEquals(a, b);

        mm.setByte(a, (byte) 0x11);
        mm.setByte(b, (byte) 0x22);
        assertEquals(0x11, mm.getByte(a));
        assertEquals(0x22, mm.getByte(b));

        short freeBefore = mm.freemem();
        mm.free(a);
        assertTrue(mm.freemem() >= freeBefore);
        assertNotEquals(MemoryManager.NULL_OFFSET, mm.alloc((short) 8));
    }

    @Test
    void memoryManagerRunsOutOfMemory() {
        MemoryManager mm = new MemoryManager((short) 64);
        assertNotEquals(MemoryManager.NULL_OFFSET, mm.alloc((short) 60));
        assertEquals(MemoryManager.NULL_OFFSET, mm.alloc((short) 60));
    }

    @Test
    void objectManagerRoundTrip() {
        ObjectManager om = new ObjectManager((short) 0x0FFF);
        short base = om.createObject((short) 0, (short) 1, (short) 16, true);
        assertTrue(om.exists((short) 0, (short) 1));
        assertEquals(1, om.getObjectNumber());
        assertEquals(16, om.getSizeFromAddress(base));
        assertEquals(1, om.getIdFromAddress(base));

        byte[] data = new byte[16];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) (i + 1);
        }
        om.setObjectData(base, (short) 0, data, (short) 0, (short) data.length);

        byte[] out = new byte[16];
        om.getObjectData(base, (short) 0, out, (short) 0, (short) out.length);
        assertArrayEquals(data, out);

        assertTrue(om.destroyObject((short) 0, (short) 1, true));
        assertFalse(om.exists((short) 0, (short) 1));
        assertEquals(0, om.getObjectNumber());
    }

    @Test
    void objectManagerIteration() {
        ObjectManager om = new ObjectManager((short) 0x0FFF);
        om.createObject((short) 0, (short) 10, (short) 8, false);
        om.createObject((short) 0, (short) 11, (short) 8, false);

        int count = 0;
        short base = om.getFirstRecord();
        while (base != (short) 0xFFFF) {
            count++;
            base = om.getNextRecord();
        }
        assertEquals(2, count);
    }

    @Test
    void loggerRecords() {
        Logger logger = new Logger((short) 10);
        byte[] buffer = new byte[128];
        logger.createLog((byte) 0x01, (short) 1, (short) 2, (short) 0x9000);
        logger.createLog((byte) 0x02, (short) 3, (short) 4, (short) 0x9000);

        assertTrue(logger.getFirstRecord(buffer, (short) 0));
        // getFirstRecord writes [nbtotal_logs(2) | capacity(2) | first record]
        assertEquals(2, ((buffer[0] & 0xFF) << 8) | (buffer[1] & 0xFF), "total logs");
        assertEquals(10, ((buffer[2] & 0xFF) << 8) | (buffer[3] & 0xFF), "capacity");
        assertEquals(0x02, buffer[4] & 0xFF, "most recent instruction");
    }
}
