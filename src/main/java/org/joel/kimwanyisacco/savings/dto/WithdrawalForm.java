package org.joel.kimwanyisacco.savings.dto;

import java.math.BigDecimal;

public class WithdrawalForm {

    private Long savingsAccountId;
    private BigDecimal amount;

    public Long getSavingsAccountId() {
        return savingsAccountId;
    }

    public void setSavingsAccountId(Long savingsAccountId) {
        this.savingsAccountId = savingsAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
