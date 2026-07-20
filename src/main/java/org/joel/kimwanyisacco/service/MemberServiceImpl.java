package org.joel.kimwanyisacco.service;

import java.util.List;
import org.joel.kimwanyisacco.dto.MemberDto;
import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.dto.MemberUpdateForm;
import org.springframework.stereotype.Service;

@Service
public class MemberServiceImpl implements MemberService {

    @Override
    public MemberDto registerMember(MemberRegistrationForm form) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MemberDto updateMember(MemberUpdateForm form) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public MemberDto getMemberById(Long id) {
        throw new UnsupportedOperationException("not implemented");
    }

    @Override
    public List<MemberDto> listMembers() {
        throw new UnsupportedOperationException("not implemented");
    }
}
