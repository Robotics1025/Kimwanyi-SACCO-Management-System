package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.dto.LoanApplicationForm;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.dto.LoanRepaymentForm;
import org.joel.kimwanyisacco.model.Loan;

public interface LoanService {
    
    Loan applyLoan(LoanApplicationForm form);
    
    Loan decideLoan(LoanDecisionForm form, Long adminUserId);
    
    void repayLoan(LoanRepaymentForm form);
    
    List<Loan> getPendingLoans();
    
    List<Loan> getLoansByMember(Long memberId);
    
    Loan getLoanById(Long loanId);
    int markOverdueLoans();
}
