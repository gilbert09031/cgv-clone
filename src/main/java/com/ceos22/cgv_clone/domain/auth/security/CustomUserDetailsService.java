package com.ceos22.cgv_clone.domain.auth.security;

import com.ceos22.cgv_clone.common.error.CustomException;
import com.ceos22.cgv_clone.common.error.ErrorCode;
import com.ceos22.cgv_clone.domain.member.entity.Member;
import com.ceos22.cgv_clone.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.transaction.annotation.Transactional;

@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    @Override
    public UserDetails loadUserByUsername(String memberId) {
        Member member = memberRepository.findById(parseMemberId(memberId))
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        return CustomUserDetails.from(member);
    }

    private Long parseMemberId(String memberId) {
        try {
            return Long.parseLong(memberId);
        } catch (NumberFormatException e) {
            // 토큰 subject가 숫자가 아니면 존재하지 않는 회원과 동일하게 인증 실패로 처리한다.
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
    }
}
