package org.joel.kimwanyisacco.repository;

import java.math.BigDecimal;
import java.util.Optional;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, Long> {

    Optional<SavingsAccount> findByAccountNumber(String accountNumber);

    Optional<SavingsAccount> findByMemberId(Long memberId);

    boolean existsByAccountNumber(String accountNumber);
 
    long countByAccountNumberStartingWith(String prefix);

    @Query("select coalesce(sum(s.balance), 0) from SavingsAccount s")
    BigDecimal sumAllBalances();
}

