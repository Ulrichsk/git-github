package com.kfokam48.tickets.service;

import com.kfokam48.tickets.domain.Ticket;
import com.kfokam48.tickets.domain.TicketComment;
import com.kfokam48.tickets.domain.TicketPriority;
import com.kfokam48.tickets.domain.TicketStatus;
import com.kfokam48.tickets.dto.CommentRequest;
import com.kfokam48.tickets.dto.CommentResponse;
import com.kfokam48.tickets.dto.PageResponse;
import com.kfokam48.tickets.dto.TicketRequest;
import com.kfokam48.tickets.dto.TicketResponse;
import com.kfokam48.tickets.dto.TicketStatsResponse;
import com.kfokam48.tickets.exception.BusinessRuleException;
import com.kfokam48.tickets.exception.ResourceNotFoundException;
import com.kfokam48.tickets.repository.TicketRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class TicketService {

    /** Transitions autorisees du cycle de vie d'un ticket. */
    private static final Map<TicketStatus, Set<TicketStatus>> ALLOWED_TRANSITIONS = Map.of(
            TicketStatus.OPEN, EnumSet.of(TicketStatus.IN_PROGRESS),
            TicketStatus.IN_PROGRESS, EnumSet.of(TicketStatus.OPEN, TicketStatus.RESOLVED),
            TicketStatus.RESOLVED, EnumSet.of(TicketStatus.IN_PROGRESS, TicketStatus.CLOSED),
            TicketStatus.CLOSED, EnumSet.noneOf(TicketStatus.class)
    );

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "reference", "title", "status", "priority", "category",
            "requesterName", "assignee", "createdAt", "updatedAt"
    );

    private final TicketRepository ticketRepository;

    public TicketService(TicketRepository ticketRepository) {
        this.ticketRepository = ticketRepository;
    }

    public PageResponse<TicketResponse> search(TicketStatus status, TicketPriority priority,
                                               String query, Pageable pageable) {
        validateSort(pageable.getSort());

        Specification<Ticket> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (priority != null) {
                predicates.add(cb.equal(root.get("priority"), priority));
            }
            if (query != null && !query.isBlank()) {
                String like = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), like),
                        cb.like(cb.lower(root.get("description")), like),
                        cb.like(cb.lower(root.get("requesterName")), like),
                        cb.like(cb.lower(root.get("reference")), like)
                ));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Ticket> page = ticketRepository.findAll(specification, pageable);
        return PageResponse.from(page, TicketResponse::summary);
    }

    public TicketResponse findById(Long id) {
        return TicketResponse.detail(getTicket(id));
    }

    public TicketStatsResponse stats() {
        Map<TicketStatus, Long> byStatus = new EnumMap<>(TicketStatus.class);
        for (TicketStatus status : TicketStatus.values()) {
            byStatus.put(status, ticketRepository.countByStatus(status));
        }
        Map<TicketPriority, Long> byPriority = new EnumMap<>(TicketPriority.class);
        for (TicketPriority priority : TicketPriority.values()) {
            byPriority.put(priority, ticketRepository.countByPriority(priority));
        }
        return new TicketStatsResponse(
                ticketRepository.count(),
                byStatus,
                byPriority,
                ticketRepository.countByAssigneeIsNull()
        );
    }

    @Transactional
    public TicketResponse create(TicketRequest request) {
        Ticket ticket = new Ticket();
        ticket.setStatus(TicketStatus.OPEN);
        applyRequest(ticket, request);

        // La reference lisible depend de l'identifiant auto-genere.
        Ticket saved = ticketRepository.saveAndFlush(ticket);
        saved.setReference(String.format("TKT-%04d", saved.getId()));
        return TicketResponse.detail(ticketRepository.save(saved));
    }

    @Transactional
    public TicketResponse update(Long id, TicketRequest request) {
        Ticket ticket = getTicket(id);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Un ticket cloture ne peut plus etre modifie.");
        }
        applyRequest(ticket, request);
        return TicketResponse.detail(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse changeStatus(Long id, TicketStatus target) {
        Ticket ticket = getTicket(id);
        TicketStatus current = ticket.getStatus();

        if (current == target) {
            throw new BusinessRuleException("Le ticket possede deja le statut " + target + ".");
        }
        if (!ALLOWED_TRANSITIONS.get(current).contains(target)) {
            throw new BusinessRuleException(
                    "Transition non autorisee de " + current + " vers " + target + ".");
        }
        if (target == TicketStatus.RESOLVED && isBlank(ticket.getAssignee())) {
            throw new BusinessRuleException("Un ticket doit etre assigne avant d'etre resolu.");
        }

        ticket.setStatus(target);
        return TicketResponse.detail(ticketRepository.save(ticket));
    }

    @Transactional
    public TicketResponse assign(Long id, String assignee) {
        Ticket ticket = getTicket(id);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Un ticket cloture ne peut plus etre reassigne.");
        }
        ticket.setAssignee(assignee.trim());
        return TicketResponse.detail(ticketRepository.save(ticket));
    }

    @Transactional
    public CommentResponse addComment(Long id, CommentRequest request) {
        Ticket ticket = getTicket(id);
        if (ticket.getStatus() == TicketStatus.CLOSED) {
            throw new BusinessRuleException("Impossible d'ajouter un commentaire a un ticket cloture.");
        }
        TicketComment comment = new TicketComment();
        comment.setAuthor(request.author().trim());
        comment.setContent(request.content().trim());
        ticket.addComment(comment);
        ticketRepository.save(ticket);
        return CommentResponse.from(comment);
    }

    @Transactional
    public void delete(Long id) {
        Ticket ticket = getTicket(id);
        if (ticket.getStatus() != TicketStatus.CLOSED) {
            throw new BusinessRuleException("Seul un ticket cloture peut etre supprime.");
        }
        ticketRepository.delete(ticket);
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket introuvable : " + id));
    }

    private void applyRequest(Ticket ticket, TicketRequest request) {
        ticket.setTitle(request.title().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setCategory(trimToNull(request.category()));
        ticket.setRequesterName(request.requesterName().trim());
        ticket.setRequesterEmail(request.requesterEmail().trim().toLowerCase(Locale.ROOT));
        ticket.setAssignee(trimToNull(request.assignee()));
    }

    private void validateSort(Sort sort) {
        for (Sort.Order order : sort) {
            if (!SORTABLE_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException(
                        "Tri non supporte : " + order.getProperty()
                                + ". Champs autorises : " + String.join(", ", SORTABLE_FIELDS));
            }
        }
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
