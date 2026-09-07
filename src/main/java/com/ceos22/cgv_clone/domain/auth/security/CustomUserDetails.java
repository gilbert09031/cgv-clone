package com.ceos22.cgv_clone.domain.auth.security;

import com.ceos22.cgv_clone.domain.member.entity.Member;
import com.ceos22.cgv_clone.domain.member.entity.Role;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

@Getter
public class CustomUserDetails implements UserDetails {

    private static final String ROLE_PREFIX = "ROLE_";

    private final Long memberId;
    private final String email;
    private final String password;
    private final Role role;
    private final Collection<? extends GrantedAuthority> authorities;

    private CustomUserDetails(Long memberId, String email, String password, Role role) {
        this.memberId = memberId;
        this.email = email;
        this.password = password;
        this.role = role;
        this.authorities = List.of(new SimpleGrantedAuthority(ROLE_PREFIX + role.name()));
    }

    public static CustomUserDetails from(Member member) {
        return new CustomUserDetails(
                member.getMemberId(),
                member.getEmail().getValue(),
                member.getPassword().getValue(),
                member.getRole()
        );
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return String.valueOf(memberId);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof CustomUserDetails other
                && Objects.equals(memberId, other.memberId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId);
    }

    /** 비밀번호 해시가 로그로 새어나가지 않도록 최소 정보만 노출한다. */
    @Override
    public String toString() {
        return "CustomUserDetails(memberId=" + memberId + ", role=" + role + ")";
    }
}
