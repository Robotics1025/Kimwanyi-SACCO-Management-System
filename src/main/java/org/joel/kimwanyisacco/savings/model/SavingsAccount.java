package org.joel.kimwanyisacco.savings.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.joel.kimwanyisacco.common.model.BaseEntity;
import org.joel.kimwanyisacco.member.model.Member;

@Entity
@Table(name = "savings_accounts")
public class SavingsAccount extends BaseEntity {

    @Column(name = "account_number", unique = true, nullable = false)
    private String accountNumber;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    private BigDecimal balance;

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Member getMember() {
        return member;
    }

    public void setMember(Member member) {
        this.member = member;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
