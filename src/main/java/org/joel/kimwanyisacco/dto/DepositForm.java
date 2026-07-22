package org.joel.kimwanyisacco.dto;

import java.math.BigDecimal;

public class DepositForm {

    private Long savingsAccountId;
    private BigDecimal amount;
    private String description;

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

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
