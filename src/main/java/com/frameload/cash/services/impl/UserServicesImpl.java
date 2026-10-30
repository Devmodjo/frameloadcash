package com.frameload.cash.services.impl;

import com.frameload.cash.dto.UserRequest;
import com.frameload.cash.dto.UserResponse;
import com.frameload.cash.entity.User;
import com.frameload.cash.enums.UserStatus;
import com.frameload.cash.exception.BusinessRuleException;
import com.frameload.cash.exception.ResourceNotFoundException;
import com.frameload.cash.repositories.UserRepository;
import com.frameload.cash.services.UserServices;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des utilisateurs de démonstration (cahier des charges, §16.1). Le mot de passe est haché avant tout
 * enregistrement ; aucune méthode ne retourne ni ne journalise un mot de passe ou un hachage.
 */
@Service
public class UserServicesImpl implements UserServices {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServicesImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public UserResponse create(UserRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.username())) {
            throw new BusinessRuleException("DUPLICATE_USERNAME", "Ce nom d'utilisateur est déjà utilisé.");
        }

        User user = new User(request.username(), passwordEncoder.encode(request.password()), request.role());
        userRepository.save(user);
        return UserResponse.from(user);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> findAll() {
        return userRepository.findAll(Sort.by("id")).stream().map(UserResponse::from).toList();
    }

    @Override
    @Transactional
    public UserResponse changeStatus(Long id, UserStatus status) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur introuvable : " + id));
        user.setStatus(status);
        return UserResponse.from(user);
    }
}