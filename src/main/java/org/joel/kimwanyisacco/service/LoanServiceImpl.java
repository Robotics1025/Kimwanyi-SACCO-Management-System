package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.LoanRepayment;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.policy.LoanEligibilityPolicy;
import org.joel.kimwanyisacco.policy.LoanInterestCalculator;
import org.joel.kimwanyisacco.repository.LoanRepository;
import org.joel.kimwanyisacco.repository.LoanRepaymentRepository;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoanServiceImpl implements LoanService {

    private final LoanRepository loanRepository;
    private final LoanRepaymentRepository loanRepaymentRepository;
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository savingsAccountRepository;
    private final UserAccountRepository userAccountRepository;
    private final LoanEligibilityPolicy loanEligibilityPolicy;
    private final LoanInterestCalculator loanInterestCalculator;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;

    public LoanServiceImpl(
            LoanRepository loanRepository,
            LoanRepaymentRepository loanRepaymentRepository,
            MemberRepository memberRepository,
            SavingsAccountRepository savingsAccountRepository,
            UserAccountRepository userAccountRepository,
            LoanEligibilityPolicy loanEligibilityPolicy,
            LoanInterestCalculator loanInterestCalculator,
            AuditLogService auditLogService,
            NotificationService notificationService
    ) {
        this.loanRepository = loanRepository;
        this.loanRepaymentRepository = loanRepaymentRepository;
        this.memberRepository = memberRepository;
        this.savingsAccountRepository = savingsAccountRepository;
        this.userAccountRepository = userAccountRepository;
        this.loanEligibilityPolicy = loanEligibilityPolicy;
        this.loanInterestCalculator = loanInterestCalculator;
        this.auditLogService = auditLogService;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public Loan applyLoan(LoanApplicationForm form) {
        Member member = memberRepository.findById(form.getMemberId())
                .orElseThrow(() -> new IllegalArgumentException("Member not found"));
        SavingsAccount savingsAccount = savingsAccountRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new IllegalArgumentException("Savings account not found for member"));
        List<Loan> memberLoans = loanRepository.findByMemberId(member.getId());

        BigDecimal principal = form.getPrincipalAmount();
        loanEligibilityPolicy.verifyEligibility(member, savingsAccount, principal, memberLoans);

        BigDecimal interest = loanInterestCalculator.calculateInterest(principal);
        BigDecimal totalRepayable = principal.add(interest);

        Loan loan = new Loan();
        loan.setMember(member);
        loan.setPrincipal(principal);
        loan.setInterestRate(new BigDecimal("10.00"));
        loan.setInterestAmount(interest);
        loan.setTotalRepayable(totalRepayable);
        loan.setAmountRepaid(BigDecimal.ZERO);
        loan.setOutstandingBalance(totalRepayable);
        loan.setStatus(LoanStatus.PENDING);
        loan.setPurpose("General Loan");
        loan.setApplicationDate(LocalDate.now());

        Loan saved = loanRepository.save(loan);

        auditLogService.record(member.getUserAccount(), AuditAction.LOAN_APPLIED, "Loan", saved.getId(),
                "Applied for loan: UGX " + principal);

        notificationService.notifyAdmins(NotificationType.LOAN_APPLICATION, "New Loan Application",
                member.getUserAccount().getUsername() + " applied for UGX " + principal);

        return saved;
    }

    @Override
    @Transactional
    public Loan decideLoan(LoanDecisionForm form, Long adminUserId) {
        Loan loan = loanRepository.findById(form.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan application not found"));

        UserAccount admin = userAccountRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("Admin account not found"));

        if (loan.getStatus() != LoanStatus.PENDING) {
            throw new IllegalStateException("Loan is not in PENDING status");
        }

        loan.setDecisionDate(LocalDate.now());
        loan.setDecidedBy(admin);

        if (form.isApproved()) {
            loan.setStatus(LoanStatus.ACTIVE);
            loan.setDueDate(LocalDate.now().plusMonths(12));
            loanRepository.save(loan);

            auditLogService.record(admin, AuditAction.LOAN_APPROVED, "Loan", loan.getId(),
                    "Approved loan for member: " + loan.getMember().getUserAccount().getUsername());

            notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_APPROVED,
                    "Loan Approved", "Your loan of UGX " + loan.getPrincipal() + " was approved.");
        } else {
            loan.setStatus(LoanStatus.REJECTED);
            loan.setRejectionReason(form.getRemarks());
            loanRepository.save(loan);

            auditLogService.record(admin, AuditAction.LOAN_REJECTED, "Loan", loan.getId(),
                    "Rejected loan. Reason: " + form.getRemarks());

            notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_REJECTED,
                    "Loan Rejected", "Your loan application was rejected. Reason: " + form.getRemarks());
        }

        return loan;
    }

    @Override
    @Transactional
    public void repayLoan(LoanRepaymentForm form) {
        Loan loan = loanRepository.findById(form.getLoanId())
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));

        if (loan.getStatus() != LoanStatus.ACTIVE && loan.getStatus() != LoanStatus.OVERDUE) {
            throw new IllegalStateException("Loan is not active or overdue");
        }

        BigDecimal amount = form.getAmount();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Repayment amount must be positive");
        }

        BigDecimal currentOutstanding = loan.getOutstandingBalance();
        if (amount.compareTo(currentOutstanding) > 0) {
            throw new IllegalArgumentException("Repayment amount exceeds outstanding balance");
        }

        BigDecimal newOutstanding = currentOutstanding.subtract(amount);
        BigDecimal newAmountRepaid = loan.getAmountRepaid().add(amount);

        loan.setOutstandingBalance(newOutstanding);
        loan.setAmountRepaid(newAmountRepaid);

        if (newOutstanding.compareTo(BigDecimal.ZERO) == 0) {
            loan.setStatus(LoanStatus.FULLY_REPAID);
        }

        loanRepository.save(loan);

        LoanRepayment repayment = new LoanRepayment();
        repayment.setLoan(loan);
        repayment.setAmount(amount);
        repayment.setBalanceBefore(currentOutstanding);
        repayment.setBalanceAfter(newOutstanding);
        repayment.setPaymentMethod("CASH");
        repayment.setReference("REP-" + System.currentTimeMillis());
        repayment.setPaymentDate(LocalDate.now());

        loanRepaymentRepository.save(repayment);

        auditLogService.record(loan.getMember().getUserAccount(), AuditAction.LOAN_REPAYMENT_RECORDED, "LoanRepayment", repayment.getId(),
                "Repaid UGX " + amount + " for Loan #" + loan.getId());

        notificationService.notify(loan.getMember().getUserAccount(), NotificationType.LOAN_REPAYMENT,
                "Repayment Recorded", "UGX " + amount + " repaid. Outstanding balance: UGX " + newOutstanding + ".");
    }

    @Override
    public List<Loan> getPendingLoans() {
        return loanRepository.findByStatus(LoanStatus.PENDING);
    }

    @Override
    public List<Loan> getLoansByMember(Long memberId) {
        return loanRepository.findByMemberId(memberId);
    }

    @Override
    public Loan getLoanById(Long loanId) {
        return loanRepository.findById(loanId)
                .orElseThrow(() -> new IllegalArgumentException("Loan not found"));
    }
}
