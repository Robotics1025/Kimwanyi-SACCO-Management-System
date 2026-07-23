package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.joel.kimwanyisacco.dto.ActivityItem;
import org.joel.kimwanyisacco.dto.ActivityItem.Category;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.joel.kimwanyisacco.service.SavingsService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("memberDashboardBean")
@RequestScope
public class MemberDashboardBean {

    private final LoanService loanService;
    private final SavingsService savingsService;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserSessionBean userSessionBean;

    // Recent unified activity
    private List<ActivityItem> recentActivity = List.of();

    // Loan summary
    private BigDecimal outstandingLoanBalance = BigDecimal.ZERO;
    private Loan activeLoan;
    private Loan latestLoan;
    private String loanJourneyStep = "3"; // "3" = no loan yet, "4" = pending, "5" = active/repaying

    public MemberDashboardBean(
            LoanService loanService,
            SavingsService savingsService,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserSessionBean userSessionBean
    ) {
        this.loanService = loanService;
        this.savingsService = savingsService;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        try {
            Member member = memberRepository.findByUserAccountId(
                    userSessionBean.getLoggedInUser().getId()).orElse(null);
            if (member == null) return;

            List<ActivityItem> items = new ArrayList<>();

            // --- Savings transactions ---
            SavingsAccount account = savingsAccountRepository.findByMemberId(member.getId()).orElse(null);
            if (account != null) {
                savingsService.getTransactionHistory(account.getId()).forEach(tx -> {
                    boolean isCredit = "DEPOSIT".equals(tx.getTransactionType())
                            || "TRANSFER_IN".equals(tx.getTransactionType())
                            || "INTEREST".equals(tx.getTransactionType());
                    items.add(new ActivityItem(
                            tx.getCreatedAt(),
                            formatTxType(tx.getTransactionType()),
                            tx.getDescription() != null ? tx.getDescription() : "Savings transaction",
                            tx.getAmount(),
                            isCredit,
                            Category.SAVINGS
                    ));
                });
            }

            // --- Loan activities ---
            List<Loan> loans = loanService.getLoansByMember(member.getId());
            latestLoan = loans.stream().max(Comparator.comparing(Loan::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder()))).orElse(null);
            for (Loan loan : loans) {
                // Loan application event
                if (loan.getApplicationDate() != null) {
                    items.add(new ActivityItem(
                            loan.getApplicationDate().atStartOfDay(),
                            "Loan Application",
                            "Applied for UGX " + String.format("%,.0f", loan.getPrincipal().doubleValue()),
                            loan.getPrincipal(),
                            true,
                            Category.LOAN
                    ));
                }
                // Loan status event (if not just pending)
                if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.APPROVED) {
                    items.add(new ActivityItem(
                            loan.getCreatedAt() != null ? loan.getCreatedAt().plusHours(1) : LocalDateTime.now(),
                            "Loan " + loan.getStatus().name(),
                            "Loan of UGX " + String.format("%,.0f", loan.getPrincipal().doubleValue()) + " " + loan.getStatus().name().toLowerCase(),
                            loan.getPrincipal(),
                            true,
                            Category.LOAN
                    ));
                }
                if (loan.getStatus() == LoanStatus.REJECTED) {
                    items.add(new ActivityItem(
                            loan.getCreatedAt() != null ? loan.getCreatedAt().plusHours(1) : LocalDateTime.now(),
                            "Loan Rejected",
                            "Your loan application was rejected",
                            loan.getPrincipal(),
                            false,
                            Category.LOAN
                    ));
                }

                // Determine active loan for outstanding balance & tracker
                if (loan.getStatus() == LoanStatus.PENDING
                        || loan.getStatus() == LoanStatus.ACTIVE
                        || loan.getStatus() == LoanStatus.OVERDUE) {
                    if (activeLoan == null) activeLoan = loan;
                }
            }

            // Compute outstanding balance
            if (activeLoan != null) {
                outstandingLoanBalance = activeLoan.getPrincipal();
                loanJourneyStep = activeLoan.getStatus() == LoanStatus.PENDING ? "4" : "5";
            }

            // Sort by timestamp descending and take top 7
            recentActivity = items.stream()
                    .sorted(Comparator.comparing(ActivityItem::getTimestamp).reversed())
                    .limit(7)
                    .toList();

        } catch (Exception ignored) {
        }
    }

    private String formatTxType(String type) {
        if (type == null) return "Transaction";
        return switch (type) {
            case "DEPOSIT" -> "Deposit";
            case "WITHDRAW", "WITHDRAWAL" -> "Withdrawal";
            case "TRANSFER_OUT" -> "Transfer Out";
            case "TRANSFER_IN" -> "Transfer In";
            case "INTEREST" -> "Interest Credit";
            default -> type;
        };
    }

    public List<ActivityItem> getRecentActivity() { return recentActivity; }
    public BigDecimal getOutstandingLoanBalance() { return outstandingLoanBalance; }
    public Loan getActiveLoan() { return activeLoan; }
    public String getActiveLoanStatus() {
        return activeLoan != null ? activeLoan.getStatus().name() : "";
    }
    public String getLoanJourneyStep() { return loanJourneyStep; }
    public boolean isHasActiveLoan() { return activeLoan != null; }
    public boolean isHasLoanJourney() { return latestLoan != null; }
    public String getLoanJourneyStatus() { return latestLoan == null ? "NONE" : latestLoan.getStatus().name(); }
    public String getLoanJourneyTitle() {
        if (latestLoan == null) return "No loan application yet";
        return switch (latestLoan.getStatus()) {
            case PENDING -> "Application under review";
            case APPROVED -> "Loan approved";
            case ACTIVE -> "Repayment in progress";
            case OVERDUE -> "Repayment overdue";
            case FULLY_REPAID -> "Loan fully repaid";
            case REJECTED -> "Application not approved";
        };
    }
    public String getLoanJourneyNextAction() {
        if (latestLoan == null) return "Apply when you are ready and eligible.";
        return switch (latestLoan.getStatus()) {
            case PENDING -> "The SACCO administrator will review your application.";
            case APPROVED -> "Wait for disbursement confirmation.";
            case ACTIVE -> "Continue making repayments before the due date.";
            case OVERDUE -> "Make a repayment or contact the SACCO immediately.";
            case FULLY_REPAID -> "You may apply for a new loan when needed.";
            case REJECTED -> "Review the decision remarks before applying again.";
        };
    }
    public int getLoanRepaymentPercent() {
        if (latestLoan == null || latestLoan.getTotalRepayable() == null
                || latestLoan.getTotalRepayable().signum() <= 0 || latestLoan.getAmountRepaid() == null) return 0;
        return latestLoan.getAmountRepaid().multiply(BigDecimal.valueOf(100))
                .divide(latestLoan.getTotalRepayable(), 0, java.math.RoundingMode.HALF_UP).min(BigDecimal.valueOf(100)).intValue();
    }
}
