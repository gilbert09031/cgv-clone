package com.ceos22.cgv_clone.domain.member.service;

public interface PasswordEncryptor {

    String encrypt(String rawPassword);

    boolean matches(String rawPassword, String encryptedPassword);
}
