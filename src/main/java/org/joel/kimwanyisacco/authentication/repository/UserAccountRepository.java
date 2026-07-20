package org.joel.kimwanyisacco.authentication.repository;

import java.util.Optional;
import org.joel.kimwanyisacco.authentication.model.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);

    boolean existsByUsername(String username);
}
