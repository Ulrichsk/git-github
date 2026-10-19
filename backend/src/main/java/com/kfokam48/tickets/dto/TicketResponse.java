package com.kfokam48.tickets.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.kfokam48.tickets.domain.Ticket;
import com.kfokam48.tickets.domain.TicketPriority;
import com.kfokam48.tickets.domain.TicketStatus;

import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String reference,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String category,
        String requesterName,
        String requesterEmail,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        int commentCount,
        @JsonInclude(JsonInclude.Include.NON_NULL) List<CommentResponse> comments
) {
    public static TicketResponse summary(Ticket ticket) {
        return from(ticket, null);
    }

    public static TicketResponse detail(Ticket ticket) {
        return from(ticket, ticket.getComments().stream().map(CommentResponse::from).toList());
    }

    private static TicketResponse from(Ticket ticket, List<CommentResponse> comments) {
        return new TicketResponse(
                ticket.getId(),
                ticket.getReference(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getCategory(),
                ticket.getRequesterName(),
                ticket.getRequesterEmail(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getComments().size(),
                comments
        );
    }
}
