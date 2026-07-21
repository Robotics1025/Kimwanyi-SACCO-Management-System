package org.joel.kimwanyisacco.service;

import org.joel.kimwanyisacco.common.util.converter.MemberConverter;
import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberServiceImpl implements MemberService {

    private final UserAccountRepository userAccountRepository;
    private final MemberRepository memberRepository;
    private final MemberConverter memberConverter;
    private final PasswordEncoder passwordEncoder;

    public MemberServiceImpl(
            UserAccountRepository userAccountRepository,
            MemberRepository memberRepository,
            MemberConverter memberConverter,
            PasswordEncoder passwordEncoder
    ) {
        this.userAccountRepository = userAccountRepository;
        this.memberRepository = memberRepository;
        this.memberConverter = memberConverter;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public Member registerMember(MemberRegistrationForm form) {

        validateForm(form);

        String username = form.getUsername().trim();
        String email = form.getEmail().trim().toLowerCase();
        String nationalId = form.getNationalId().trim();

        if (userAccountRepository.existsByUsername(username)) {
            throw new IllegalArgumentException(
                    "Username already exists"
            );
        }

        if (userAccountRepository.existsByEmail(email)) {
            throw new IllegalArgumentException(
                    "Email already exists"
            );
        }

        if (memberRepository.existsByNationalId(nationalId)) {
            throw new IllegalArgumentException(
                    "National ID already exists"
            );
        }

        String hashedPassword =
                passwordEncoder.encode(form.getPassword());

        UserAccount userAccount =
                memberConverter.toUserAccount(
                        form,
                        hashedPassword
                );

        UserAccount savedAccount =
                userAccountRepository.save(userAccount);

        String membershipNumber =
                generateMembershipNumber();

        Member member =
                memberConverter.toMember(
                        form,
                        savedAccount,
                        membershipNumber
                );

        return memberRepository.save(member);
    }

    private void validateForm(MemberRegistrationForm form) {

        if (form == null) {
            throw new IllegalArgumentException(
                    "Registration form is required"
            );
        }

        if (form.getUsername() == null ||
                form.getUsername().isBlank()) {
            throw new IllegalArgumentException(
                    "Username is required"
            );
        }

        if (form.getPassword() == null ||
                form.getPassword().isBlank()) {
            throw new IllegalArgumentException(
                    "Password is required"
            );
        }

        if (form.getFirstName() == null ||
                form.getFirstName().isBlank()) {
            throw new IllegalArgumentException(
                    "First name is required"
            );
        }

        if (form.getLastName() == null ||
                form.getLastName().isBlank()) {
            throw new IllegalArgumentException(
                    "Last name is required"
            );
        }

        if (form.getEmail() == null ||
                form.getEmail().isBlank()) {
            throw new IllegalArgumentException(
                    "Email is required"
            );
        }

        if (form.getNationalId() == null ||
                form.getNationalId().isBlank()) {
            throw new IllegalArgumentException(
                    "National ID is required"
            );
        }
    }

    private String generateMembershipNumber() {
        return "KIM-" + System.currentTimeMillis();
    }
}