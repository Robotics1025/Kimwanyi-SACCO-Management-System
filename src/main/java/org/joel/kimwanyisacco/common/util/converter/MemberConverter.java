package org.joel.kimwanyisacco.common.util.converter;


import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.MemberStatus;
import org.joel.kimwanyisacco.model.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class MemberConverter {

    public MemberDto toDto(Member member) {
        MemberDto dto = new MemberDto();
        dto.setId(member.getId());
        dto.setMembershipNumber(member.getMembershipNumber());
        dto.setNationalId(member.getNationalId());
        dto.setPhoneNumber(member.getPhoneNumber());
        dto.setJoinedAt(member.getJoinedAt());
        dto.setStatus(member.getStatus() != null ? member.getStatus().name() : null);
        if (member.getUserAccount() != null) {
            dto.setFirstName(member.getUserAccount().getFirstName());
            dto.setLastName(member.getUserAccount().getLastName());
            dto.setEmail(member.getUserAccount().getEmail());
        }
        return dto;
    }

    public UserAccount toUserAccount(
            MemberRegistrationForm form,
            String hashedPassword
    ) {
        UserAccount account = new UserAccount();

        account.setUsername(form.getUsername().trim());
        account.setPasswordHash(hashedPassword);
        account.setFirstName(form.getFirstName().trim());
        account.setLastName(form.getLastName().trim());
        account.setEmail(form.getEmail().trim().toLowerCase());
        account.setRole(Role.MEMBER);
        // A self-registered member cannot authenticate until an administrator
        // has verified and approved the application.
        account.setEnabled(false);

        return account;
    }

    public Member toMember(
            MemberRegistrationForm form,
            UserAccount savedAccount,
            String membershipNumber
    ) {
        Member member = new Member();

        member.setUserAccount(savedAccount);
        member.setMembershipNumber(membershipNumber);
        member.setNationalId(form.getNationalId().trim());
        member.setStatus(MemberStatus.PENDING);

        return member;
    }
}
