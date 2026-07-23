package org.joel.kimwanyisacco.repository;

import java.util.List;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;

public interface SavingsTransactionRepository extends JpaRepository<SavingsTransaction, Long> {

    List<SavingsTransaction> findBySavingsAccountId(Long savingsAccountId);
    boolean existsByReference(String reference);

    @Query("select t from SavingsTransaction t where t.savingsAccount.id = :accountId "
            + "and t.createdAt between :fromDate and :toDate order by t.createdAt, t.id")
    List<SavingsTransaction> findStatementTransactions(@Param("accountId") Long accountId,
            @Param("fromDate") LocalDateTime fromDate, @Param("toDate") LocalDateTime toDate);

    Optional<SavingsTransaction> findTopBySavingsAccountIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(
            Long savingsAccountId, LocalDateTime before);
}
