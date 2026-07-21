package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.faces.context.FacesContext;
import java.math.BigDecimal;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.SavingsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InternalTransferBeanTest {

    @Mock private SavingsService savingsService;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private UserSessionBean userSessionBean;

    @Test
    void initResolvesMemberAndBalance() {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(1L);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));

        SavingsAccount account = new SavingsAccount();
        account.setId(20L);
        account.setBalance(new BigDecimal("50000.00"));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(account));

        InternalTransferBean bean = new InternalTransferBean(savingsService, memberRepository, savingsAccountRepository, userSessionBean);
        bean.init();

        assertEquals(new BigDecimal("50000.00"), bean.getMyBalance());
        assertEquals(new BigDecimal("30000.00"), bean.getMaxTransferable()); // 50000 - 20000 min balance
    }
}
