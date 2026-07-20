package org.joel.kimwanyisacco.savings.controller;

import org.joel.kimwanyisacco.savings.dto.DepositForm;
import org.joel.kimwanyisacco.savings.service.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("depositBean")
@RequestScope
public class DepositBean {

    private final SavingsService savingsService;

    private DepositForm depositForm = new DepositForm();

    public DepositBean(SavingsService savingsService) {
        this.savingsService = savingsService;
    }

    public DepositForm getDepositForm() {
        return depositForm;
    }

    public void setDepositForm(DepositForm depositForm) {
        this.depositForm = depositForm;
    }

    public String deposit() {
        return null;
    }
}
