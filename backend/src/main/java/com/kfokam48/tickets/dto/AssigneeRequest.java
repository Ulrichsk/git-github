package com.kfokam48.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssigneeRequest(
        @NotBlank(message = "Le nom de l'assigne est obligatoire")
        @Size(max = 120, message = "Le nom de l'assigne ne peut pas depasser 120 caracteres")
        String assignee
) {
}
