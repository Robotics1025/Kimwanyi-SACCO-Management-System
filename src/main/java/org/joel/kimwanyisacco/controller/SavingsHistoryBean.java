package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("savingsHistoryBean")
@RequestScope
public class SavingsHistoryBean {

    private final SavingsService savingsService;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserSessionBean userSessionBean;

    private SavingsAccountDto account;
    private List<SavingsTransactionDto> transactions = List.of();

    public SavingsHistoryBean(
            SavingsService savingsService,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserSessionBean userSessionBean
    ) {
        this.savingsService = savingsService;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        try {
            Member member = memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                    .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));

            SavingsAccount savingsAccount = savingsAccountRepository.findByMemberId(member.getId())
                    .orElseThrow(() -> new IllegalStateException("No savings account found for member"));

            account = savingsService.getAccountById(savingsAccount.getId());
            transactions = savingsService.getTransactionHistory(savingsAccount.getId());
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load savings history: " + e.getMessage());
        }
    }

    public SavingsAccountDto getAccount() {
        return account;
    }

    public List<SavingsTransactionDto> getTransactions() {
        return transactions;
    }
}
