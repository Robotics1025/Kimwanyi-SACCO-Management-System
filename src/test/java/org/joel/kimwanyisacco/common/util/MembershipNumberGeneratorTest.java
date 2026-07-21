package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MembershipNumberGeneratorTest {

    @Mock
    private MemberRepository memberRepository;

    @Test
    void generatesFirstMembershipNumberOfTheYear() {
        String prefix = "KIM-" + LocalDate.now().getYear() + "-";
        when(memberRepository.countByMembershipNumberStartingWith(prefix)).thenReturn(0L);
        when(memberRepository.existsByMembershipNumber(prefix + "0001")).thenReturn(false);

        MembershipNumberGenerator generator = new MembershipNumberGenerator(memberRepository);

        assertEquals(prefix + "0001", generator.generate());
    }

    @Test
    void continuesSequenceFromExistingCountForTheYear() {
        String prefix = "KIM-" + LocalDate.now().getYear() + "-";
        when(memberRepository.countByMembershipNumberStartingWith(prefix)).thenReturn(41L);
        when(memberRepository.existsByMembershipNumber(prefix + "0042")).thenReturn(false);

        MembershipNumberGenerator generator = new MembershipNumberGenerator(memberRepository);

        assertEquals(prefix + "0042", generator.generate());
    }

    @Test
    void incrementsPastCollisionsUntilAnUnusedNumberIsFound() {
        String prefix = "KIM-" + LocalDate.now().getYear() + "-";
        when(memberRepository.countByMembershipNumberStartingWith(prefix)).thenReturn(0L);
        when(memberRepository.existsByMembershipNumber(prefix + "0001")).thenReturn(true);
        when(memberRepository.existsByMembershipNumber(prefix + "0002")).thenReturn(false);

        MembershipNumberGenerator generator = new MembershipNumberGenerator(memberRepository);

        assertEquals(prefix + "0002", generator.generate());
    }
}
