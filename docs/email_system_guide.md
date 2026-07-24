# Kimwanyi SACCO Management System: Email Subsystem Guide

This guide describes how the email composition interface, backing controllers, configurations, and the transactional SMTP delivery service are designed and integrated within the application.

---

## 1. High-Level Architectural Flow

```
[Admin Clicks "Send Email" on Sidebar]
                  │
                  ▼
[admin-template.xhtml (Global Dialog)] ── (Binds input values) ──► [EmailComposerBean (Controller)]
                                                                           │
                                                                           ▼
[MySQL Database (EmailLog / AuditLog)] ◄── (Logs results) ─── [EmailServiceImpl (Spring Service)]
                                                                           │
                                                                           ▼
                                                              [Apache Commons Mail (SMTP)]
                                                                           │
                                                                           ▼
                                                              [Target Member Inbox (SMTP)]
```

---

## 2. Configuration (`application.properties`)

The email system uses standard SMTP (e.g., Google Gmail SMTP) for dispatch. Outbound mail is **disabled by default** to prevent application crashes during offline local testing.

```properties
mail.enabled=${MAIL_ENABLED:false}
mail.host=${MAIL_HOST:smtp.gmail.com}
mail.port=${MAIL_PORT:587}
mail.username=${MAIL_USERNAME:}
mail.password=${MAIL_PASSWORD:}
mail.from=${MAIL_FROM:${MAIL_USERNAME:}}
```

### Enable Email in Production/Test
Provide the following environment variables when running Tomcat:
* `MAIL_ENABLED=true`
* `MAIL_HOST=smtp.gmail.com`
* `MAIL_PORT=587`
* `MAIL_USERNAME=your-sacco-email@gmail.com`
* `MAIL_PASSWORD=your-google-app-password` (Note: Use a Google App Password, not your standard Gmail login password).

---

## 3. UI Layer: The Composer Dialog

Instead of having a dedicated page for composing emails, the email composer dialog is **globally registered** in the `/WEB-INF/templates/admin-template.xhtml` template layout. This allows administrators to launch the email composer from any administration screen (e.g., member list, loans review, dashboard).

### Recipient Modes:
1. **Single Member (`MEMBER`)**:
   - Selects a specific member from a list populated from `MemberService.search(null)`.
2. **Member Group (`GROUP`)**:
   - Targets a category: `ACTIVE`, `PENDING`, `INACTIVE`, or `ALL`.
   - The UI automatically calculates:
     - **Eligible Recipients**: The count of members in the selected group who have a valid email address.
     - **Skipped Recipients**: The count of members in the group who do not have an email address populated in their profile.

---

## 4. Backing Bean: `EmailComposerBean.java`

* **Scope**: `@RequestScope`
* **Core Function**: Validates input selection and compiles the list of recipient primary keys (`memberIds`).
* **Code Trace (`send` Method)**:
```java
public void send() {
    try {
        if (!"GROUP".equals(recipientMode) && memberId == null)
            throw new IllegalArgumentException("Select at least one recipient");
            
        // 1. Compile target ID list based on selected mode
        List<Long> ids = "GROUP".equals(recipientMode)
                ? members.stream()
                         .filter(m -> "ALL".equals(group) || group.equals(m.getStatus()))
                         .map(MemberDto::getId).toList()
                : List.of(memberId);
                
        if (ids.isEmpty()) 
            throw new IllegalArgumentException("No members match the selected recipient group");
            
        // 2. Fetch logged-in admin user
        var actor = accounts.findById(session.getLoggedInUser().getId()).orElseThrow();
        
        // 3. Delegate sending to EmailService
        int sent = emailService.sendToMembers(ids, subject, message, actor);
        
        FacesMessageUtil.addInfoMessage("Email delivered to " + sent + " recipient(s)");
        subject = null; 
        message = null;
    } catch (Exception e) { 
        FacesMessageUtil.addErrorMessage(e.getMessage()); 
    }
}
```

---

## 5. Email Service: `EmailServiceImpl.java`

* **Techno Stack**: Uses **Apache Commons Mail 2** (`org.apache.commons.mail2.jakarta.SimpleEmail`) with SMTP Authentication over TLS 587.
* **Database Ledger Logging**: For each member recipient, the service constructs a database log entry (`EmailLog`) containing the recipient's email address, subject, message, and execution details.
* **Error Tolerant Dispatch**: The service loops through all recipients. If sending to one recipient fails (e.g. invalid email address format, network timeout), it catches the exception, updates that specific record status as `FAILED` (with the error message logged in `failureReason`), and **continues** dispatching to the remaining recipients in the list.

### Code Trace (`sendToMembers` Method):
```java
@Override
@Transactional
public int sendToMembers(List<Long> memberIds, String subject, String body, UserAccount actor) {
    // 1. Assert email configurations are enabled and present
    if (!enabled || username.isBlank() || password.isBlank())
        throw new IllegalStateException("Email is not configured. Set MAIL_ENABLED, MAIL_USERNAME and MAIL_PASSWORD.");
    if (subject == null || subject.isBlank() || body == null || body.isBlank())
        throw new IllegalArgumentException("Subject and message are required");
        
    int sent = 0;
    
    // 2. Loop through distinct recipients
    for (Long id : memberIds.stream().distinct().toList()) {
        var member = members.findById(id).orElseThrow(() -> new IllegalArgumentException("Member not found: " + id));
        String recipient = member.getUserAccount().getEmail();
        
        EmailLog log = new EmailLog(); 
        log.setUserAccount(member.getUserAccount());
        log.setRecipientEmail(recipient); 
        log.setSubject(subject.trim()); 
        log.setMessage(body.trim());
        
        try {
            // 3. Configure Apache Commons SMTP connection
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
            
            // 4. Dispatch email
            email.send();
            
            log.setStatus(EmailStatus.SENT); 
            log.setSentAt(LocalDateTime.now()); 
            sent++;
        } catch (Exception ex) { 
            // Catch error on individual recipient, update status to FAILED, and continue loop
            log.setStatus(EmailStatus.FAILED); 
            log.setFailureReason(ex.getMessage()); 
        }
        
        // 5. Save the email dispatch history record
        logs.save(log);
    }
    
    // 6. Record aggregate action in general System Audit Log
    audit.record(actor, sent > 0 ? AuditAction.EMAIL_SENT : AuditAction.EMAIL_FAILED, "EmailLog", null,
            "Sent " + sent + " of " + memberIds.size() + " message(s): " + subject.trim());
            
    return sent;
}
```
