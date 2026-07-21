package org.joel.kimwanyisacco.service;

import org.joel.kimwanyisacco.dto.MemberRegistrationForm;
import org.joel.kimwanyisacco.model.Member;

public interface MemberService {

    Member registerMember(MemberRegistrationForm form);
}