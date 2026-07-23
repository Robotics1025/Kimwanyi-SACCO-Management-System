package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.time.LocalDate;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.AccountStatementDto;
import org.joel.kimwanyisacco.service.StatementService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("statementBean")
@RequestScope
public class StatementBean {
    private final StatementService statementService;
    private final UserSessionBean session;
    private LocalDate fromDate;
    private LocalDate toDate;
    private AccountStatementDto statement;

    public StatementBean(StatementService statementService, UserSessionBean session) {
        this.statementService = statementService;
        this.session = session;
    }

    @PostConstruct
    public void init() {
        toDate = LocalDate.now();
        fromDate = toDate.minusMonths(1);
        generate();
    }

    public void generate() {
        try {
            statement = statementService.generateForMember(session.getLoggedInUser().getId(), fromDate, toDate);
        } catch (RuntimeException exception) {
            FacesMessageUtil.addErrorMessage(exception.getMessage());
        }
    }

    public LocalDate getFromDate() { return fromDate; }
    public void setFromDate(LocalDate fromDate) { this.fromDate = fromDate; }
    public LocalDate getToDate() { return toDate; }
    public void setToDate(LocalDate toDate) { this.toDate = toDate; }
    public AccountStatementDto getStatement() { return statement; }
}
