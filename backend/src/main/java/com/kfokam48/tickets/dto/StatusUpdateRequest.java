package com.kfokam48.tickets.dto;

import com.kfokam48.tickets.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusUpdateRequest(
        @NotNull(message = "Le statut cible est obligatoire")
        TicketStatus status
) {
}
