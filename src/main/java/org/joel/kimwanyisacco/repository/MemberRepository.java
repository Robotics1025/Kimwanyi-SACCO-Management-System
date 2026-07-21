package org.joel.kimwanyisacco.repository;

import java.util.Optional;

import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository
        extends JpaRepository<Member, Long> {

    Optional<Member> findByMembershipNumber(String membershipNumber);

    Optional<Member> findByNationalId(String nationalId);

    Optional<Member> findByUserAccountId(Long userAccountId);

    boolean existsByMembershipNumber(String membershipNumber);

    boolean existsByNationalId(String nationalId);

    long countByStatus(MemberStatus status);
}