package com.kfokam48.tickets.repository;

import com.kfokam48.tickets.domain.Ticket;
import com.kfokam48.tickets.domain.TicketPriority;
import com.kfokam48.tickets.domain.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TicketRepository extends JpaRepository<Ticket, Long>, JpaSpecificationExecutor<Ticket> {

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    long countByAssigneeIsNull();
}
