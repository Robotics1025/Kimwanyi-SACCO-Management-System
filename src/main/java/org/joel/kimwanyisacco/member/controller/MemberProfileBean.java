package org.joel.kimwanyisacco.member.controller;

import org.joel.kimwanyisacco.member.dto.MemberDto;
import org.joel.kimwanyisacco.member.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("memberProfileBean")
@RequestScope
public class MemberProfileBean {

    private final MemberService memberService;

    private MemberDto member;

    public MemberProfileBean(MemberService memberService) {
        this.memberService = memberService;
    }

    public MemberDto getMember() {
        return member;
    }

    public void setMember(MemberDto member) {
        this.member = member;
    }
}
