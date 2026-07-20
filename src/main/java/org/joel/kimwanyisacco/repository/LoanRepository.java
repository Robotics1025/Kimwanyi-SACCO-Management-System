package org.joel.kimwanyisacco.repository;

import java.util.List;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.LoanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoanRepository extends JpaRepository<Loan, Long> {

    List<Loan> findByMemberId(Long memberId);

    List<Loan> findByStatus(LoanStatus status);
}
