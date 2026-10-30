package com.frameload.cash.dto;

import com.frameload.cash.entity.User;
import com.frameload.cash.enums.Role;
import com.frameload.cash.enums.UserStatus;
import java.time.LocalDate;

/**
 * Représentation d'un utilisateur exposée par l'API : ni mot de passe, ni hachage.
 */
public record UserResponse(Long id, String username, Role role, UserStatus status, LocalDate createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getRole(), user.getStatus(),
                user.getCreatedAt());
    }
}