package org.joel.kimwanyisacco.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class AccountStatementDto {
    private String memberName;
    private String membershipNumber;
    private String accountNumber;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance = BigDecimal.ZERO;
    private BigDecimal closingBalance = BigDecimal.ZERO;
    private List<StatementEntryDto> entries = List.of();

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
    public String getMembershipNumber() { return membershipNumber; }
    public void setMembershipNumber(String membershipNumber) { this.membershipNumber = membershipNumber; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public BigDecimal getOpeningBalance() { return openingBalance; }
    public void setOpeningBalance(BigDecimal openingBalance) { this.openingBalance = openingBalance; }
    public BigDecimal getClosingBalance() { return closingBalance; }
    public void setClosingBalance(BigDecimal closingBalance) { this.closingBalance = closingBalance; }
    public List<StatementEntryDto> getEntries() { return entries; }
    public void setEntries(List<StatementEntryDto> entries) { this.entries = entries; }
}
