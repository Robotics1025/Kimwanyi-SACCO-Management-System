package org.joel.kimwanyisacco.controller;

import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.dto.MemberUpdateForm;
import org.joel.kimwanyisacco.common.util.FacesMessageUtil;
import org.joel.kimwanyisacco.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;
import jakarta.annotation.PostConstruct;

@Component("memberProfileBean")
@RequestScope
public class MemberProfileBean {

    private final MemberService memberService;
    private final UserSessionBean userSessionBean;

    private MemberDto member;
    private MemberUpdateForm form = new MemberUpdateForm();

    public MemberProfileBean(MemberService memberService, UserSessionBean userSessionBean) {
        this.memberService = memberService;
        this.userSessionBean = userSessionBean;
    }

    @PostConstruct
    public void init() {
        member = memberService.getByUserAccountId(userSessionBean.getLoggedInUser().getId());
        form.setId(member.getId());
        form.setEmail(member.getEmail());
        form.setPhoneNumber(member.getPhoneNumber());
    }

    public String update() {
        try {
            member = memberService.updateProfile(userSessionBean.getLoggedInUser().getId(), form);
            FacesMessageUtil.addInfoMessage("Profile updated successfully");
            return "/members/profile.xhtml?faces-redirect=true";
        } catch (RuntimeException exception) {
            FacesMessageUtil.addErrorMessage(exception.getMessage());
            return null;
        }
    }

    public MemberDto getMember() {
        return member;
    }

    public void setMember(MemberDto member) {
        this.member = member;
    }

    public MemberUpdateForm getForm() { return form; }
    public void setForm(MemberUpdateForm form) { this.form = form; }
}
