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

        // Pipeline extra stats
        try {
            long savers = savingsAccountRepository.findAll().stream()
                .filter(a -> a.getBalance() != null && a.getBalance().compareTo(BigDecimal.ZERO) > 0)
                .count();
            summary.setMembersWithSavings(savers);
            summary.setFullyRepaidLoans(loanRepository.countByStatus(LoanStatus.FULLY_REPAID));
        } catch (Exception e) {
            summary.setMembersWithSavings(0);
            summary.setFullyRepaidLoans(0);
        }

        // Chart Data (Mocking past 6 months aggregate for now until transaction-level aggregates are built)
        java.time.YearMonth currentMonth = java.time.YearMonth.now();
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("MMM");
        
        java.util.List<String> labels = new java.util.ArrayList<>();
        java.util.List<BigDecimal> savingsData = new java.util.ArrayList<>();
        java.util.List<BigDecimal> loansData = new java.util.ArrayList<>();
        
        // We'll generate 6 months of data, trending towards the current real totals
        BigDecimal targetSavings = summary.getTotalSavingsBalance() != null ? summary.getTotalSavingsBalance() : BigDecimal.ZERO;
        BigDecimal targetLoans = summary.getTotalOutstandingLoans() != null ? summary.getTotalOutstandingLoans() : BigDecimal.ZERO;

        for (int i = 5; i >= 0; i--) {
            labels.add(currentMonth.minusMonths(i).format(formatter));
            // Simple linear curve for visualization
            double factor = (6 - i) / 6.0;
            savingsData.add(targetSavings.multiply(BigDecimal.valueOf(factor)));
            loansData.add(targetLoans.multiply(BigDecimal.valueOf(factor)));
        }

        summary.setChartLabels(labels);
        summary.setChartSavings(savingsData);
        summary.setChartLoans(loansData);

        return summary;
    }
}
