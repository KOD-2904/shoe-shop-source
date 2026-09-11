package com.ttthinh.shoe_shop_basic.auth.repository;

import com.ttthinh.shoe_shop_basic.auth.entity.EmailVerifyToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailVerifyRepository extends JpaRepository<EmailVerifyToken, String> {
    Optional<EmailVerifyToken> findByToken(String token);
}
