package com.kfokam48.tickets.web;

import com.kfokam48.tickets.domain.TicketPriority;
import com.kfokam48.tickets.domain.TicketStatus;
import com.kfokam48.tickets.dto.AssigneeRequest;
import com.kfokam48.tickets.dto.CommentRequest;
import com.kfokam48.tickets.dto.CommentResponse;
import com.kfokam48.tickets.dto.PageResponse;
import com.kfokam48.tickets.dto.StatusUpdateRequest;
import com.kfokam48.tickets.dto.TicketRequest;
import com.kfokam48.tickets.dto.TicketResponse;
import com.kfokam48.tickets.dto.TicketStatsResponse;
import com.kfokam48.tickets.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@Tag(name = "Tickets", description = "Gestion des tickets de support")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @GetMapping
    @Operation(summary = "Lister et filtrer les tickets")
    public PageResponse<TicketResponse> search(
            @Parameter(description = "Filtre sur le statut") @RequestParam(required = false) TicketStatus status,
            @Parameter(description = "Filtre sur la priorite") @RequestParam(required = false) TicketPriority priority,
            @Parameter(description = "Recherche dans le titre, la description, la reference et le demandeur")
            @RequestParam(required = false, name = "q") String query,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ticketService.search(status, priority, query, pageable);
    }

    @GetMapping("/stats")
    @Operation(summary = "Obtenir les statistiques des tickets")
    public TicketStatsResponse stats() {
        return ticketService.stats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Consulter un ticket et ses commentaires")
    public TicketResponse findById(@PathVariable Long id) {
        return ticketService.findById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Creer un ticket")
    public TicketResponse create(@Valid @RequestBody TicketRequest request) {
        return ticketService.create(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier un ticket non cloture")
    public TicketResponse update(@PathVariable Long id, @Valid @RequestBody TicketRequest request) {
        return ticketService.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Changer le statut d'un ticket")
    public TicketResponse changeStatus(@PathVariable Long id,
                                       @Valid @RequestBody StatusUpdateRequest request) {
        return ticketService.changeStatus(id, request.status());
    }

    @PatchMapping("/{id}/assignee")
    @Operation(summary = "Assigner un ticket a un technicien")
    public TicketResponse assign(@PathVariable Long id,
                                 @Valid @RequestBody AssigneeRequest request) {
        return ticketService.assign(id, request.assignee());
    }

    @PostMapping("/{id}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Ajouter un commentaire a un ticket")
    public CommentResponse addComment(@PathVariable Long id,
                                      @Valid @RequestBody CommentRequest request) {
        return ticketService.addComment(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Supprimer un ticket cloture")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        ticketService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
