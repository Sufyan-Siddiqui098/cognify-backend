package com.cognify.cognify_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request payload for updating a user's active/inactive status.
 */
@Data
public class AdminUserStatusRequest {

    @NotNull
    private Boolean active;
}
