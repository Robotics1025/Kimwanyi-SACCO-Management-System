package org.joel.kimwanyisacco.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.joel.kimwanyisacco.common.exception.ResourceNotFoundException;
import org.joel.kimwanyisacco.dto.AccountStatementDto;
import org.joel.kimwanyisacco.dto.StatementEntryDto;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.joel.kimwanyisacco.model.enums.TransactionType;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.repository.SavingsTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StatementServiceImpl implements StatementService {
    private final MemberRepository memberRepository;
    private final SavingsAccountRepository accountRepository;
    private final SavingsTransactionRepository transactionRepository;

    public StatementServiceImpl(MemberRepository memberRepository, SavingsAccountRepository accountRepository,
                                SavingsTransactionRepository transactionRepository) {
        this.memberRepository = memberRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountStatementDto generateForMember(Long userAccountId, LocalDate fromDate, LocalDate toDate) {
        if (fromDate == null || toDate == null || fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("A valid statement date range is required");
        }
        Member member = memberRepository.findByUserAccountId(userAccountId)
                .orElseThrow(() -> new ResourceNotFoundException("Member profile not found"));
        SavingsAccount account = accountRepository.findByMemberId(member.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Savings account not found"));
        List<SavingsTransaction> transactions = transactionRepository
                .findStatementTransactions(account.getId(), fromDate.atStartOfDay(), toDate.atTime(LocalTime.MAX));

        AccountStatementDto statement = new AccountStatementDto();
        statement.setMemberName(member.getUserAccount().getFirstName() + " " + member.getUserAccount().getLastName());
        statement.setMembershipNumber(member.getMembershipNumber());
        statement.setAccountNumber(account.getAccountNumber());
        statement.setFromDate(fromDate);
        statement.setToDate(toDate);
        BigDecimal opening = transactionRepository
                .findTopBySavingsAccountIdAndCreatedAtBeforeOrderByCreatedAtDescIdDesc(account.getId(), fromDate.atStartOfDay())
                .map(SavingsTransaction::getBalanceAfter)
                .orElse(BigDecimal.ZERO);
        statement.setOpeningBalance(opening);
        statement.setClosingBalance(transactions.isEmpty() ? opening
                : transactions.get(transactions.size() - 1).getBalanceAfter());
        statement.setEntries(transactions.stream().map(this::toEntry).toList());
        return statement;
    }

    private StatementEntryDto toEntry(SavingsTransaction transaction) {
        StatementEntryDto entry = new StatementEntryDto();
        entry.setDate(transaction.getCreatedAt());
        entry.setReference(transaction.getReference());
        entry.setDescription(transaction.getDescription());
        entry.setType(transaction.getType().name());
        boolean debit = transaction.getType() == TransactionType.WITHDRAW
                || transaction.getType() == TransactionType.TRANSFER_OUT;
        if (debit) entry.setDebit(transaction.getAmount()); else entry.setCredit(transaction.getAmount());
        entry.setBalance(transaction.getBalanceAfter());
        return entry;
    }
}
