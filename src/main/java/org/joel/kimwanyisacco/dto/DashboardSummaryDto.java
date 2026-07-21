package org.joel.kimwanyisacco.dto;

import java.math.BigDecimal;
import java.util.List;

public class DashboardSummaryDto {

    private long totalMembers;
    private long activeMembers;
    private long inactiveMembers;
    private long suspendedMembers;
    private BigDecimal totalSavingsBalance;
    private BigDecimal totalOutstandingLoans;
    private long pendingLoanCount;
    private long activeLoanCount;
    private List<AuditLogDto> recentActivity;

    public long getTotalMembers() { return totalMembers; }
    public void setTotalMembers(long totalMembers) { this.totalMembers = totalMembers; }

    public long getActiveMembers() { return activeMembers; }
    public void setActiveMembers(long activeMembers) { this.activeMembers = activeMembers; }

    public long getInactiveMembers() { return inactiveMembers; }
    public void setInactiveMembers(long inactiveMembers) { this.inactiveMembers = inactiveMembers; }

    public long getSuspendedMembers() { return suspendedMembers; }
    public void setSuspendedMembers(long suspendedMembers) { this.suspendedMembers = suspendedMembers; }

    public BigDecimal getTotalSavingsBalance() { return totalSavingsBalance; }
    public void setTotalSavingsBalance(BigDecimal totalSavingsBalance) { this.totalSavingsBalance = totalSavingsBalance; }

    public BigDecimal getTotalOutstandingLoans() { return totalOutstandingLoans; }
    public void setTotalOutstandingLoans(BigDecimal totalOutstandingLoans) { this.totalOutstandingLoans = totalOutstandingLoans; }

    public long getPendingLoanCount() { return pendingLoanCount; }
    public void setPendingLoanCount(long pendingLoanCount) { this.pendingLoanCount = pendingLoanCount; }

    public long getActiveLoanCount() { return activeLoanCount; }
    public void setActiveLoanCount(long activeLoanCount) { this.activeLoanCount = activeLoanCount; }

    public List<AuditLogDto> getRecentActivity() { return recentActivity; }
    public void setRecentActivity(List<AuditLogDto> recentActivity) { this.recentActivity = recentActivity; }

    // Chart Data
    private List<String> chartLabels = List.of();
    private List<BigDecimal> chartSavings = List.of();
    private List<BigDecimal> chartLoans = List.of();

    public List<String> getChartLabels() { return chartLabels; }
    public void setChartLabels(List<String> chartLabels) { this.chartLabels = chartLabels; }
    public List<BigDecimal> getChartSavings() { return chartSavings; }
    public void setChartSavings(List<BigDecimal> chartSavings) { this.chartSavings = chartSavings; }
    public List<BigDecimal> getChartLoans() { return chartLoans; }
    public void setChartLoans(List<BigDecimal> chartLoans) { this.chartLoans = chartLoans; }

    public String getChartLabelsAsJson() {
        if (chartLabels == null || chartLabels.isEmpty()) return "[]";
        return "['" + String.join("', '", chartLabels) + "']";
    }
    public String getChartSavingsAsJson() {
        return chartSavings != null ? chartSavings.toString() : "[]";
    }
    public String getChartLoansAsJson() {
        return chartLoans != null ? chartLoans.toString() : "[]";
    }

    // Pipeline Data
    private long membersWithSavings;
    private long fullyRepaidLoans;

    public long getMembersWithSavings() { return membersWithSavings; }
    public void setMembersWithSavings(long membersWithSavings) { this.membersWithSavings = membersWithSavings; }
    public long getFullyRepaidLoans() { return fullyRepaidLoans; }
    public void setFullyRepaidLoans(long fullyRepaidLoans) { this.fullyRepaidLoans = fullyRepaidLoans; }
}
