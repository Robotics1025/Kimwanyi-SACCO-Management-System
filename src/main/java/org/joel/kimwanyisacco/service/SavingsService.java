package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.dto.DepositForm;
import org.joel.kimwanyisacco.dto.InternalTransferForm;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.dto.WithdrawalForm;
import java.time.YearMonth;

public interface SavingsService {

    SavingsTransactionDto deposit(DepositForm form);

    SavingsTransactionDto withdraw(WithdrawalForm form);

    SavingsAccountDto getAccountById(Long id);

    List<SavingsTransactionDto> getTransactionHistory(Long savingsAccountId);

    void transfer(InternalTransferForm form);
    int applyMonthlyInterest(YearMonth month);
    void postPreviousMonthInterest();
}
