package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.model.UserAccount;

public interface EmailService {
    int sendToMembers(List<Long> memberIds, String subject, String message, UserAccount actor);
}
