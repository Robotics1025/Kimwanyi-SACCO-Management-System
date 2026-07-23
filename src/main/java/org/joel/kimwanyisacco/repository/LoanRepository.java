package org.joel.kimwanyisacco.repository;

import java.math.BigDecimal;

import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    long countByStatus(LoanStatus status);

    java.util.List<Loan> findByMemberId(Long memberId);

    @Query("select l from Loan l join fetch l.member m join fetch m.userAccount where l.status = :status")
    java.util.List<Loan> findByStatus(@Param("status") LoanStatus status);

    @Query("select coalesce(sum(l.outstandingBalance), 0) from Loan l where l.status in ('ACTIVE','OVERDUE')")
    BigDecimal sumOutstandingBalance();

    java.util.List<Loan> findByStatusAndDueDateBefore(LoanStatus status, LocalDate date);
}
