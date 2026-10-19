package com.kfokam48.tickets.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(

        @NotBlank(message = "L'auteur du commentaire est obligatoire")
        @Size(max = 120, message = "L'auteur ne peut pas depasser 120 caracteres")
        String author,

        @NotBlank(message = "Le contenu du commentaire est obligatoire")
        @Size(min = 2, max = 2000, message = "Le commentaire doit contenir entre 2 et 2000 caracteres")
        String content
) {
}
