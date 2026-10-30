package com.frameload.cash.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.frameload.cash.dto.UserRequest;
import com.frameload.cash.dto.UserResponse;
import com.frameload.cash.entity.User;
import com.frameload.cash.enums.Role;
import com.frameload.cash.enums.UserStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

/**
 * Teste la gestion des utilisateurs avec la vraie persistance (H2) : mot de passe haché, nom unique, statut.
 */
@SpringBootTest
@ActiveProfiles("test")
class UserServicesImplTest {

    private static final String PASSWORD = "Secret-12345";

    @Autowired
    private UserServices userServices;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String uniqueUsername() {
        return "user-" + UUID.randomUUID().toString().substring(0, 8);
    }

    private UserResponse newUser(Role role) {
        return userServices.create(new UserRequest(uniqueUsername(), PASSWORD, role));
    }

    @Test
    void create_storesOnlyABcryptHashOfThePassword() {
        UserResponse created = newUser(Role.AGENT);

        User stored = userRepository.findById(created.id()).orElseThrow();
        assertThat(stored.getPasswordHash()).isNotEqualTo(PASSWORD).startsWith("$2");
        assertThat(passwordEncoder.matches(PASSWORD, stored.getPasswordHash())).isTrue();
        assertThat(passwordEncoder.matches("mauvais-mot-de-passe", stored.getPasswordHash())).isFalse();
    }

    @Test
    void create_givesTwoUsersWithTheSamePasswordDifferentHashes() {
        User first = userRepository.findById(newUser(Role.AGENT).id()).orElseThrow();
        User second = userRepository.findById(newUser(Role.AGENT).id()).orElseThrow();

        assertThat(first.getPasswordHash()).isNotEqualTo(second.getPasswordHash());
    }

    @Test
    void create_startsActiveWithTheRequestedRole() {
        UserResponse created = newUser(Role.SUPERVISOR);

        assertThat(created.status()).isEqualTo(UserStatus.ACTIVE);
        assertThat(created.role()).isEqualTo(Role.SUPERVISOR);
        assertThat(created.createdAt()).isNotNull();
    }

    @Test
    void create_rejectsADuplicateUsernameIgnoringCase() {
        String username = uniqueUsername();
        userServices.create(new UserRequest(username, PASSWORD, Role.AGENT));

        assertThatThrownBy(() -> userServices.create(new UserRequest(username.toUpperCase(), PASSWORD, Role.ADMIN)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("nom d'utilisateur");
    }

    @Test
    void findAll_containsTheCreatedUser() {
        UserResponse created = newUser(Role.AGENT);

        assertThat(userServices.findAll()).extracting(UserResponse::id).contains(created.id());
    }

    @Test
    void changeStatus_deactivatesThenReactivatesAUser() {
        UserResponse created = newUser(Role.AGENT);

        assertThat(userServices.changeStatus(created.id(), UserStatus.INACTIVE).status())
                .isEqualTo(UserStatus.INACTIVE);
        assertThat(userServices.changeStatus(created.id(), UserStatus.ACTIVE).status())
                .isEqualTo(UserStatus.ACTIVE);
    }

    @Test
    void changeStatus_withAnUnknownUser_throwsNotFound() {
        assertThatThrownBy(() -> userServices.changeStatus(999_999L, UserStatus.INACTIVE))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}