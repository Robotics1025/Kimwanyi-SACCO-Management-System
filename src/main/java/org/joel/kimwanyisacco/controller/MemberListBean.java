package org.joel.kimwanyisacco.controller;

import java.util.List;
import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.service.MemberService;
import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component("memberListBean")
@RequestScope
public class MemberListBean {

    private final MemberService memberService;

    private List<MemberDto> members;

    public MemberListBean(MemberService memberService) {
        this.memberService = memberService;
    }

    public List<MemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<MemberDto> members) {
        this.members = members;
    }
}
