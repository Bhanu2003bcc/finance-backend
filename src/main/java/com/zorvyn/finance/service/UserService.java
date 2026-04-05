package com.zorvyn.finance.service;

import com.zorvyn.finance.dto.request.UpdateUserRequest;
import com.zorvyn.finance.dto.response.UserResponse;
import com.zorvyn.finance.model.User;

import java.util.List;

public interface UserService {

    List<UserResponse> getAllUsers();

    UserResponse getUserById(Long id);

    UserResponse updateUser(Long id, UpdateUserRequest request, User currentUser);

    void deleteUser(Long id, User currentUser);

    UserResponse getMyProfile(User currentUser);
}
