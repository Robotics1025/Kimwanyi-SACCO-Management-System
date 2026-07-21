package org.joel.kimwanyisacco.common.util.converter;


import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.MemberStatus;
import org.joel.kimwanyisacco.model.enums.Role;
import org.springframework.stereotype.Component;

@Component
public class MemberConverter {

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
        account.setEnabled(true);

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
        member.setStatus(MemberStatus.ACTIVE);

        return member;
    }
}