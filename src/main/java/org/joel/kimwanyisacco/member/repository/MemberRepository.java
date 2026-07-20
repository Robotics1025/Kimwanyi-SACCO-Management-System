package org.joel.kimwanyisacco.member.repository;

import java.util.Optional;
import org.joel.kimwanyisacco.member.model.Member;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMembershipNumber(String membershipNumber);

    Optional<Member> findByNationalId(String nationalId);

    boolean existsByNationalId(String nationalId);
}
