package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import org.joel.kimwanyisacco.dto.DashboardSummaryDto;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.model.enums.MemberStatus;
import org.joel.kimwanyisacco.repository.LoanRepository;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final LoanRepository loanRepository;
    private final AuditLogService auditLogService;

    public DashboardServiceImpl(MemberRepository memberRepository,
                                SavingsAccountRepository savingsAccountRepository,
                                LoanRepository loanRepository,
                                AuditLogService auditLogService) {
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.loanRepository = loanRepository;
        this.auditLogService = auditLogService;
    }

    @Override
    public DashboardSummaryDto getSummary() {
        DashboardSummaryDto summary = new DashboardSummaryDto();

        try {
            summary.setTotalMembers(memberRepository.count());
            summary.setActiveMembers(memberRepository.countByStatus(MemberStatus.ACTIVE));
            summary.setInactiveMembers(memberRepository.countByStatus(MemberStatus.INACTIVE));
            summary.setSuspendedMembers(memberRepository.countByStatus(MemberStatus.SUSPENDED));
        } catch (Exception e) {
            // fallback defaults
        }

        try {
            BigDecimal totalSavings = savingsAccountRepository.sumAllBalances();
            summary.setTotalSavingsBalance(totalSavings != null ? totalSavings : java.math.BigDecimal.ZERO);
        } catch (Exception e) {
            summary.setTotalSavingsBalance(java.math.BigDecimal.ZERO);
        }
        
        try {
            summary.setPendingLoanCount(loanRepository.countByStatus(LoanStatus.PENDING));
            summary.setActiveLoanCount(loanRepository.countByStatus(LoanStatus.ACTIVE));
            BigDecimal totalLoans = loanRepository.sumOutstandingBalance();
            summary.setTotalOutstandingLoans(totalLoans != null ? totalLoans : java.math.BigDecimal.ZERO);
        } catch (Exception e) {
            summary.setTotalOutstandingLoans(java.math.BigDecimal.ZERO);
        }

        try {
            summary.setRecentActivity(auditLogService.findRecent(10));
        } catch (Exception e) {
            summary.setRecentActivity(java.util.Collections.emptyList());
        }

        return summary;
    }
}
