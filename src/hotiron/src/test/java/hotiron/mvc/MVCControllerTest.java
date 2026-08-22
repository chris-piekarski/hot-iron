package hotiron.mvc;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.SpinnerListModel;
import javax.swing.SwingUtilities;

import hotiron.mvc.ModelValue;

import org.junit.jupiter.api.Test;

import hotiron.mvc.ModelValue.ModelValueBoolean;
import hotiron.mvc.ModelValue.ModelValueInt;

class MVCControllerTest {

    private static void flushEdt() throws Exception {
        SwingUtilities.invokeAndWait(() -> { });
    }

    @Test
    void testGenericConstructorSyncsModelToView() throws Exception {
        ModelValueInt model = new ModelValueInt("test", 42);
        AtomicReference<Integer> viewValue = new AtomicReference<>(0);

        new MVCController(
            listener -> { },
            viewValue::set,
            model,
            v -> v,
            v -> v
        );

        flushEdt();
        assertEquals(42, viewValue.get().intValue());
    }

    @Test
    void testGenericViewToModel() throws Exception {
        ModelValueInt model = new ModelValueInt("test", 0);
        final Consumer<Integer>[] holder = new Consumer[1];
        MVCController.ViewAddChangeListener<Integer> mockView = cb -> holder[0] = cb;

        new MVCController(
            mockView,
            v -> { },
            model,
            v -> v,
            v -> v
        );

        holder[0].accept(99);
        flushEdt();
        assertEquals(99, model.getValue().intValue());
    }

    @Test
    void testJCheckBoxBinding() throws Exception {
        JCheckBox cb = new JCheckBox();
        ModelValueBoolean model = new ModelValueBoolean("flag", true);
        new MVCController(cb, model);
        flushEdt();

        assertTrue(cb.isSelected());
        assertTrue(model.getValue());

        SwingUtilities.invokeAndWait(cb::doClick);
        flushEdt();
        assertFalse(model.getValue());
    }

    @Test
    void testJSliderBinding() throws Exception {
        JSlider slider = new JSlider(0, 100, 50);
        ModelValueInt model = new ModelValueInt("gain", 50, 1, 0, 100);
        new MVCController(slider, model);
        flushEdt();

        assertEquals(50, slider.getValue());
        assertEquals(50, model.getValue().intValue());

        SwingUtilities.invokeAndWait(() -> slider.setValue(75));
        flushEdt();
        assertEquals(75, model.getValue().intValue());
    }

    @Test
    void testJSpinnerBinding() throws Exception {
        JSpinner spinner = new JSpinner(new SpinnerListModel(new String[] { "8192", "16384", "32768" }));
        ModelValueInt model = new ModelValueInt("samples", 8192);
        new MVCController(spinner, model, val -> Integer.parseInt(val.toString()), val -> val.toString());
        flushEdt();

        assertEquals("8192", spinner.getValue().toString());
    }

    @Test
    void testJComboBoxBinding() throws Exception {
        JComboBox<String> box = new JComboBox<String>(new String[] { "a", "b", "c" });
        ModelValue<String> model = new ModelValue<String>("choice", "a");
        new MVCController(box, model);
        flushEdt();

        assertEquals("a", box.getSelectedItem());
        SwingUtilities.invokeAndWait(() -> box.setSelectedItem("b"));
        flushEdt();
        assertEquals("b", model.getValue());

        model.setValue("c");
        flushEdt();
        assertEquals("c", box.getSelectedItem());
    }
}
