package org.joel.kimwanyisacco.common.util.converter;

import org.joel.kimwanyisacco.dto.SavingsTransactionDto;
import org.joel.kimwanyisacco.model.SavingsTransaction;
import org.springframework.stereotype.Component;

@Component
public class SavingsTransactionConverter {

    public SavingsTransactionDto toDto(SavingsTransaction transaction) {
        SavingsTransactionDto dto = new SavingsTransactionDto();
        dto.setId(transaction.getId());
        dto.setTransactionType(transaction.getType() != null ? transaction.getType().name() : null);
        dto.setAmount(transaction.getAmount());
        dto.setBalanceAfter(transaction.getBalanceAfter());
        dto.setCreatedAt(transaction.getCreatedAt());
        dto.setReference(transaction.getReference());
        dto.setDescription(transaction.getDescription());
        return dto;
    }
}
