package com.ttthinh.shoe_shop_basic.auth.repository;

import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, String> {
    @EntityGraph(attributePaths = {"roles", "roles.permissions", "providers"})
    Optional<UserAccount> findByEmail(String email);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "providers"})
    Optional<UserAccount> findByPhone(String phone);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "providers"})
    Optional<UserAccount> findByEmailOrPhone(String email, String phone);

    @EntityGraph(attributePaths = {"roles", "roles.permissions", "providers"})
    Optional<UserAccount> findWithRolesAndProvidersById(String id);

    boolean existsByEmail(String email);

    boolean existsByPhone(String phone);
}
