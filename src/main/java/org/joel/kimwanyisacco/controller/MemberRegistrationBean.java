package org.joel.kimwanyisacco.controller;

import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.springframework.dao.DataIntegrityViolationException;

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
                    "Application submitted",
                    "Your account is awaiting administrator approval. You will be able to sign in after approval."
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
        } catch (DataIntegrityViolationException exception) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_WARN,
                    "Account already registered",
                    "That username, email address, or National ID is already registered. "
                            + "If you recently applied, your account may already be waiting for administrator approval."
            ));
            return null;
        } catch (RuntimeException exception) {
            FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(
                    FacesMessage.SEVERITY_ERROR,
                    "Registration could not be completed",
                    "Please try again once. If the problem continues, contact the SACCO administrator."
            ));
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
