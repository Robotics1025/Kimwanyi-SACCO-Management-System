package org.joel.kimwanyisacco.controller;

import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;

@Component
@RequestScope
public class MemberRegistrationBean {

    private final MemberService memberService;

    private MemberRegistrationForm form =
            new MemberRegistrationForm();

    public MemberRegistrationBean(
            MemberService memberService
    ) {
        this.memberService = memberService;
    }

    public String register() {
        try {
            memberService.registerMember(form);

            FacesContext fc = FacesContext.getCurrentInstance();
            fc.getExternalContext().getFlash().setKeepMessages(true);
            fc.addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_INFO,
                    "Account Created!",
                    "Your account was created successfully. Please sign in."
            ));

            form = new MemberRegistrationForm();
            return "/login.xhtml?faces-redirect=true";

        } catch (IllegalArgumentException exception) {

            FacesContext.getCurrentInstance()
                    .addMessage(
                            null,
                            new FacesMessage(
                                    FacesMessage.SEVERITY_ERROR,
                                    "Registration failed",
                                    exception.getMessage()
                            )
                    );

            return null;
        }
    }

    public MemberRegistrationForm getForm() {
        return form;
    }

    public void setForm(MemberRegistrationForm form) {
        this.form = form;
    }
}