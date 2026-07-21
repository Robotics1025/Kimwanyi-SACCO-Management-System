package org.joel.kimwanyisacco.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavingsAccountNumberGeneratorTest {

    @Mock
    private SavingsAccountRepository savingsAccountRepository;

    @Test
    void generatesFirstAccountNumberOfTheYear() {
        String prefix = "SAV-" + LocalDate.now().getYear() + "-";
        when(savingsAccountRepository.countByAccountNumberStartingWith(prefix)).thenReturn(0L);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0001")).thenReturn(false);

        SavingsAccountNumberGenerator generator = new SavingsAccountNumberGenerator(savingsAccountRepository);

        assertEquals(prefix + "0001", generator.generate());
    }

    @Test
    void continuesSequenceFromExistingCountForTheYear() {
        String prefix = "SAV-" + LocalDate.now().getYear() + "-";
        when(savingsAccountRepository.countByAccountNumberStartingWith(prefix)).thenReturn(41L);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0042")).thenReturn(false);

        SavingsAccountNumberGenerator generator = new SavingsAccountNumberGenerator(savingsAccountRepository);

        assertEquals(prefix + "0042", generator.generate());
    }

    @Test
    void incrementsPastCollisionsUntilAnUnusedNumberIsFound() {
        String prefix = "SAV-" + LocalDate.now().getYear() + "-";
        when(savingsAccountRepository.countByAccountNumberStartingWith(prefix)).thenReturn(0L);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0001")).thenReturn(true);
        when(savingsAccountRepository.existsByAccountNumber(prefix + "0002")).thenReturn(false);

        SavingsAccountNumberGenerator generator = new SavingsAccountNumberGenerator(savingsAccountRepository);

        assertEquals(prefix + "0002", generator.generate());
    }
}
