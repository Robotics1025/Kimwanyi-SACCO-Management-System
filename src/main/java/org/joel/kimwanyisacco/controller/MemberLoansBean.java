package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.util.Comparator;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("memberLoansBean")
@RequestScope
public class MemberLoansBean {

    private final LoanService loanService;
    private final MemberRepository memberRepository;
    private final UserSessionBean userSessionBean;

    private List<Loan> loans = List.of();

    public MemberLoansBean(LoanService loanService, MemberRepository memberRepository, UserSessionBean userSessionBean) {
        this.loanService = loanService;
        this.memberRepository = memberRepository;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        try {
            Member member = memberRepository.findByUserAccountId(userSessionBean.getLoggedInUser().getId())
                    .orElseThrow(() -> new IllegalStateException("No member found for the logged-in user"));

            loans = loanService.getLoansByMember(member.getId()).stream()
                    .sorted(Comparator.comparing(Loan::getApplicationDate).reversed())
                    .toList();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load your loans: " + e.getMessage());
        }
    }

    public String badgeClass(Loan loan) {
        return switch (loan.getStatus()) {
            case PENDING -> "badge-yellow";
            case ACTIVE -> "badge-blue";
            case FULLY_REPAID -> "badge-green";
            case REJECTED, OVERDUE -> "badge-red";
            case APPROVED -> "badge-gray";
        };
    }

    public List<Loan> getLoans() {
        return loans;
    }
}
