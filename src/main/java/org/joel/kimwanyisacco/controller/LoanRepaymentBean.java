package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.List;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("loanRepaymentBean")
@RequestScope
public class LoanRepaymentBean {
    private final LoanService loanService;
    private final MemberRepository memberRepository;
    private final UserSessionBean session;
    private List<Loan> loans = List.of();
    private Long loanId;
    private BigDecimal amount;

    public LoanRepaymentBean(LoanService loanService, MemberRepository memberRepository, UserSessionBean session) {
        this.loanService = loanService;
        this.memberRepository = memberRepository;
        this.session = session;
    }

    @PostConstruct
    public void init() {
        Member member = memberRepository.findByUserAccountId(session.getLoggedInUser().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        loans = loanService.getLoansByMember(member.getId()).stream()
                .filter(loan -> loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE)
                .toList();
        if (loans.size() == 1) loanId = loans.get(0).getId();
    }

    public void repay() {
        try {
            if (loans.stream().noneMatch(loan -> loan.getId().equals(loanId))) {
                throw new SecurityException("The selected loan does not belong to this member");
            }
            LoanRepaymentForm form = new LoanRepaymentForm();
            form.setLoanId(loanId);
            form.setAmount(amount);
            loanService.repayLoan(form);
            amount = null;
            init();
            FacesMessageUtil.addInfoMessage("Loan repayment recorded successfully");
        } catch (RuntimeException exception) {
            FacesMessageUtil.addErrorMessage(exception.getMessage());
        }
    }

    public List<Loan> getLoans() { return loans; }
    public Long getLoanId() { return loanId; }
    public void setLoanId(Long loanId) { this.loanId = loanId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
