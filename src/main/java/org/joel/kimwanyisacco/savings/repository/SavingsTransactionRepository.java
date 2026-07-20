package org.joel.kimwanyisacco.savings.repository;

import java.util.List;
import org.joel.kimwanyisacco.savings.model.SavingsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {

    List<SavingsTransaction> findBySavingsAccountId(Long savingsAccountId);
}
