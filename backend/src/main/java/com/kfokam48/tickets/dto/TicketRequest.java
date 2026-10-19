package com.kfokam48.tickets.dto;

import com.kfokam48.tickets.domain.TicketPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TicketRequest(

        @NotBlank(message = "Le titre est obligatoire")
        @Size(min = 5, max = 120, message = "Le titre doit contenir entre 5 et 120 caracteres")
        String title,

        @NotBlank(message = "La description est obligatoire")
        @Size(min = 10, max = 4000, message = "La description doit contenir entre 10 et 4000 caracteres")
        String description,

        @NotNull(message = "La priorite est obligatoire")
        TicketPriority priority,

        @Size(max = 60, message = "La categorie ne peut pas depasser 60 caracteres")
        String category,

        @NotBlank(message = "Le nom du demandeur est obligatoire")
        @Size(max = 120, message = "Le nom du demandeur ne peut pas depasser 120 caracteres")
        String requesterName,

        @NotBlank(message = "L'email du demandeur est obligatoire")
        @Email(message = "L'email du demandeur n'est pas valide")
        @Size(max = 180, message = "L'email du demandeur ne peut pas depasser 180 caracteres")
        String requesterEmail,

        @Size(max = 120, message = "Le nom de l'assigne ne peut pas depasser 120 caracteres")
        String assignee
) {
}
