package hotiron.core;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class EMATest {

    @Test
    void testStaticCalculate() {
        double result = EMA.calculate(10.0, 5.0, 5.0);
        // k = 2 / (5+1) = 1/3
        // result = 10*(1/3) + 5*(2/3) = 3.333... + 3.333... = 6.666...
        assertEquals(20.0 / 3.0, result, 1e-9);
    }

    @Test
    void testInstanceAddNewValue() {
        EMA ema = new EMA(4);
        // ema starts at 0 internally
        // k = 2/(4+1) = 0.4
        double first = ema.addNewValue(10.0);
        assertEquals(4.0, first, 1e-9);   // 10*0.4 + 0*0.6

        double second = ema.addNewValue(20.0);
        // k still 0.4: 20*0.4 + 4*0.6 = 8 + 2.4 = 10.4
        assertEquals(10.4, second, 1e-9);
    }

    @Test
    void testTimeDependent() {
        double result = EMA.calculateTimeDependent(100.0, 50.0, 100, 500);
        assertTrue(result > 50.0 && result < 100.0);
    }

    @Test
    void testGetEmaAfterAdds() {
        EMA ema = new EMA(3);
        ema.addNewValue(10);
        ema.addNewValue(20);
        assertTrue(ema.getEma() > 0);
    }
}
