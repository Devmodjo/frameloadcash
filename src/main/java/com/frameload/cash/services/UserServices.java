package com.frameload.cash.services;

import com.frameload.cash.dto.UserRequest;
import com.frameload.cash.dto.UserResponse;
import com.frameload.cash.enums.UserStatus;
import java.util.List;

public interface UserServices {

    UserResponse create(UserRequest request);

    List<UserResponse> findAll();

    UserResponse changeStatus(Long id, UserStatus status);
}