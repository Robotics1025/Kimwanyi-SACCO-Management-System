package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.SavingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SavingsHistoryBeanTest {

    @Mock private SavingsService savingsService;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private UserSessionBean userSessionBean;

    @Test
    void initLoadsAccountAndTransactionHistory() {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(1L);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));

        SavingsAccount account = new SavingsAccount();
        account.setId(20L);
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(account));

        SavingsAccountDto accountDto = new SavingsAccountDto();
        accountDto.setId(20L);
        accountDto.setBalance(new BigDecimal("30000.00"));
        when(savingsService.getAccountById(20L)).thenReturn(accountDto);

        SavingsTransactionDto txDto = new SavingsTransactionDto();
        txDto.setTransactionType("DEPOSIT");
        when(savingsService.getTransactionHistory(20L)).thenReturn(List.of(txDto));

        SavingsHistoryBean bean = new SavingsHistoryBean(savingsService, memberRepository, savingsAccountRepository, userSessionBean);
        bean.init();

        assertEquals(new BigDecimal("30000.00"), bean.getAccount().getBalance());
        assertEquals(1, bean.getTransactions().size());
        assertEquals("DEPOSIT", bean.getTransactions().get(0).getTransactionType());
    }
}
