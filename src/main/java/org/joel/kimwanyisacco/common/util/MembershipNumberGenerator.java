package org.joel.kimwanyisacco.common.util;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.springframework.stereotype.Component;

@Component
public class MembershipNumberGenerator {

    private final MemberRepository memberRepository;

    public MembershipNumberGenerator(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public String generate() {
        int year = LocalDate.now().getYear();
        String prefix = "KIM-" + year + "-";

        long sequence = memberRepository.countByMembershipNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", sequence);

        while (memberRepository.existsByMembershipNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }

        return candidate;
    }
}
