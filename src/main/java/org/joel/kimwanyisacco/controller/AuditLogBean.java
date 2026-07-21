package org.joel.kimwanyisacco.controller;

import java.util.List;

import org.joel.kimwanyisacco.dto.AuditLogDto;
import org.joel.kimwanyisacco.dto.AuditLogFilterForm;
import org.joel.kimwanyisacco.service.AuditLogService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.annotation.PostConstruct;

@Component("auditLogBean")
@RequestScope
public class AuditLogBean {

    private final AuditLogService auditLogService;
    
    private AuditLogFilterForm filter = new AuditLogFilterForm();
    private List<AuditLogDto> results;

    public AuditLogBean(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @PostConstruct
    public void init() {
        search();
    }

    public void search() {
        results = auditLogService.search(filter);
    }

    public AuditLogFilterForm getFilter() { return filter; }
    public void setFilter(AuditLogFilterForm filter) { this.filter = filter; }

    public List<AuditLogDto> getResults() { return results; }
    public void setResults(List<AuditLogDto> results) { this.results = results; }
}
