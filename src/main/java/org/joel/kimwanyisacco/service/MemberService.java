package org.joel.kimwanyisacco.service;

import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.model.Member;
import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.dto.MemberUpdateForm;
import java.util.List;

public interface MemberService {

    Member registerMember(MemberRegistrationForm form);
    MemberDto getByUserAccountId(Long userAccountId);
    List<MemberDto> search(String keyword);
    MemberDto updateProfile(Long userAccountId, MemberUpdateForm form);
}
