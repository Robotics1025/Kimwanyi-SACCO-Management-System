package org.joel.kimwanyisacco.service;

import java.util.List;

import org.joel.kimwanyisacco.dto.UserAccountDto;
import org.joel.kimwanyisacco.model.UserAccount;

public interface UserManagementService {

    List<UserAccountDto> search(String keyword);

    void setEnabled(Long userId, boolean enabled, UserAccount adminAccount);

    void resetPassword(Long userId, String newPassword, UserAccount adminAccount);

    void approveMember(Long userId, UserAccount adminAccount);
}
