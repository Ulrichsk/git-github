package com.kfokam48.tickets.domain;

/**
 * Cycle de vie d'un ticket : OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED.
 * Les transitions autorisees sont definies dans {@link com.kfokam48.tickets.service.TicketService}.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
