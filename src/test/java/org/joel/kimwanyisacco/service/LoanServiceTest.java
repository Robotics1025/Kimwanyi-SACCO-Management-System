package org.joel.kimwanyisacco.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.policy.LoanEligibilityPolicy;
import org.joel.kimwanyisacco.policy.LoanInterestCalculator;
import org.joel.kimwanyisacco.repository.LoanRepaymentRepository;
import org.joel.kimwanyisacco.repository.LoanRepository;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock private LoanRepository loanRepository;
    @Mock private LoanRepaymentRepository loanRepaymentRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private UserAccountRepository userAccountRepository;
    @Mock private AuditLogService auditLogService;
    @Mock private NotificationService notificationService;

    private final LoanEligibilityPolicy loanEligibilityPolicy = new LoanEligibilityPolicy();
    private final LoanInterestCalculator loanInterestCalculator = new LoanInterestCalculator();

    private LoanServiceImpl service() {
        return new LoanServiceImpl(loanRepository, loanRepaymentRepository, memberRepository,
                savingsAccountRepository, userAccountRepository, loanEligibilityPolicy,
                loanInterestCalculator, auditLogService, notificationService);
    }

    private Member memberWithUserAccount(Long memberId, String username) {
        UserAccount userAccount = new UserAccount();
        userAccount.setUsername(username);
        Member member = new Member();
        member.setId(memberId);
        member.setUserAccount(userAccount);
        return member;
    }

    @Test
    void applyLoanNotifiesAllAdmins() {
        Member member = memberWithUserAccount(1L, "jkamau");
        when(memberRepository.findById(1L)).thenReturn(Optional.of(member));

        SavingsAccount account = new SavingsAccount();
        account.setBalance(new BigDecimal("100000.00"));
        when(savingsAccountRepository.findByMemberId(1L)).thenReturn(Optional.of(account));
        when(loanRepository.findByMemberId(1L)).thenReturn(List.of());
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanApplicationForm form = new LoanApplicationForm();
        form.setMemberId(1L);
        form.setPrincipalAmount(new BigDecimal("50000.00"));
        form.setTermMonths(6);

        service().applyLoan(form);

        verify(notificationService).notifyAdmins(eq(NotificationType.LOAN_APPLICATION), anyString(), anyString());
    }

    @Test
    void decideLoanApprovedNotifiesTheApplicant() {
        Member member = memberWithUserAccount(1L, "jkamau");
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setMember(member);
        loan.setPrincipal(new BigDecimal("50000.00"));
        loan.setStatus(LoanStatus.PENDING);
        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanDecisionForm form = new LoanDecisionForm();
        form.setLoanId(5L);
        form.setApproved(true);

        service().decideLoan(form, 2L);

        verify(notificationService).notify(eq(member.getUserAccount()), eq(NotificationType.LOAN_APPROVED), anyString(), anyString());
    }

    @Test
    void decideLoanRejectedNotifiesTheApplicant() {
        Member member = memberWithUserAccount(1L, "jkamau");
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setMember(member);
        loan.setPrincipal(new BigDecimal("50000.00"));
        loan.setStatus(LoanStatus.PENDING);
        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        UserAccount admin = new UserAccount();
        admin.setUsername("admin");
        when(userAccountRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanDecisionForm form = new LoanDecisionForm();
        form.setLoanId(5L);
        form.setApproved(false);
        form.setRemarks("Insufficient collateral");

        service().decideLoan(form, 2L);

        verify(notificationService).notify(eq(member.getUserAccount()), eq(NotificationType.LOAN_REJECTED), anyString(), anyString());
    }

    @Test
    void repayLoanNotifiesTheApplicant() {
        Member member = memberWithUserAccount(1L, "jkamau");
        Loan loan = new Loan();
        loan.setId(5L);
        loan.setMember(member);
        loan.setStatus(LoanStatus.ACTIVE);
        loan.setOutstandingBalance(new BigDecimal("20000.00"));
        loan.setAmountRepaid(BigDecimal.ZERO);
        when(loanRepository.findById(5L)).thenReturn(Optional.of(loan));

        LoanRepaymentForm form = new LoanRepaymentForm();
        form.setLoanId(5L);
        form.setAmount(new BigDecimal("5000.00"));

        service().repayLoan(form);

        verify(notificationService).notify(eq(member.getUserAccount()), eq(NotificationType.LOAN_REPAYMENT), anyString(), anyString());
    }
}
