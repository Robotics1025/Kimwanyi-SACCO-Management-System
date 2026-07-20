package org.joel.kimwanyisacco.controller;

import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.service.MemberService;
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
