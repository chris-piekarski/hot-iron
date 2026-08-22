package hotiron.mvc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import hotiron.mvc.ModelValue.ModelValueBoolean;
import hotiron.mvc.ModelValue.ModelValueInt;

class ModelValueTest {

    @Test
    void testBasicSetGetAndListener() {
        ModelValueInt mv = new ModelValueInt("test", 10);
        assertEquals(10, mv.getValue());
        assertEquals("test", mv.toString());

        AtomicInteger calls = new AtomicInteger(0);
        mv.addListener((Consumer<Integer>) v -> calls.incrementAndGet());

        mv.setValue(20);
        assertEquals(20, mv.getValue());
        assertEquals(1, calls.get());
    }

    @Test
    void testBooleanModel() {
        ModelValueBoolean b = new ModelValueBoolean("flag", true);
        assertTrue(b.getValue());

        b.setValue(false);
        assertFalse(b.getValue());
    }

    @Test
    void testIntWithBounds() {
        ModelValueInt i = new ModelValueInt("gain", 5, 1, 0, 10);
        assertEquals(5, i.getValue());
        assertEquals(1, i.getStep());
        assertEquals(0, i.getMin());
        assertEquals(10, i.getMax());
        assertThrows(IllegalStateException.class, () -> i.setValue(11));
        assertEquals(5, i.getValue());
    }

    @Test
    void testMultipleListenersAndEqualsNoOp() {
        ModelValueInt mv = new ModelValueInt("val", 0);
        AtomicInteger c1 = new AtomicInteger(0);
        AtomicInteger c2 = new AtomicInteger(0);

        mv.addListener((Consumer<Integer>) v -> c1.incrementAndGet());
        mv.addListener((Runnable) c2::incrementAndGet);

        mv.setValue(1);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());

        mv.setValue(1);
        assertEquals(1, c1.get());
        assertEquals(1, c2.get());
    }
}
