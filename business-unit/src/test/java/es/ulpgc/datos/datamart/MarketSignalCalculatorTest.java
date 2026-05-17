package es.ulpgc.datos.datamart;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarketSignalCalculatorTest {
    private MarketSignalCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new MarketSignalCalculator();
    }

    @Test
    void shouldCalculateZeroVolatilityWhenMinPriceIsInvalid() {
        double ratio = calculator.calculateVolatilityRatio(0.0, 100.0);

        assertEquals(0.0, ratio);
    }

    @Test
    void shouldCalculateVolatilityRatioCorrectly() {
        double ratio = calculator.calculateVolatilityRatio(100.0, 105.0);

        assertEquals(0.05, ratio, 0.0001);
    }

    @Test
    void shouldActivateHypeWarningWhenVolatilityIsHighAndNewsVolumeIsHigh() {
        boolean hypeWarning = calculator.calculateHypeWarning(0.04, 10);

        assertTrue(hypeWarning);
    }

    @Test
    void shouldNotActivateHypeWarningWhenVolatilityIsLow() {
        boolean hypeWarning = calculator.calculateHypeWarning(0.01, 10);

        assertFalse(hypeWarning);
    }

    @Test
    void shouldReturnFavorableWhenVolatilityIsLowAndSentimentIsPositive() {
        String signal = calculator.calculateSignal(0.01, 3, 0.35, false);

        assertEquals("Favorable", signal);
    }

    @Test
    void shouldReturnNeutralWhenSignalsAreMixed() {
        String signal = calculator.calculateSignal(0.008, 4, 0.02, false);

        assertEquals("Neutral", signal);
    }

    @Test
    void shouldReturnRiskyWhenVolatilityIsHigh() {
        String signal = calculator.calculateSignal(0.05, 2, 0.20, false);

        assertEquals("Riesgoso", signal);
    }

    @Test
    void shouldReturnRiskyWhenSentimentIsNegative() {
        String signal = calculator.calculateSignal(0.01, 6, -0.45, false);

        assertEquals("Riesgoso", signal);
    }

    @Test
    void shouldReturnRiskyWhenThereIsHypeScenario() {
        String signal = calculator.calculateSignal(0.04, 12, 0.30, true);

        assertEquals("Riesgoso", signal);
    }
}