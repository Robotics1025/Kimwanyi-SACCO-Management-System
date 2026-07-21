package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("adminSavingsBean")
@SessionScope
public class AdminSavingsBean implements Serializable {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsService savingsService;

    private List<SavingsAccount> accounts;
    private SavingsAccount selectedAccount;
    
    private BigDecimal amount;
    private String description;
    
    // Type of transaction currently being modal-ed ("DEPOSIT" or "WITHDRAW")
    private String transactionType;

    public AdminSavingsBean(SavingsAccountRepository savingsAccountRepository, SavingsService savingsService) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsService = savingsService;
    }

    @PostConstruct
    public void init() {
        refreshAccounts();
    }

    public void refreshAccounts() {
        this.accounts = savingsAccountRepository.findAll();
    }

    public void prepareTransaction(SavingsAccount account, String type) {
        this.selectedAccount = account;
        this.transactionType = type;
        this.amount = null;
        this.description = null;
    }

    public void processTransaction() {
        if (selectedAccount == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            FacesMessageUtil.addErrorMessage("Invalid amount specified.");
            return;
        }

        try {
            if ("DEPOSIT".equals(transactionType)) {
                org.joel.kimwanyisacco.dto.DepositForm form = new org.joel.kimwanyisacco.dto.DepositForm();
                form.setSavingsAccountId(selectedAccount.getId());
                form.setAmount(amount);
                form.setDescription(description != null && !description.isBlank() ? description : "Admin Cash Deposit");
                savingsService.deposit(form);
                FacesMessageUtil.addInfoMessage("Successfully deposited UGX " + amount);
            } else if ("WITHDRAW".equals(transactionType)) {
                org.joel.kimwanyisacco.dto.WithdrawalForm form = new org.joel.kimwanyisacco.dto.WithdrawalForm();
                form.setSavingsAccountId(selectedAccount.getId());
                form.setAmount(amount);
                form.setDescription(description != null && !description.isBlank() ? description : "Admin Cash Withdrawal");
                savingsService.withdraw(form);
                FacesMessageUtil.addInfoMessage("Successfully withdrawn UGX " + amount);
            }
            refreshAccounts();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    // Getters and Setters
    public List<SavingsAccount> getAccounts() { return accounts; }
    public SavingsAccount getSelectedAccount() { return selectedAccount; }
    public void setSelectedAccount(SavingsAccount selectedAccount) { this.selectedAccount = selectedAccount; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getTransactionType() { return transactionType; }
    public void setTransactionType(String transactionType) { this.transactionType = transactionType; }
}
