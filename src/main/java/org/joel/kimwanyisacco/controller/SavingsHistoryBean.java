package org.joel.kimwanyisacco.controller;

import java.util.List;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.service.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("savingsHistoryBean")
@RequestScope
public class SavingsHistoryBean {

    private final SavingsService savingsService;

    private List<SavingsTransactionDto> transactions;

    public SavingsHistoryBean(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    public List<SavingsTransactionDto> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<SavingsTransactionDto> transactions) {
        this.transactions = transactions;
    }
}
