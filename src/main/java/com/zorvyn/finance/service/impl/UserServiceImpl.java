package com.zorvyn.finance.service.impl;

import com.zorvyn.finance.dto.request.UpdateUserRequest;
import com.zorvyn.finance.dto.response.UserResponse;
import com.zorvyn.finance.exception.AppExceptions;
import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(UserResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = findUserOrThrow(id);
        return UserResponse.from(user);
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UpdateUserRequest request, User currentUser) {
        User target = findUserOrThrow(id);

        // A non-admin user can only update their own profile
        if (currentUser.getRole() != Role.ADMIN && !currentUser.getId().equals(id)) {
            throw new AppExceptions.AccessDeniedException(
                    "You are not allowed to modify another user's profile.");
        }

        // Only ADMIN may change role or active status
        if (currentUser.getRole() != Role.ADMIN) {
            if (request.getRole() != null || request.getActive() != null) {
                throw new AppExceptions.AccessDeniedException(
                        "Only admins can change user roles or active status.");
            }
        }

        // Prevent an admin from deactivating themselves
        if (request.getActive() != null
                && Boolean.FALSE.equals(request.getActive())
                && currentUser.getId().equals(id)) {
            throw new AppExceptions.BadRequestException(
                    "You cannot deactivate your own account.");
        }

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            target.setFullName(request.getFullName().trim());
        }
        if (request.getRole() != null) {
            target.setRole(request.getRole());
        }
        if (request.getActive() != null) {
            target.setActive(request.getActive());
        }

        target = userRepository.save(target);
        log.info("User {} updated by {}", target.getEmail(), currentUser.getEmail());
        return UserResponse.from(target);
    }

    @Override
    @Transactional
    public void deleteUser(Long id, User currentUser) {
        if (currentUser.getId().equals(id)) {
            throw new AppExceptions.BadRequestException("You cannot delete your own account.");
        }

        User target = findUserOrThrow(id);
        userRepository.delete(target);
        log.info("User {} deleted by admin {}", target.getEmail(), currentUser.getEmail());
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(User currentUser) {
        return UserResponse.from(currentUser);
    }

    // Helpers

    private User findUserOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new AppExceptions.ResourceNotFoundException(
                        "User not found with id: " + id));
    }
}
