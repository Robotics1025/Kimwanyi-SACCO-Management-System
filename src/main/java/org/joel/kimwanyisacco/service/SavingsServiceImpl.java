package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.common.util.converter.SavingsAccountConverter;
import org.joel.kimwanyisacco.common.util.converter.SavingsTransactionConverter;
import org.joel.kimwanyisacco.dto.DepositForm;
import org.joel.kimwanyisacco.dto.InternalTransferForm;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.dto.WithdrawalForm;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.joel.kimwanyisacco.model.enums.NotificationType;
import org.joel.kimwanyisacco.model.enums.TransactionType;
import org.joel.kimwanyisacco.policy.WithdrawalPolicy;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.SavingsTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SavingsServiceImpl implements SavingsService {

    private static final BigDecimal MINIMUM_BALANCE = new BigDecimal("20000.00");

    private final SavingsAccountRepository savingsAccountRepository;
    private final SavingsTransactionRepository savingsTransactionRepository;
    private final SavingsAccountConverter savingsAccountConverter;
    private final SavingsTransactionConverter savingsTransactionConverter;
    private final WithdrawalPolicy withdrawalPolicy;
    private final NotificationService notificationService;

    public SavingsServiceImpl(
            SavingsAccountRepository savingsAccountRepository,
            SavingsTransactionRepository savingsTransactionRepository,
            SavingsAccountConverter savingsAccountConverter,
            SavingsTransactionConverter savingsTransactionConverter,
            WithdrawalPolicy withdrawalPolicy,
            NotificationService notificationService
    ) {
        this.savingsAccountRepository = savingsAccountRepository;
        this.savingsTransactionRepository = savingsTransactionRepository;
        this.savingsAccountConverter = savingsAccountConverter;
        this.savingsTransactionConverter = savingsTransactionConverter;
        this.withdrawalPolicy = withdrawalPolicy;
        this.notificationService = notificationService;
    }

    @Override
    @Transactional
    public SavingsTransactionDto deposit(DepositForm form) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Deposit amount must be greater than zero");
        }

        SavingsAccount account = savingsAccountRepository.findById(form.getSavingsAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        BigDecimal balanceBefore = account.getBalance();
        BigDecimal balanceAfter = balanceBefore.add(form.getAmount());

        account.setBalance(balanceAfter);
        savingsAccountRepository.save(account);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setSavingsAccount(account);
        tx.setReference("DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setType(TransactionType.DEPOSIT);
        tx.setAmount(form.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription("Member deposit");
        tx.setCreatedAt(LocalDateTime.now());
        savingsTransactionRepository.save(tx);

        notificationService.notify(account.getMember().getUserAccount(), NotificationType.DEPOSIT,
                "Deposit Received", "UGX " + form.getAmount() + " deposited. New balance: UGX " + balanceAfter + ".");

        return savingsTransactionConverter.toDto(tx);
    }

    @Override
    @Transactional
    public SavingsTransactionDto withdraw(WithdrawalForm form) {
        SavingsAccount account = savingsAccountRepository.findById(form.getSavingsAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));

        BigDecimal balanceBefore = account.getBalance();

        if (!withdrawalPolicy.isWithdrawalAllowed(balanceBefore, form.getAmount())) {
            throw new IllegalArgumentException(
                    "Withdrawal not allowed: amount must be positive and leave at least UGX 20,000 in the account.");
        }

        BigDecimal balanceAfter = balanceBefore.subtract(form.getAmount());

        account.setBalance(balanceAfter);
        savingsAccountRepository.save(account);

        SavingsTransaction tx = new SavingsTransaction();
        tx.setSavingsAccount(account);
        tx.setReference("WTH-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        tx.setType(TransactionType.WITHDRAW);
        tx.setAmount(form.getAmount());
        tx.setBalanceBefore(balanceBefore);
        tx.setBalanceAfter(balanceAfter);
        tx.setDescription(form.getDescription() != null && !form.getDescription().isBlank()
                ? form.getDescription().trim() : "Member withdrawal");
        tx.setCreatedAt(LocalDateTime.now());
        savingsTransactionRepository.save(tx);

        notificationService.notify(account.getMember().getUserAccount(), NotificationType.WITHDRAWAL,
                "Withdrawal Processed", "UGX " + form.getAmount() + " withdrawn. New balance: UGX " + balanceAfter + ".");

        return savingsTransactionConverter.toDto(tx);
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

    @Override
    @Transactional
    public void transfer(InternalTransferForm form) {
        if (form.getAmount() == null || form.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transfer amount must be greater than zero");
        }

        SavingsAccount fromAccount = savingsAccountRepository.findById(form.getFromAccountId())
                .orElseThrow(() -> new ResourceNotFoundException("Your savings account was not found"));

        SavingsAccount toAccount = savingsAccountRepository.findByAccountNumber(form.getToAccountNumber().trim().toUpperCase())
                .orElseThrow(() -> new IllegalArgumentException("Recipient account number not found: " + form.getToAccountNumber()));

        if (fromAccount.getId().equals(toAccount.getId())) {
            throw new IllegalArgumentException("You cannot transfer to your own account");
        }

        BigDecimal transferAmount = form.getAmount();
        BigDecimal fromBalanceAfter = fromAccount.getBalance().subtract(transferAmount);

        if (fromBalanceAfter.compareTo(MINIMUM_BALANCE) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance. Transfer would leave your account below the minimum balance of UGX 20,000. " +
                    "Available for transfer: UGX " + fromAccount.getBalance().subtract(MINIMUM_BALANCE).toPlainString()
            );
        }

        BigDecimal toBalanceAfter = toAccount.getBalance().add(transferAmount);
        String refPair = "TXF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String description = form.getDescription() != null && !form.getDescription().isBlank()
                ? form.getDescription().trim()
                : "Internal transfer";
        LocalDateTime now = LocalDateTime.now();

        // Debit sender
        fromAccount.setBalance(fromBalanceAfter);
        savingsAccountRepository.save(fromAccount);

        SavingsTransaction outTx = new SavingsTransaction();
        outTx.setSavingsAccount(fromAccount);
        outTx.setReference(refPair + "-OUT");
        outTx.setType(TransactionType.TRANSFER_OUT);
        outTx.setAmount(transferAmount);
        outTx.setBalanceBefore(fromAccount.getBalance().add(transferAmount));
        outTx.setBalanceAfter(fromBalanceAfter);
        outTx.setDescription(description + " → " + toAccount.getAccountNumber());
        savingsTransactionRepository.save(outTx);

        // Credit receiver
        toAccount.setBalance(toBalanceAfter);
        savingsAccountRepository.save(toAccount);

        SavingsTransaction inTx = new SavingsTransaction();
        inTx.setSavingsAccount(toAccount);
        inTx.setReference(refPair + "-IN");
        inTx.setType(TransactionType.TRANSFER_IN);
        inTx.setAmount(transferAmount);
        inTx.setBalanceBefore(toAccount.getBalance().subtract(transferAmount));
        inTx.setBalanceAfter(toBalanceAfter);
        inTx.setDescription(description + " ← " + fromAccount.getAccountNumber());
        savingsTransactionRepository.save(inTx);
    }
}
