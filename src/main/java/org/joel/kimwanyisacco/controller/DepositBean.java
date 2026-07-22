package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.DepositForm;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.SessionScope;

@Component("depositBean")
@SessionScope
public class DepositBean {

    private final SavingsService savingsService;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserSessionBean userSessionBean;
    private final InternalTransferBean internalTransferBean;

    private Long accountId;
    private BigDecimal amount;

    public DepositBean(
            SavingsService savingsService,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserSessionBean userSessionBean,
            InternalTransferBean internalTransferBean
    ) {
        this.savingsService = savingsService;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userSessionBean = userSessionBean;
        this.internalTransferBean = internalTransferBean;
    }

    @PostConstruct
    public void init() {
        try {
            if (userSessionBean.getLoggedInUser() == null) return;
            Member member = memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId()).orElse(null);
            if (member == null) return;
            SavingsAccount account = savingsAccountRepository.findByMemberId(member.getId()).orElse(null);
            if (account != null) {
                this.accountId = account.getId();
            }
        } catch (Exception ignored) {
        }
    }

    public void deposit() {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            FacesMessageUtil.addErrorMessage("Please enter a valid deposit amount.");
            return;
        }

        try {
            DepositForm form = new DepositForm();
            form.setSavingsAccountId(accountId);
            form.setAmount(amount);

            savingsService.deposit(form);

            // Refresh other beans that depend on the balance
            internalTransferBean.refreshBalance();

            FacesMessageUtil.addInfoMessage("Deposit of UGX " + amount.toPlainString() + " was successful!");
            amount = null; // Clear amount for next time
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
