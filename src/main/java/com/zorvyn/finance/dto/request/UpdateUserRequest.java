package com.zorvyn.finance.dto.request;

import com.zorvyn.finance.model.Role;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateUserRequest {

    @Size(min = 2, max = 100, message = "Full name must be between 2 and 100 characters")
    private String fullName;

    /** Only ADMIN may change roles. */
    private Role role;

    /** Active/inactive status — ADMIN only. */
    private Boolean active;
}
