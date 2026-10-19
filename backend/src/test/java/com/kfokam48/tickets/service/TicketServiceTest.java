package com.kfokam48.tickets.service;

import com.kfokam48.tickets.domain.TicketPriority;
import com.kfokam48.tickets.domain.TicketStatus;
import com.kfokam48.tickets.dto.CommentRequest;
import com.kfokam48.tickets.dto.TicketRequest;
import com.kfokam48.tickets.dto.TicketResponse;
import com.kfokam48.tickets.dto.TicketStatsResponse;
import com.kfokam48.tickets.exception.BusinessRuleException;
import com.kfokam48.tickets.exception.ResourceNotFoundException;
import com.kfokam48.tickets.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class TicketServiceTest {

    @Autowired
    private TicketService ticketService;

    @Autowired
    private TicketRepository ticketRepository;

    private TicketRequest request(String title, TicketPriority priority, String assignee) {
        return new TicketRequest(
                title,
                "Description detaillee du probleme rencontre par le demandeur.",
                priority,
                "Reseau",
                "Awa Ndiaye",
                "awa.ndiaye@kfokam48.cm",
                assignee
        );
    }

    @Test
    void createInitializesOpenTicketWithReference() {
        TicketResponse created = ticketService.create(request("Connexion impossible", TicketPriority.HIGH, null));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.reference()).matches("TKT-\\d{4}");
        assertThat(created.assignee()).isNull();
        assertThat(created.commentCount()).isZero();
        assertThat(ticketRepository.count()).isEqualTo(1);
    }

    @Test
    void createTrimsValuesAndNormalizesEmail() {
        TicketRequest input = new TicketRequest(
                "  Ecran noir au demarrage  ",
                "  L'ecran reste noir apres le demarrage du poste.  ",
                TicketPriority.MEDIUM,
                "   ",
                "  Ulrich Siko  ",
                "  SIKOULRICH204@GMAIL.COM ",
                "   "
        );

        TicketResponse created = ticketService.create(input);

        assertThat(created.title()).isEqualTo("Ecran noir au demarrage");
        assertThat(created.description()).startsWith("L'ecran reste noir");
        assertThat(created.category()).isNull();
        assertThat(created.requesterName()).isEqualTo("Ulrich Siko");
        assertThat(created.requesterEmail()).isEqualTo("sikoulrich204@gmail.com");
        assertThat(created.assignee()).isNull();
    }

    @Test
    void searchFiltersByStatusPriorityAndText() {
        ticketService.create(request("Probleme de connexion VPN", TicketPriority.CRITICAL, null));
        ticketService.create(request("Imprimante hors service", TicketPriority.LOW, null));

        assertThat(ticketService.search(null, null, null, PageRequest.of(0, 10)).totalElements())
                .isEqualTo(2);
        assertThat(ticketService.search(null, TicketPriority.CRITICAL, null, PageRequest.of(0, 10))
                .content()).hasSize(1);
        assertThat(ticketService.search(null, null, "imprimante", PageRequest.of(0, 10))
                .content()).hasSize(1);
        assertThat(ticketService.search(TicketStatus.RESOLVED, null, null, PageRequest.of(0, 10))
                .totalElements()).isZero();
    }

    @Test
    void fullLifecycleAllowsCommentsThenClosureAndDeletion() {
        TicketResponse created = ticketService.create(request("Serveur de fichiers inaccessible", TicketPriority.HIGH, null));
        Long id = created.id();

        TicketResponse inProgress = ticketService.changeStatus(id, TicketStatus.IN_PROGRESS);
        assertThat(inProgress.status()).isEqualTo(TicketStatus.IN_PROGRESS);

        ticketService.assign(id, "Jean Technicien");
        ticketService.addComment(id, new CommentRequest("Jean Technicien", "Diagnostic en cours."));

        TicketResponse resolved = ticketService.changeStatus(id, TicketStatus.RESOLVED);
        assertThat(resolved.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(resolved.assignee()).isEqualTo("Jean Technicien");
        assertThat(resolved.commentCount()).isEqualTo(1);
        assertThat(resolved.comments()).hasSize(1);

        TicketResponse closed = ticketService.changeStatus(id, TicketStatus.CLOSED);
        assertThat(closed.status()).isEqualTo(TicketStatus.CLOSED);

        ticketService.delete(id);
        assertThat(ticketRepository.existsById(id)).isFalse();
    }

    @Test
    void transitionFromOpenDirectlyToResolvedIsRejected() {
        TicketResponse created = ticketService.create(request("Demande d'acces", TicketPriority.LOW, "Paul"));

        assertThatThrownBy(() -> ticketService.changeStatus(created.id(), TicketStatus.RESOLVED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Transition non autorisee");
    }

    @Test
    void resolvingRequiresAnAssignee() {
        TicketResponse created = ticketService.create(request("Demande de licence", TicketPriority.MEDIUM, null));
        ticketService.changeStatus(created.id(), TicketStatus.IN_PROGRESS);

        assertThatThrownBy(() -> ticketService.changeStatus(created.id(), TicketStatus.RESOLVED))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("assigne");
    }

    @Test
    void closedTicketIsImmutable() {
        TicketResponse created = ticketService.create(request("Ticket a cloturer", TicketPriority.LOW, "Paul"));
        ticketService.changeStatus(created.id(), TicketStatus.IN_PROGRESS);
        ticketService.changeStatus(created.id(), TicketStatus.RESOLVED);
        ticketService.changeStatus(created.id(), TicketStatus.CLOSED);

        assertThatThrownBy(() -> ticketService.update(created.id(),
                request("Modification interdite", TicketPriority.HIGH, "Paul")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cloture");
        assertThatThrownBy(() -> ticketService.addComment(created.id(),
                new CommentRequest("Paul", "Un dernier commentaire.")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cloture");
        assertThatThrownBy(() -> ticketService.assign(created.id(), "Marie"))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void deletingAnOpenTicketIsRejected() {
        TicketResponse created = ticketService.create(request("Ticket encore ouvert", TicketPriority.MEDIUM, null));

        assertThatThrownBy(() -> ticketService.delete(created.id()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("cloture");
    }

    @Test
    void unknownTicketThrowsNotFound() {
        assertThatThrownBy(() -> ticketService.findById(9999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void statsReflectTicketCounts() {
        TicketResponse first = ticketService.create(request("Premier ticket", TicketPriority.HIGH, null));
        ticketService.create(request("Deuxieme ticket", TicketPriority.HIGH, "Paul"));
        ticketService.changeStatus(first.id(), TicketStatus.IN_PROGRESS);

        TicketStatsResponse stats = ticketService.stats();

        assertThat(stats.total()).isEqualTo(2);
        assertThat(stats.byStatus().get(TicketStatus.OPEN)).isEqualTo(1);
        assertThat(stats.byStatus().get(TicketStatus.IN_PROGRESS)).isEqualTo(1);
        assertThat(stats.byPriority().get(TicketPriority.HIGH)).isEqualTo(2);
        assertThat(stats.unassigned()).isEqualTo(1);
    }

    @Test
    void unsupportedSortFieldIsRejected() {
        assertThatThrownBy(() -> ticketService.search(null, null, null,
                PageRequest.of(0, 10, org.springframework.data.domain.Sort.by("hackedField"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Tri non supporte");
    }
}
