package hotiron.ui;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.Color;

import org.junit.jupiter.api.Test;

class HotIronBluePaletteTest {

    @Test
    void testSize() {
        HotIronBluePalette palette = new HotIronBluePalette();
        assertTrue(palette.size() > 200);  // from the init string
    }

    @Test
    void testGetColor() {
        HotIronBluePalette palette = new HotIronBluePalette();
        Color c0 = palette.getColor(0);
        assertEquals(0, c0.getRed());
        assertEquals(0, c0.getGreen());
        assertEquals(0, c0.getBlue());

        Color cLast = palette.getColor(palette.size() - 1);
        assertEquals(255, cLast.getRed());
        assertEquals(255, cLast.getGreen());
        assertEquals(255, cLast.getBlue());
    }

    @Test
    void testGetColorNormalized() {
        HotIronBluePalette palette = new HotIronBluePalette();
        int size = palette.size();

        Color c0 = palette.getColorNormalized(0.0);
        assertEquals(palette.getColor(0), c0);

        Color c1 = palette.getColorNormalized(1.0);
        assertEquals(palette.getColor(size - 1), c1);

        Color cMid = palette.getColorNormalized(0.5);
        int expectedIndex = (int) (size * 0.5);
        if (expectedIndex >= size) expectedIndex = size - 1;
        assertEquals(palette.getColor(expectedIndex), cMid);

        // out of range
        Color cNeg = palette.getColorNormalized(-0.5);
        assertEquals(palette.getColor(0), cNeg);

        Color cOver = palette.getColorNormalized(1.5);
        assertEquals(palette.getColor(size - 1), cOver);
    }

    @Test
    void testImplementsColorPalette() {
        ColorPalette palette = new HotIronBluePalette();
        assertNotNull(palette.getColor(10));
        assertTrue(palette.size() > 0);
        assertNotNull(palette.getColorNormalized(0.3));
    }
}
