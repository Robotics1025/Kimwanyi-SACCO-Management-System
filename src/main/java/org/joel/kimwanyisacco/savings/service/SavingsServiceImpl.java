package org.joel.kimwanyisacco.savings.service;

import java.util.List;
import org.joel.kimwanyisacco.savings.dto.DepositForm;
import org.joel.kimwanyisacco.savings.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.savings.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.savings.dto.WithdrawalForm;
import org.springframework.stereotype.Service;

@Service
public class SavingsServiceImpl implements SavingsService {

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
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<SavingsTransactionDto> getTransactionHistory(Long savingsAccountId) {
        throw new UnsupportedOperationException("not implemented");
    }
}
