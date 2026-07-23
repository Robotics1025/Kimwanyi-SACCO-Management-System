package org.joel.kimwanyisacco.service;

import java.time.LocalDate;
import org.joel.kimwanyisacco.dto.AccountStatementDto;

public interface StatementService {
    AccountStatementDto generateForMember(Long userAccountId, LocalDate fromDate, LocalDate toDate);
}
