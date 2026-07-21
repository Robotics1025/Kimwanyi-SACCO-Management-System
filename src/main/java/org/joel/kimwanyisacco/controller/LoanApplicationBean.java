package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loanApplicationBean")
@RequestScope
public class LoanApplicationBean {

    private final LoanService loanService;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserSessionBean userSessionBean;

    private Long memberId;
    private BigDecimal maxEligibleAmount = BigDecimal.ZERO;
    private boolean hasActiveLoan;

    private BigDecimal principalAmount;
    private Integer termMonths;

    public LoanApplicationBean(
            LoanService loanService,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserSessionBean userSessionBean
    ) {
        this.loanService = loanService;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        Member member = memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));
        memberId = member.getId();

        SavingsAccount savingsAccount = savingsAccountRepository.findByMemberId(memberId).orElse(null);
        BigDecimal balance = savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;
        maxEligibleAmount = balance.multiply(new BigDecimal("3"));

        List<Loan> memberLoans = loanService.getLoansByMember(memberId);
        hasActiveLoan = memberLoans.stream().anyMatch(loan ->
                loan.getStatus() == LoanStatus.PENDING
                        || loan.getStatus() == LoanStatus.ACTIVE
                        || loan.getStatus() == LoanStatus.OVERDUE);
    }

    public String apply() {
        try {
            LoanApplicationForm form = new LoanApplicationForm();
            form.setMemberId(memberId);
            form.setPrincipalAmount(principalAmount);
            form.setTermMonths(termMonths);

            loanService.applyLoan(form);

            FacesMessageUtil.addInfoMessage("Loan application submitted successfully");
            return "/loans/applications?faces-redirect=true";
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
            return null;
        }
    }

    public BigDecimal getMaxEligibleAmount() {
        return maxEligibleAmount;
    }

    public boolean isHasActiveLoan() {
        return hasActiveLoan;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public Integer getTermMonths() {
        return termMonths;
    }

    public void setTermMonths(Integer termMonths) {
        this.termMonths = termMonths;
    }
}
