package org.joel.kimwanyisacco.repository;

import java.math.BigDecimal;

import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByStatus(LoanStatus status);

    java.util.List<Loan> findByMemberId(Long memberId);

    java.util.List<Loan> findByStatus(LoanStatus status);

    @Query("select coalesce(sum(l.outstandingBalance), 0) from Loan l where l.status in ('ACTIVE','OVERDUE')")
    BigDecimal sumOutstandingBalance();
}
