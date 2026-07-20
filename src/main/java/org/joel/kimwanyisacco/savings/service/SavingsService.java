package org.joel.kimwanyisacco.savings.service;

import java.util.List;
import org.joel.kimwanyisacco.savings.dto.DepositForm;
import org.joel.kimwanyisacco.savings.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.savings.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.savings.dto.WithdrawalForm;

public interface SavingsService {

    SavingsTransactionDto deposit(DepositForm form);

    SavingsTransactionDto withdraw(WithdrawalForm form);

    SavingsAccountDto getAccountById(Long id);

    List<SavingsTransactionDto> getTransactionHistory(Long savingsAccountId);
}
