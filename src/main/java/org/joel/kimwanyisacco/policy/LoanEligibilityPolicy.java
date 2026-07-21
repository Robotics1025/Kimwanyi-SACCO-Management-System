package org.joel.kimwanyisacco.policy;

import java.math.BigDecimal;
import java.util.List;
import org.joel.kimwanyisacco.common.exception.LoanNotEligibleException;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.springframework.stereotype.Component;

@Component
public class LoanEligibilityPolicy {

    public void verifyEligibility(Member member, SavingsAccount savingsAccount, BigDecimal requestedAmount, List<Loan> memberLoans) {
        for (Loan loan : memberLoans) {
            if (loan.getStatus() == LoanStatus.ACTIVE || loan.getStatus() == LoanStatus.OVERDUE || loan.getStatus() == LoanStatus.PENDING) {
                throw new LoanNotEligibleException("Member already has an active or pending loan application.");
            }
        }

        BigDecimal savingsBalance = savingsAccount != null ? savingsAccount.getBalance() : BigDecimal.ZERO;
        BigDecimal maxLoanAmount = savingsBalance.multiply(new BigDecimal("3"));
        if (requestedAmount.compareTo(maxLoanAmount) > 0) {
            throw new LoanNotEligibleException("Requested loan amount exceeds the maximum allowed limit of three times savings balance.");
        }
    }
}
