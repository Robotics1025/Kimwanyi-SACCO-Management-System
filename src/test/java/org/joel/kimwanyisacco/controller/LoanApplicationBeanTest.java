package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import jakarta.faces.context.FacesContext;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.repository.SavingsAccountRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoanApplicationBeanTest {

    @Mock private LoanService loanService;
    @Mock private MemberRepository memberRepository;
    @Mock private SavingsAccountRepository savingsAccountRepository;
    @Mock private UserSessionBean userSessionBean;

    private LoanApplicationBean bean() {
        return new LoanApplicationBean(loanService, memberRepository, savingsAccountRepository, userSessionBean);
    }

    private void loggedInAs(long userAccountId) {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(userAccountId);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);
    }

    @Test
    void initResolvesMemberAndComputesMaxEligibleAmount() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));

        SavingsAccount account = new SavingsAccount();
        account.setBalance(new BigDecimal("50000.00"));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(account));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of());

        LoanApplicationBean bean = bean();
        bean.init();

        assertEquals(new BigDecimal("150000.00"), bean.getMaxEligibleAmount());
        assertFalse(bean.isHasActiveLoan());
    }

    @Test
    void initDetectsExistingPendingLoan() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(new SavingsAccount()));

        Loan pendingLoan = new Loan();
        pendingLoan.setStatus(LoanStatus.PENDING);
        when(loanService.getLoansByMember(10L)).thenReturn(List.of(pendingLoan));

        LoanApplicationBean bean = bean();
        bean.init();

        assertTrue(bean.isHasActiveLoan());
    }

    @Test
    void applyRedirectsToMyLoansOnSuccess() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(new SavingsAccount()));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of());

        LoanApplicationBean bean = bean();
        bean.init();
        bean.setPrincipalAmount(new BigDecimal("10000.00"));
        bean.setTermMonths(6);

        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = bean.apply();

            assertEquals("/loans/applications?faces-redirect=true", outcome);
        }
    }

    @Test
    void applyReturnsNullAndAddsErrorMessageWhenServiceRejects() {
        loggedInAs(1L);
        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));
        when(savingsAccountRepository.findByMemberId(10L)).thenReturn(Optional.of(new SavingsAccount()));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of());
        when(loanService.applyLoan(org.mockito.ArgumentMatchers.any()))
                .thenThrow(new RuntimeException("Requested loan amount exceeds the maximum allowed limit"));

        LoanApplicationBean bean = bean();
        bean.init();
        bean.setPrincipalAmount(new BigDecimal("999999.00"));
        bean.setTermMonths(6);

        FacesContext facesContext = mock(FacesContext.class);
        try (MockedStatic<FacesContext> mockedStatic = mockStatic(FacesContext.class)) {
            mockedStatic.when(FacesContext::getCurrentInstance).thenReturn(facesContext);

            String outcome = bean.apply();

            assertNull(outcome);
        }
    }
}
