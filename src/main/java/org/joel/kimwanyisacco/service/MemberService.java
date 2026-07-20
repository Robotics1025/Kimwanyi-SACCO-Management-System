package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.dto.MemberUpdateForm;

public interface MemberService {

    MemberDto registerMember(MemberRegistrationForm form);

    MemberDto updateMember(MemberUpdateForm form);

    MemberDto getMemberById(Long id);

    List<MemberDto> listMembers();
}
