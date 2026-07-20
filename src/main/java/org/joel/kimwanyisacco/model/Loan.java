package org.joel.kimwanyisacco.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.joel.kimwanyisacco.model.enums.LoanStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "loans")
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal principal;

    @Column(
            name = "interest_rate",
            nullable = false,
            precision = 5,
            scale = 2
    )
    private BigDecimal interestRate =
            new BigDecimal("10.00");

    @Column(
            name = "interest_amount",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal interestAmount;

    @Column(
            name = "total_repayable",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal totalRepayable;

    @Column(
            name = "amount_repaid",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amountRepaid = BigDecimal.ZERO;

    @Column(
            name = "outstanding_balance",
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.PENDING;

    @Column(nullable = false, length = 500)
    private String purpose;

    @Column(name = "application_date", nullable = false)
    private LocalDate applicationDate;

    @Column(name = "decision_date")
    private LocalDate decisionDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "decided_by")
    private UserAccount decidedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public Loan() {
    }

    @PrePersist
    public void beforeInsert() {
        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;

        if (applicationDate == null) {
            applicationDate = LocalDate.now();
        }

        if (status == null) {
            status = LoanStatus.PENDING;
        }

        if (amountRepaid == null) {
            amountRepaid = BigDecimal.ZERO;
        }

        if (interestRate == null) {
            interestRate = new BigDecimal("10.00");
        }
    }

    @PreUpdate
    public void beforeUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public BigDecimal getPrincipal() {
        return principal;
    }

    public void setPrincipal(BigDecimal principal) {
        this.principal = principal;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public BigDecimal getTotalRepayable() {
        return totalRepayable;
    }

    public void setTotalRepayable(BigDecimal totalRepayable) {
        this.totalRepayable = totalRepayable;
    }

    public BigDecimal getAmountRepaid() {
        return amountRepaid;
    }

    public void setAmountRepaid(BigDecimal amountRepaid) {
        this.amountRepaid = amountRepaid;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public LocalDate getApplicationDate() {
        return applicationDate;
    }

    public void setApplicationDate(LocalDate applicationDate) {
        this.applicationDate = applicationDate;
    }

    public LocalDate getDecisionDate() {
        return decisionDate;
    }

    public void setDecisionDate(LocalDate decisionDate) {
        this.decisionDate = decisionDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public UserAccount getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(UserAccount decidedBy) {
        this.decidedBy = decidedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}