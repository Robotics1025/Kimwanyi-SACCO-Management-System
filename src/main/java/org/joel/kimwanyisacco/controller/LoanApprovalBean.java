package org.joel.kimwanyisacco.controller;

import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.LoanDecisionForm;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.service.LoanService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.annotation.PostConstruct;

@Component("loanApprovalBean")
@RequestScope
public class LoanApprovalBean {

    private final LoanService loanService;
    private final UserSessionBean userSessionBean;

    private List<Loan> pendingLoans;
    private Long selectedLoanId;
    private String rejectionReason;

    public LoanApprovalBean(LoanService loanService, UserSessionBean userSessionBean) {
        this.loanService = loanService;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        loadPendingLoans();
    }

    public void loadPendingLoans() {
        try {
            pendingLoans = loanService.getPendingLoans();
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage("Failed to load pending loans: " + e.getMessage());
        }
    }

    public void approve(Long loanId) {
        try {
            if (!userSessionBean.isLoggedIn()) {
                throw new IllegalStateException("You must be logged in to approve loans");
            }
            LoanDecisionForm form = new LoanDecisionForm();
            form.setLoanId(loanId);
            form.setApproved(true);
            
            loanService.decideLoan(form, userSessionBean.getLoggedInUser().getId());
            loadPendingLoans();
            FacesMessageUtil.addInfoMessage("Loan approved successfully");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    public void reject() {
        try {
            if (!userSessionBean.isLoggedIn()) {
                throw new IllegalStateException("You must be logged in to reject loans");
            }
            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                throw new IllegalArgumentException("Rejection reason is required");
            }
            LoanDecisionForm form = new LoanDecisionForm();
            form.setLoanId(selectedLoanId);
            form.setApproved(false);
            form.setRemarks(rejectionReason);

            loanService.decideLoan(form, userSessionBean.getLoggedInUser().getId());
            
            rejectionReason = null;
            selectedLoanId = null;
            
            loadPendingLoans();
            FacesMessageUtil.addInfoMessage("Loan rejected successfully");
        } catch (Exception e) {
            FacesMessageUtil.addErrorMessage(e.getMessage());
        }
    }

    // Getters and Setters
    public List<Loan> getPendingLoans() {
        return pendingLoans;
    }

    public Long getSelectedLoanId() {
        return selectedLoanId;
    }

    public void setSelectedLoanId(Long selectedLoanId) {
        this.selectedLoanId = selectedLoanId;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
}
