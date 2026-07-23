package org.joel.kimwanyisacco.controller;

import jakarta.annotation.PostConstruct;
import java.util.List;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.repository.UserAccountRepository;
import org.joel.kimwanyisacco.service.EmailService;
import org.joel.kimwanyisacco.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("emailComposerBean")
@RequestScope
public class EmailComposerBean {
    private final MemberService memberService;
    private final EmailService emailService;
    private final UserSessionBean session;
    private final UserAccountRepository accounts;
    private List<MemberDto> members;
    private String recipientMode = "MEMBER";
    private Long memberId;
    private String group = "ACTIVE";
    private String subject;
    private String message;

    public EmailComposerBean(MemberService memberService, EmailService emailService,
            UserSessionBean session, UserAccountRepository accounts) {
        this.memberService = memberService; this.emailService = emailService;
        this.session = session; this.accounts = accounts;
    }
    @PostConstruct public void init() { members = memberService.search(null); }
    public void send() {
        try {
            if (!"GROUP".equals(recipientMode) && memberId == null)
                throw new IllegalArgumentException("Select at least one recipient");
            List<Long> ids = "GROUP".equals(recipientMode)
                    ? members.stream().filter(m -> "ALL".equals(group) || group.equals(m.getStatus())).map(MemberDto::getId).toList()
                    : List.of(memberId);
            if (ids.isEmpty()) throw new IllegalArgumentException("No members match the selected recipient group");
            var actor = accounts.findById(session.getLoggedInUser().getId()).orElseThrow();
            int sent = emailService.sendToMembers(ids, subject, message, actor);
            FacesMessageUtil.addInfoMessage("Email delivered to " + sent + " recipient(s)");
            subject = null; message = null;
        } catch (Exception e) { FacesMessageUtil.addErrorMessage(e.getMessage()); }
    }
    public List<MemberDto> getMembers() { return members; }
    public String[] getGroupOptions() { return new String[]{"ACTIVE", "PENDING", "INACTIVE", "ALL"}; }
    public long getRecipientCount() {
        return members.stream().filter(m -> "ALL".equals(group) || group.equals(m.getStatus()))
                .filter(m -> m.getEmail() != null && !m.getEmail().isBlank()).count();
    }
    public long getSkippedRecipientCount() {
        return members.stream().filter(m -> "ALL".equals(group) || group.equals(m.getStatus()))
                .filter(m -> m.getEmail() == null || m.getEmail().isBlank()).count();
    }
    public String getRecipientMode() { return recipientMode; }
    public void setRecipientMode(String value) { recipientMode = value; }
    public Long getMemberId() { return memberId; }
    public void setMemberId(Long value) { memberId = value; }
    public String getGroup() { return group; }
    public void setGroup(String value) { group = value; }
    public String getSubject() { return subject; }
    public void setSubject(String value) { subject = value; }
    public String getMessage() { return message; }
    public void setMessage(String value) { message = value; }
}
