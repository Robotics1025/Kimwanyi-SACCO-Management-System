package org.joel.kimwanyisacco.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class SavingsInterestCalculator {

    private static final BigDecimal ANNUAL_RATE = new BigDecimal("0.05");
    private static final BigDecimal MONTHS_PER_YEAR = new BigDecimal("12");

    public BigDecimal calculateInterest(BigDecimal balance) {
        if (balance == null) {
            throw new IllegalArgumentException("Savings balance is required");
        }
        if (balance.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Savings balance cannot be negative");
        }

        return balance.multiply(ANNUAL_RATE)
                .divide(MONTHS_PER_YEAR, 2, RoundingMode.HALF_UP);
    }
}
