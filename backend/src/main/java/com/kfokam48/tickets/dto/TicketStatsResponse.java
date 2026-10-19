package com.kfokam48.tickets.dto;

import com.kfokam48.tickets.domain.TicketPriority;
import com.kfokam48.tickets.domain.TicketStatus;

import java.util.Map;

public record TicketStatsResponse(
        long total,
        Map<TicketStatus, Long> byStatus,
        Map<TicketPriority, Long> byPriority,
        long unassigned
) {
}
