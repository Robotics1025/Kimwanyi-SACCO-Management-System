package org.joel.kimwanyisacco.service;

import java.util.Comparator;
import java.util.List;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.SavingsAccountConverter;
import org.joel.kimwanyisacco.common.util.converter.SavingsTransactionConverter;
import org.joel.kimwanyisacco.dto.DepositForm;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.dto.WithdrawalForm;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.SavingsTransactionRepository;
import org.springframework.stereotype.Service;

@Service
public class SavingsServiceImpl implements SavingsService {

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final SavingsAccountConverter savingsAccountConverter;
    private final SavingsTransactionConverter savingsTransactionConverter;

    public SavingsServiceImpl(
            SavingsAccountRepository savingsAccountRepository,
            SavingsTransactionRepository savingsTransactionRepository,
            SavingsAccountConverter savingsAccountConverter,
            SavingsTransactionConverter savingsTransactionConverter
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsTransactionRepository = savingsTransactionRepository;
        this.savingsAccountConverter = savingsAccountConverter;
        this.savingsTransactionConverter = savingsTransactionConverter;
    }

    @Override
    public SavingsTransactionDto deposit(DepositForm form) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public SavingsTransactionDto withdraw(WithdrawalForm form) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public SavingsAccountDto getAccountById(Long id) {
        SavingsAccount account = savingsAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found with ID: " + id));
        return savingsAccountConverter.toDto(account);
    }

    @Override
    public List<SavingsTransactionDto> getTransactionHistory(Long savingsAccountId) {
        return savingsTransactionRepository.findBySavingsAccountId(savingsAccountId).stream()
                .sorted(Comparator.comparing(org.joel.kimwanyisacco.model.SavingsTransaction::getCreatedAt).reversed())
                .map(savingsTransactionConverter::toDto)
                .toList();
    }
}
