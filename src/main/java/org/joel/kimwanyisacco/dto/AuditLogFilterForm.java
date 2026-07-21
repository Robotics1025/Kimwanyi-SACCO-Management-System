package org.joel.kimwanyisacco.dto;

import java.time.LocalDate;

import org.joel.kimwanyisacco.model.enums.AuditAction;

public class AuditLogFilterForm {

    private AuditAction action;
    private String username;
    private LocalDate dateFrom;
    private LocalDate dateTo;

    public AuditAction getAction() { return action; }
    public void setAction(AuditAction action) { this.action = action; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDate getDateFrom() { return dateFrom; }
    public void setDateFrom(LocalDate dateFrom) { this.dateFrom = dateFrom; }

    public LocalDate getDateTo() { return dateTo; }
    public void setDateTo(LocalDate dateTo) { this.dateTo = dateTo; }
}
