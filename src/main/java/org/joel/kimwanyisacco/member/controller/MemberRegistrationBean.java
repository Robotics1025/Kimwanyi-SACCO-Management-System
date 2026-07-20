package org.joel.kimwanyisacco.member.controller;

import org.joel.kimwanyisacco.member.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.member.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("memberRegistrationBean")
@RequestScope
public class MemberRegistrationBean {

    private final MemberService memberService;

    private MemberRegistrationForm registrationForm = new MemberRegistrationForm();

    public MemberRegistrationBean(MemberService memberService) {
        this.memberService = memberService;
    }

    public MemberRegistrationForm getRegistrationForm() {
        return registrationForm;
    }

    public void setRegistrationForm(MemberRegistrationForm registrationForm) {
        this.registrationForm = registrationForm;
    }

    public String register() {
        return null;
    }
}
