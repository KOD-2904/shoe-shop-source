package com.ttthinh.shoe_shop_basic.config;

import com.ttthinh.shoe_shop_basic.auth.entity.Permission;
import com.ttthinh.shoe_shop_basic.auth.entity.Role;
import com.ttthinh.shoe_shop_basic.auth.entity.UserAccount;
import com.ttthinh.shoe_shop_basic.auth.enums.AuthProvider;
import com.ttthinh.shoe_shop_basic.auth.enums.UserStatus;
import com.ttthinh.shoe_shop_basic.auth.repository.PermissionRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.RoleRepository;
import com.ttthinh.shoe_shop_basic.auth.repository.UserAccountRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashSet;

@Configuration
@Slf4j
public class ApplicationInitConfig {
    private final PasswordEncoder passwordEncoder;

    @Value("${app.init.admin-email:admin@shoe-shop.local}")
    private String adminEmail;

    @Value("${app.init.admin-password:ChangeMe123!}")
    private String adminPassword;

    @Value("${app.init.demo-email:demo@shoe-shop.local}")
    private String demoEmail;

    @Value("${app.init.demo-password:ChangeMe123!}")
    private String demoPassword;

    public ApplicationInitConfig(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Bean
    @ConditionalOnProperty(prefix = "app.init", name = "enabled", havingValue = "true", matchIfMissing = true)
    ApplicationRunner runner(
            UserAccountRepository userAccountRepository,
            RoleRepository roleRepository,
            PermissionRepository permissionRepository
    ) {
        return args -> {
            log.info("===== START INITIALIZING DATABASE =====");

            Role roleUser = getOrCreateRole(roleRepository, "ROLE_USER", "User", "Default user role");
            Role roleAdmin = getOrCreateRole(roleRepository, "ROLE_ADMIN", "Admin", "Administrator");
            Role roleStaff = getOrCreateRole(roleRepository, "ROLE_STAFF", "Staff", "Staff");
            seedPermissions(permissionRepository, roleAdmin, roleStaff, roleUser, roleRepository);

            if (!userAccountRepository.existsByEmail(adminEmail)) {
                UserAccount admin = new UserAccount();
                admin.setEmail(adminEmail);
                admin.setPassword(passwordEncoder.encode(adminPassword));
                admin.setPhone("0123456789");
                admin.addProvider(AuthProvider.LOCAL);
                admin.setStatus(UserStatus.ACTIVE);
                admin.setEmailVerified(true);

                HashSet<Role> roles = new HashSet<>();
                roles.add(roleAdmin);
                roles.add(roleUser);
                admin.setRoles(roles);

                userAccountRepository.save(admin);
                log.info("Created initial admin user with email: {}", adminEmail);
            } else {
                userAccountRepository.findByEmail(adminEmail).ifPresent(admin -> {
                    admin.getRoles().add(roleAdmin);
                    admin.getRoles().add(roleUser);
                    admin.addProvider(AuthProvider.LOCAL);
                    admin.setStatus(UserStatus.ACTIVE);
                    admin.setEmailVerified(true);
                    userAccountRepository.save(admin);
                    log.info("Ensured initial admin user has admin roles and local provider: {}", adminEmail);
                });
            }

            if (!userAccountRepository.existsByEmail(demoEmail)) {
                UserAccount user = new UserAccount();
                user.setEmail(demoEmail);
                user.setPassword(passwordEncoder.encode(demoPassword));
                user.setPhone("0987654321");
                user.addProvider(AuthProvider.LOCAL);
                user.setStatus(UserStatus.ACTIVE);
                user.setEmailVerified(true);

                HashSet<Role> roles = new HashSet<>();
                roles.add(roleUser);
                user.setRoles(roles);

                userAccountRepository.save(user);
                log.info("Created initial demo user with email: {}", demoEmail);
            } else {
                userAccountRepository.findByEmail(demoEmail).ifPresent(user -> {
                    user.addProvider(AuthProvider.LOCAL);
                    userAccountRepository.save(user);
                    log.info("Ensured initial demo user has local provider: {}", demoEmail);
                });
            }

            log.info("===== DATABASE INITIALIZATION COMPLETED =====");
        };
    }

    private Role getOrCreateRole(RoleRepository roleRepository, String code, String name, String description) {
        return roleRepository.findByCode(code)
                .orElseGet(() -> {
                    Role role = new Role();
                    role.setCode(code);
                    role.setName(name);
                    role.setDescription(description);
                    roleRepository.save(role);
                    log.info("Created {}", code);
                    return role;
                });
    }

    private void seedPermissions(
            PermissionRepository permissionRepository,
            Role roleAdmin,
            Role roleStaff,
            Role roleUser,
            RoleRepository roleRepository
    ) {
        Permission manageCatalog = getOrCreatePermission(permissionRepository, "CATALOG_MANAGE", "Manage catalog", "Create and update products, variants, brands, and categories");
        Permission manageOrders = getOrCreatePermission(permissionRepository, "ORDER_MANAGE", "Manage orders", "Update order status and shipping handoff");
        Permission manageInventory = getOrCreatePermission(permissionRepository, "INVENTORY_MANAGE", "Manage inventory", "Adjust stock and inventory settings");
        Permission manageUsers = getOrCreatePermission(permissionRepository, "USER_MANAGE", "Manage users", "View and administer users");
        Permission shop = getOrCreatePermission(permissionRepository, "SHOP", "Shop", "Browse, cart, checkout, orders, wishlist, and reviews");

        roleAdmin.getPermissions().addAll(java.util.Set.of(manageCatalog, manageOrders, manageInventory, manageUsers, shop));
        roleStaff.getPermissions().addAll(java.util.Set.of(manageCatalog, manageOrders, manageInventory));
        roleUser.getPermissions().add(shop);

        roleRepository.save(roleAdmin);
        roleRepository.save(roleStaff);
        roleRepository.save(roleUser);
    }

    private Permission getOrCreatePermission(PermissionRepository permissionRepository, String code, String name, String description) {
        return permissionRepository.findByCode(code)
                .orElseGet(() -> {
                    Permission permission = new Permission();
                    permission.setCode(code);
                    permission.setName(name);
                    permission.setDescription(description);
                    permissionRepository.save(permission);
                    log.info("Created {}", code);
                    return permission;
                });
    }
}
