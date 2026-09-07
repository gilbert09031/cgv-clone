package com.ceos22.cgv_clone.common.security;

import com.ceos22.cgv_clone.domain.member.service.PasswordEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SpringPasswordEncryptor implements PasswordEncryptor {

    private final PasswordEncoder passwordEncoder;

    @Override
    public String encrypt(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String encryptedPassword) {
        return passwordEncoder.matches(rawPassword, encryptedPassword);
    }
}
