package org.joel.kimwanyisacco.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.joel.kimwanyisacco.dto.LoggedInUserDto;
import org.joel.kimwanyisacco.model.Loan;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.model.enums.LoanStatus;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.joel.kimwanyisacco.service.LoanService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MemberLoansBeanTest {

    @Mock private LoanService loanService;
    @Mock private MemberRepository memberRepository;
    @Mock private UserSessionBean userSessionBean;

    private Loan loanWith(LoanStatus status, LocalDate applicationDate) {
        Loan loan = new Loan();
        loan.setStatus(status);
        loan.setPrincipal(new BigDecimal("10000.00"));
        loan.setApplicationDate(applicationDate);
        return loan;
    }

    @Test
    void initLoadsMemberLoansNewestFirst() {
        LoggedInUserDto dto = new LoggedInUserDto();
        dto.setId(1L);
        when(userSessionBean.getLoggedInUser()).thenReturn(dto);

        Member member = new Member();
        member.setId(10L);
        when(memberRepository.findByUserAccountId(1L)).thenReturn(Optional.of(member));

        Loan older = loanWith(LoanStatus.FULLY_REPAID, LocalDate.of(2026, 1, 1));
        Loan newer = loanWith(LoanStatus.PENDING, LocalDate.of(2026, 3, 1));
        when(loanService.getLoansByMember(10L)).thenReturn(List.of(older, newer));

        MemberLoansBean bean = new MemberLoansBean(loanService, memberRepository, userSessionBean);
        bean.init();

        assertEquals(2, bean.getLoans().size());
        assertEquals(LoanStatus.PENDING, bean.getLoans().get(0).getStatus());
        assertEquals(LoanStatus.FULLY_REPAID, bean.getLoans().get(1).getStatus());
    }

    @Test
    void badgeClassMapsStatusToBadgeColor() {
        MemberLoansBean bean = new MemberLoansBean(loanService, memberRepository, userSessionBean);

        assertEquals("badge-yellow", bean.badgeClass(loanWith(LoanStatus.PENDING, LocalDate.now())));
        assertEquals("badge-blue", bean.badgeClass(loanWith(LoanStatus.ACTIVE, LocalDate.now())));
        assertEquals("badge-green", bean.badgeClass(loanWith(LoanStatus.FULLY_REPAID, LocalDate.now())));
        assertEquals("badge-red", bean.badgeClass(loanWith(LoanStatus.REJECTED, LocalDate.now())));
        assertEquals("badge-red", bean.badgeClass(loanWith(LoanStatus.OVERDUE, LocalDate.now())));
        assertEquals("badge-gray", bean.badgeClass(loanWith(LoanStatus.APPROVED, LocalDate.now())));
    }
}
