package com.kfokam48.tickets.dto;

import com.kfokam48.tickets.domain.TicketComment;

import java.time.Instant;

public record CommentResponse(
        Long id,
        String author,
        String content,
        Instant createdAt
) {
    public static CommentResponse from(TicketComment comment) {
        return new CommentResponse(
                comment.getId(),
                comment.getAuthor(),
                comment.getContent(),
                comment.getCreatedAt()
        );
    }
}
