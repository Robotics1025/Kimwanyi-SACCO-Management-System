package org.joel.kimwanyisacco.common.util;

import java.time.LocalDate;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.springframework.stereotype.Component;

@Component
public class SavingsAccountNumberGenerator {

    private final SavingsAccountRepository savingsAccountRepository;

    public SavingsAccountNumberGenerator(SavingsAccountRepository savingsAccountRepository) {
        this.savingsAccountRepository = savingsAccountRepository;
    }

    public String generate() {
        int year = LocalDate.now().getYear();
        String prefix = "SAV-" + year + "-";

        long sequence = savingsAccountRepository.countByAccountNumberStartingWith(prefix) + 1;
        String candidate = prefix + String.format("%04d", sequence);

        while (savingsAccountRepository.existsByAccountNumber(candidate)) {
            sequence++;
            candidate = prefix + String.format("%04d", sequence);
        }

        return candidate;
    }
}
