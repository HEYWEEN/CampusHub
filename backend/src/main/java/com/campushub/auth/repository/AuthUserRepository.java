package com.campushub.auth.repository;

import com.campushub.auth.entity.AuthUser;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthUserRepository extends JpaRepository<AuthUser, Long> {

    Optional<AuthUser> findByPhoneHmac(String phoneHmac);

    boolean existsByPhoneHmac(String phoneHmac);
}
