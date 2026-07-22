package org.joel.kimwanyisacco.repository;

import java.util.List;
import java.util.Optional;

import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsername(String username);
    Optional<UserAccount> findByEmail(String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<UserAccount> findByRole(Role role);

    @Query("select u from UserAccount u where lower(u.username) like lower(concat('%', :kw, '%')) or lower(u.email) like lower(concat('%', :kw, '%')) or lower(u.firstName) like lower(concat('%', :kw, '%')) or lower(u.lastName) like lower(concat('%', :kw, '%'))")
    List<UserAccount> search(@Param("kw") String keyword);
}

