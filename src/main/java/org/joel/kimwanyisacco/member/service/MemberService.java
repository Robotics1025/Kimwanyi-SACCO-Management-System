package org.joel.kimwanyisacco.member.service;

import java.util.List;
import org.joel.kimwanyisacco.member.dto.MemberDto;
import org.joel.kimwanyisacco.member.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.member.dto.MemberUpdateForm;

public interface MemberService {

    MemberDto registerMember(MemberRegistrationForm form);

    MemberDto updateMember(MemberUpdateForm form);

    MemberDto getMemberById(Long id);

    List<MemberDto> listMembers();
}
