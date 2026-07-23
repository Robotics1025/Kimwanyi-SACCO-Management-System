package org.joel.kimwanyisacco.policy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class SavingsInterestCalculatorTest {

    private final SavingsInterestCalculator calculator = new SavingsInterestCalculator();

    @Test
    void calculatesFivePercentAnnualInterestAppliedMonthly() {
        assertEquals(new BigDecimal("5000.00"), calculator.calculateInterest(new BigDecimal("1200000.00")));
    }

    @Test
    void roundsMonthlyInterestToCurrencyScale() {
        assertEquals(new BigDecimal("83.33"), calculator.calculateInterest(new BigDecimal("20000.00")));
    }

    @Test
    void rejectsMissingOrNegativeBalances() {
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateInterest(null));
        assertThrows(IllegalArgumentException.class, () -> calculator.calculateInterest(new BigDecimal("-1.00")));
    }
}
