package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyVetoException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

class FrequencySelectorPanelTest {

    @Test
    void testInitialValueAndGetSet() {
        FrequencySelectorPanel panel = new FrequencySelectorPanel(100, 1000, 1, 500);
        assertEquals(500, panel.getValue());

        assertTrue(panel.setValue(750));
        assertEquals(750, panel.getValue());

        // out of range
        assertFalse(panel.setValue(50));
        assertEquals(750, panel.getValue());

        assertTrue(panel.setValue(750));
        assertEquals(750, panel.getValue());
    }

    @Test
    void testAddSubtractDigits() {
        FrequencySelectorPanel panel = new FrequencySelectorPanel(0, 9999, 1, 1234);
        java.awt.Component[] children = panel.getComponents();
        javax.swing.JButton plusUnits = (javax.swing.JButton) children[3];
        javax.swing.JButton plusTens = (javax.swing.JButton) children[2];
        javax.swing.JButton minusThousands = (javax.swing.JButton) children[8];

        plusUnits.doClick();
        assertEquals(1235, panel.getValue());
        plusTens.doClick();
        assertEquals(1245, panel.getValue());
        minusThousands.doClick();
        assertEquals(245, panel.getValue());
    }

    @Test
    void digitButtonsClampAtMinAndMax() {
        FrequencySelectorPanel panel = new FrequencySelectorPanel(100, 200, 1, 200);
        javax.swing.JButton plusThousands = (javax.swing.JButton) panel.getComponents()[0];
        plusThousands.doClick();
        assertEquals(200, panel.getValue());

        panel.setValue(100);
        javax.swing.JButton minusUnits = (javax.swing.JButton) panel.getComponents()[11];
        minusUnits.doClick();
        assertEquals(100, panel.getValue());
    }

    @Test
    void testVetoableAndPropertyChangeOnSet() throws PropertyVetoException {
        FrequencySelectorPanel panel = new FrequencySelectorPanel(0, 9999, 1, 100);

        AtomicReference<Integer> lastOld = new AtomicReference<>();
        AtomicReference<Integer> lastNew = new AtomicReference<>();
        AtomicInteger vetoCount = new AtomicInteger(0);

        panel.addVetoableChangeListener(evt -> {
            vetoCount.incrementAndGet();
            lastOld.set((Integer) evt.getOldValue());
            lastNew.set((Integer) evt.getNewValue());
        });

        panel.addPropertyChangeListener("value", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                // just to have listener
            }
        });

        panel.setValue(200);

        assertEquals(1, vetoCount.get());
        assertEquals(100, lastOld.get().intValue());
        assertEquals(200, lastNew.get().intValue());
        assertEquals(200, panel.getValue());
    }

    @Test
    void testDigitExtractionInDisplay() {
        // setValue updates internal text fields, but since private, we test via getValue
        FrequencySelectorPanel panel = new FrequencySelectorPanel(0, 9999, 1, 1234);
        assertEquals(1234, panel.getValue());
        panel.setValue(5);
        assertEquals(5, panel.getValue());
    }
}
