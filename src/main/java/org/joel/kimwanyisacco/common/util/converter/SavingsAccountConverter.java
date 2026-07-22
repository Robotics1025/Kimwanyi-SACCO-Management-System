package org.joel.kimwanyisacco.common.util.converter;

import org.joel.kimwanyisacco.dto.SavingsAccountDto;
import org.joel.kimwanyisacco.model.SavingsAccount;
import org.springframework.stereotype.Component;

@Component
public class SavingsAccountConverter {

    public SavingsAccountDto toDto(SavingsAccount account) {
        SavingsAccountDto dto = new SavingsAccountDto();
        dto.setId(account.getId());
        dto.setAccountNumber(account.getAccountNumber());
        dto.setMemberId(account.getMember() != null ? account.getMember().getId() : null);
        dto.setBalance(account.getBalance());
        return dto;
    }
}
