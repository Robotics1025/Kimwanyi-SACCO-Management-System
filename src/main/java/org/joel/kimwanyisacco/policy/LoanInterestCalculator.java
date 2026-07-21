package org.joel.kimwanyisacco.policy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class LoanInterestCalculator {

    public BigDecimal calculateInterest(BigDecimal principal) {
        return principal.multiply(new BigDecimal("0.10"));
    }
}
