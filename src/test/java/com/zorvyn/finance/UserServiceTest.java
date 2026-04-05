package com.zorvyn.finance;

import com.zorvyn.finance.dto.request.UpdateUserRequest;
import com.zorvyn.finance.dto.response.UserResponse;
import com.zorvyn.finance.exception.AppExceptions;
import com.zorvyn.finance.model.Role;
import com.zorvyn.finance.model.User;
import com.zorvyn.finance.repository.UserRepository;
import com.zorvyn.finance.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserService Unit Tests")
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    private User adminUser;
    private User viewerUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .id(1L).fullName("Admin User").email("admin@test.com")
                .role(Role.ADMIN).active(true).build();

        viewerUser = User.builder()
                .id(2L).fullName("Viewer User").email("viewer@test.com")
                .role(Role.VIEWER).active(true).build();
    }

    // -------------------------------------------------------
    // getAllUsers
    // -------------------------------------------------------

    @Test
    @DisplayName("getAllUsers() — returns mapped list")
    void getAllUsers_returnsList() {
        when(userRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(adminUser, viewerUser));

        List<UserResponse> result = userService.getAllUsers();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getEmail()).isEqualTo("admin@test.com");
    }

    // -------------------------------------------------------
    // getUserById
    // -------------------------------------------------------

    @Test
    @DisplayName("getUserById() — found returns UserResponse")
    void getUserById_found() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));

        UserResponse response = userService.getUserById(2L);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getRole()).isEqualTo(Role.VIEWER);
    }

    @Test
    @DisplayName("getUserById() — not found throws ResourceNotFoundException")
    void getUserById_notFound_throws() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserById(99L))
                .isInstanceOf(AppExceptions.ResourceNotFoundException.class);
    }

    // -------------------------------------------------------
    // updateUser
    // -------------------------------------------------------

    @Test
    @DisplayName("updateUser() — ADMIN can change role of another user")
    void updateUser_admin_changesRole() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(Role.ANALYST);

        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.updateUser(2L, req, adminUser);

        assertThat(result.getRole()).isEqualTo(Role.ANALYST);
    }

    @Test
    @DisplayName("updateUser() — non-admin cannot change another user's role")
    void updateUser_nonAdmin_changesOtherRole_throws() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setRole(Role.ADMIN);

        when(userRepository.findById(1L)).thenReturn(Optional.of(adminUser));

        // viewerUser trying to edit adminUser's profile
        assertThatThrownBy(() -> userService.updateUser(1L, req, viewerUser))
                .isInstanceOf(AppExceptions.AccessDeniedException.class);
    }

    @Test
    @DisplayName("updateUser() — user can update their own name")
    void updateUser_self_canUpdateName() {
        UpdateUserRequest req = new UpdateUserRequest();
        req.setFullName("New Name");

        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        UserResponse result = userService.updateUser(2L, req, viewerUser);

        assertThat(result.getFullName()).isEqualTo("New Name");
    }

    // -------------------------------------------------------
    // deleteUser
    // -------------------------------------------------------

    @Test
    @DisplayName("deleteUser() — admin deletes another user")
    void deleteUser_admin_success() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(viewerUser));

        userService.deleteUser(2L, adminUser);

        verify(userRepository).delete(viewerUser);
    }

    @Test
    @DisplayName("deleteUser() — admin cannot delete themselves")
    void deleteUser_self_throws() {
        assertThatThrownBy(() -> userService.deleteUser(1L, adminUser))
                .isInstanceOf(AppExceptions.BadRequestException.class)
                .hasMessageContaining("cannot delete your own account");
    }
}
