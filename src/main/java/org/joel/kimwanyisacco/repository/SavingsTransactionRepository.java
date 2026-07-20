package org.joel.kimwanyisacco.repository;

import java.util.List;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {

    List<SavingsTransaction> findBySavingsAccountId(Long savingsAccountId);
}
