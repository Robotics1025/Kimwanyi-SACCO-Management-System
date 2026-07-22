package org.joel.kimwanyisacco.policy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class WithdrawalPolicyTest {

    private final WithdrawalPolicy policy = new WithdrawalPolicy();

    @Test
    void allowsWithdrawalThatLeavesExactlyMinimumBalance() {
        assertTrue(policy.isWithdrawalAllowed(new BigDecimal("50000.00"), new BigDecimal("30000.00")));
    }

    @Test
    void blocksWithdrawalThatWouldDropBelowMinimumBalance() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("30000.00"), new BigDecimal("15000.00")));
    }

    @Test
    void blocksZeroAmount() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("50000.00"), BigDecimal.ZERO));
    }

    @Test
    void blocksNegativeAmount() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("50000.00"), new BigDecimal("-100.00")));
    }

    @Test
    void blocksAmountExceedingBalance() {
        assertFalse(policy.isWithdrawalAllowed(new BigDecimal("10000.00"), new BigDecimal("20000.00")));
    }
}
