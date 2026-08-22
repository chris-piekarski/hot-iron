package hotiron.core.jfc;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.awt.BasicStroke;
import java.awt.Color;

import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.junit.jupiter.api.Test;

class XYLineAndShapeRendererApiTest {

    @Test
    void jfreeChart15DefaultRendererApi() {
        // JFreeChart 1.5 removed setBase* in favor of setDefault*.
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
        assertDoesNotThrow(() -> {
            renderer.setDefaultShapesVisible(false);
            renderer.setDefaultStroke(new BasicStroke(1.0f));
            renderer.setDefaultPaint(Color.white);
        });
    }
}
