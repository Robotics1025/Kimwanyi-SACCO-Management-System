package org.joel.kimwanyisacco.repository;

import java.util.Optional;

import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.enums.MemberStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface MemberRepository
        extends JpaRepository<Member, Long> {

    Optional<Member> findByMembershipNumber(String membershipNumber);

    Optional<Member> findByNationalId(String nationalId);

    Optional<Member> findByUserAccountId(Long userAccountId);

    boolean existsByMembershipNumber(String membershipNumber);

    boolean existsByNationalId(String nationalId);

    long countByStatus(MemberStatus status);

    long countByMembershipNumberStartingWith(String prefix);

    @Query("select m from Member m join fetch m.userAccount order by m.membershipNumber")
    List<Member> findAllWithUserAccount();

    @Query("select m from Member m join fetch m.userAccount u where "
            + "lower(m.membershipNumber) like lower(concat('%', :keyword, '%')) or "
            + "lower(m.nationalId) like lower(concat('%', :keyword, '%')) or "
            + "lower(u.firstName) like lower(concat('%', :keyword, '%')) or "
            + "lower(u.lastName) like lower(concat('%', :keyword, '%'))")
    List<Member> search(@Param("keyword") String keyword);
}
