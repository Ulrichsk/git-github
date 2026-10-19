package com.kfokam48.tickets.web;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kfokam48.tickets.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TicketApiIntegrationTest {

    private static final String BASE = "/api/tickets";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TicketRepository ticketRepository;

    @BeforeEach
    void cleanDatabase() {
        ticketRepository.deleteAll();
    }

    private String payload(String title, String priority, String assignee) {
        return """
                {
                  "title": "%s",
                  "description": "Description suffisamment longue pour passer la validation.",
                  "priority": "%s",
                  "category": "Materiel",
                  "requesterName": "Ulrich Siko",
                  "requesterEmail": "sikoulrich204@gmail.com",
                  "assignee": %s
                }
                """.formatted(title, priority, assignee == null ? "null" : "\"" + assignee + "\"");
    }

    private JsonNode createTicket(String title, String priority, String assignee) throws Exception {
        MvcResult result = mockMvc.perform(post(BASE)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload(title, priority, assignee)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    @Test
    void createAndReadTicket() throws Exception {
        JsonNode created = createTicket("Connexion impossible au portail", "HIGH", null);

        assertThat(created.get("id").asLong()).isPositive();
        assertThat(created.get("reference").asText()).matches("TKT-\\d{4}");
        assertThat(created.get("status").asText()).isEqualTo("OPEN");
        assertThat(created.get("priority").asText()).isEqualTo("HIGH");

        mockMvc.perform(get(BASE + "/" + created.get("id").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Connexion impossible au portail"))
                .andExpect(jsonPath("$.comments").isArray())
                .andExpect(jsonPath("$.comments.length()").value(0));
    }

    @Test
    void listSupportsFilteringSearchAndPagination() throws Exception {
        createTicket("Probleme VPN site distant", "CRITICAL", null);
        createTicket("Imprimante du service RH", "LOW", null);
        createTicket("Acces au dossier partage", "HIGH", "Paul");

        mockMvc.perform(get(BASE).param("priority", "CRITICAL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].priority").value("CRITICAL"));

        mockMvc.perform(get(BASE).param("q", "imprimante"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get(BASE).param("size", "2").param("page", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.first").value(true));
    }

    @Test
    void validationErrorsAreReturnedAsProblemDetail() throws Exception {
        String invalid = """
                {
                  "title": "abc",
                  "description": "court",
                  "priority": null,
                  "requesterName": "",
                  "requesterEmail": "pas-un-email"
                }
                """;

        mockMvc.perform(post(BASE).contentType(MediaType.APPLICATION_JSON).content(invalid))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Donnees invalides"))
                .andExpect(jsonPath("$.errors.title").exists())
                .andExpect(jsonPath("$.errors.description").exists())
                .andExpect(jsonPath("$.errors.priority").exists())
                .andExpect(jsonPath("$.errors.requesterEmail").exists());
    }

    @Test
    void workflowFromOpenToClosedWithComment() throws Exception {
        long id = createTicket("Serveur de messagerie sature", "HIGH", null).get("id").asLong();

        mockMvc.perform(patch(BASE + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));

        mockMvc.perform(patch(BASE + "/" + id + "/assignee")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assignee\":\"Jean Technicien\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee").value("Jean Technicien"));

        mockMvc.perform(post(BASE + "/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"author\":\"Jean Technicien\",\"content\":\"Redemarrage du service effectue.\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.author").value("Jean Technicien"));

        mockMvc.perform(patch(BASE + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.commentCount").value(1))
                .andExpect(jsonPath("$.comments.length()").value(1));

        mockMvc.perform(patch(BASE + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"CLOSED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));

        mockMvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(BASE + "/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Ressource introuvable"));
    }

    @Test
    void invalidTransitionReturnsConflict() throws Exception {
        long id = createTicket("Demande de nouveau materiel", "MEDIUM", null).get("id").asLong();

        mockMvc.perform(patch(BASE + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Regle metier non respectee"));
    }

    @Test
    void resolvingWithoutAssigneeReturnsConflict() throws Exception {
        long id = createTicket("Installer un logiciel comptable", "MEDIUM", null).get("id").asLong();

        mockMvc.perform(patch(BASE + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"IN_PROGRESS\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(patch(BASE + "/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Un ticket doit etre assigne avant d'etre resolu."));
    }

    @Test
    void deletingOpenTicketReturnsConflict() throws Exception {
        long id = createTicket("Ticket encore en cours", "LOW", null).get("id").asLong();

        mockMvc.perform(delete(BASE + "/" + id))
                .andExpect(status().isConflict());
    }

    @Test
    void statsAndOpenApiDocumentationAreAvailable() throws Exception {
        createTicket("Premier incident", "CRITICAL", null);
        createTicket("Second incident", "LOW", "Marie");

        mockMvc.perform(get(BASE + "/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(2))
                .andExpect(jsonPath("$.byStatus.OPEN").value(2))
                .andExpect(jsonPath("$.byPriority.CRITICAL").value(1))
                .andExpect(jsonPath("$.unassigned").value(1));

        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.info.title").value("API de gestion des tickets de support"))
                .andExpect(jsonPath("$.paths['/api/tickets']").exists());
    }
}
