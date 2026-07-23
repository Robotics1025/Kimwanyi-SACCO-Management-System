package org.joel.kimwanyisacco.service;

import java.time.LocalDateTime;
import java.util.List;
import org.apache.commons.mail2.jakarta.DefaultAuthenticator;
import org.apache.commons.mail2.jakarta.SimpleEmail;
import org.joel.kimwanyisacco.model.EmailLog;
import org.joel.kimwanyisacco.model.UserAccount;
import org.joel.kimwanyisacco.model.enums.AuditAction;
import org.joel.kimwanyisacco.model.enums.EmailStatus;
import org.joel.kimwanyisacco.repository.EmailLogRepository;
import org.joel.kimwanyisacco.repository.MemberRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailServiceImpl implements EmailService {
    private final MemberRepository members;
    private final EmailLogRepository logs;
    private final AuditLogService audit;
    private final boolean enabled;
    private final String host, username, password, from;
    private final int port;

    public EmailServiceImpl(MemberRepository members, EmailLogRepository logs, AuditLogService audit,
            @Value("${mail.enabled:false}") boolean enabled, @Value("${mail.host}") String host,
            @Value("${mail.port}") int port, @Value("${mail.username:}") String username,
            @Value("${mail.password:}") String password, @Value("${mail.from:}") String from) {
        this.members = members; this.logs = logs; this.audit = audit; this.enabled = enabled;
        this.host = host; this.port = port; this.username = username; this.password = password; this.from = from;
    }

    @Override
    @Transactional
    public int sendToMembers(List<Long> memberIds, String subject, String body, UserAccount actor) {
        if (!enabled || username.isBlank() || password.isBlank())
            throw new IllegalStateException("Email is not configured. Set MAIL_ENABLED, MAIL_USERNAME and MAIL_PASSWORD.");
        if (subject == null || subject.isBlank() || body == null || body.isBlank())
            throw new IllegalArgumentException("Subject and message are required");
        int sent = 0;
        for (Long id : memberIds.stream().distinct().toList()) {
            var member = members.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));
            String recipient = member.getUserAccount().getEmail();
            EmailLog log = new EmailLog(); log.setUserAccount(member.getUserAccount());
            log.setRecipientEmail(recipient); log.setSubject(subject.trim()); log.setMessage(body.trim());
            try {
                SimpleEmail email = new SimpleEmail();
                email.setHostName(host);
                email.setSmtpPort(port);
                email.setAuthenticator(new DefaultAuthenticator(username, password));
                email.setStartTLSEnabled(true);
                email.setStartTLSRequired(true);
                email.setSSLCheckServerIdentity(true);
                email.setCharset("UTF-8");
                email.setFrom(from.isBlank() ? username : from);
                email.addTo(recipient);
                email.setSubject(subject.trim());
                email.setMsg(body.trim());
                email.send();
                log.setStatus(EmailStatus.SENT); log.setSentAt(LocalDateTime.now()); sent++;
            } catch (Exception ex) { log.setStatus(EmailStatus.FAILED); log.setFailureReason(ex.getMessage()); }
            logs.save(log);
        }
        audit.record(actor, sent > 0 ? AuditAction.EMAIL_SENT : AuditAction.EMAIL_FAILED, "EmailLog", null,
                "Sent " + sent + " of " + memberIds.size() + " message(s): " + subject.trim());
        return sent;
    }
}
