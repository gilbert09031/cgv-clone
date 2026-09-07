package com.ceos22.cgv_clone.domain.member.entity;

import com.ceos22.cgv_clone.common.entity.BaseEntity;
import com.ceos22.cgv_clone.domain.member.service.PasswordEncryptor;
import com.ceos22.cgv_clone.domain.member.vo.BirthDate;
import com.ceos22.cgv_clone.domain.member.vo.Email;
import com.ceos22.cgv_clone.domain.member.vo.MemberName;
import com.ceos22.cgv_clone.domain.member.vo.Password;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name = "member")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)   // JPA 전용 통로. 애플리케이션 코드는 signUp()만 사용한다.
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @Embedded
    private Email email;

    @Embedded
    private Password password;

    @Embedded
    private MemberName name;

    @Embedded
    private BirthDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20, nullable = false)
    private Role role;

    private Member(Email email, Password password, MemberName name, BirthDate birthDate, Role role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.birthDate = birthDate;
        this.role = role;
    }

    public static Member signUp(
            String email,
            String rawPassword,
            String name,
            LocalDate birthDate,
            PasswordEncryptor passwordEncryptor
    ) {
        return new Member(
                Email.from(email),
                Password.from(rawPassword, passwordEncryptor),
                MemberName.from(name),
                BirthDate.from(birthDate),
                Role.USER
        );
    }

    public boolean matchesPassword(String rawPassword, PasswordEncryptor passwordEncryptor) {
        return this.password.matches(rawPassword, passwordEncryptor);
    }
}
