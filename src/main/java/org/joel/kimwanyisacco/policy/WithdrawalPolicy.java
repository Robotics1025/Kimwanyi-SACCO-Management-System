package org.joel.kimwanyisacco.policy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalPolicy {

    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("20000.00");

    public boolean isWithdrawalAllowed(BigDecimal currentBalance, BigDecimal requestedAmount) {
        if (requestedAmount == null || requestedAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal balanceAfter = currentBalance.subtract(requestedAmount);
        return balanceAfter.compareTo(MINIMUM_BALANCE) >= 0;
    }
}
