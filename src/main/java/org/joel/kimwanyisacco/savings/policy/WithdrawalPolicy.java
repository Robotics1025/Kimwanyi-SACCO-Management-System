package org.joel.kimwanyisacco.savings.policy;

import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class WithdrawalPolicy {

    public boolean isWithdrawalAllowed(BigDecimal currentBalance, BigDecimal requestedAmount) {
        throw new UnsupportedOperationException("not implemented");
    }
}
