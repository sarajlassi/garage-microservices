package com.garage.vehicle.service;

import com.garage.vehicle.dto.VehicleDto;
import com.garage.vehicle.entity.ServiceStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final VehicleService vehicleService;

    @Value("${ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ollama.model:llama3.2}")
    private String ollamaModel;

    private record OllamaMessage(String role, String content) {}
    private record OllamaChatRequest(String model, List<OllamaMessage> messages, boolean stream) {}
    private record OllamaMessageWrapper(String content) {}
    private record OllamaChatResponse(OllamaMessageWrapper message) {}

    public String chat(String mechanicUsername, String userMessage) {
        String systemPrompt = buildSystemPrompt(mechanicUsername);

        List<OllamaMessage> messages = List.of(
                new OllamaMessage("system", systemPrompt),
                new OllamaMessage("user", userMessage)
        );

        OllamaChatRequest request = new OllamaChatRequest(ollamaModel, messages, false);

        try {
            OllamaChatResponse response = RestClient.builder()
                    .baseUrl(ollamaBaseUrl)
                    .build()
                    .post()
                    .uri("/api/chat")
                    .body(request)
                    .retrieve()
                    .body(OllamaChatResponse.class);

            if (response != null && response.message() != null) {
                return response.message().content();
            }
            return "Je n'ai pas pu générer une réponse. Veuillez réessayer.";
        } catch (Exception e) {
            log.error("Ollama request failed: {}", e.getMessage());
            throw new IllegalStateException(
                    "Le service de chatbot est indisponible. Assurez-vous qu'Ollama est démarré (ollama serve) et que le modèle '"
                            + ollamaModel + "' est installé (ollama pull " + ollamaModel + ").");
        }
    }

    private String buildSystemPrompt(String mechanicUsername) {
        List<VehicleDto.ServiceRecordResponse> myRecords =
                vehicleService.getServiceRecordsByMechanic(mechanicUsername);

        long totalRepairs = myRecords.size();
        long scheduled = myRecords.stream()
                .filter(r -> r.getServiceStatus() == ServiceStatus.SCHEDULED)
                .count();
        long inProgress = myRecords.stream()
                .filter(r -> r.getServiceStatus() == ServiceStatus.IN_PROGRESS)
                .count();
        long completed = myRecords.stream()
                .filter(r -> r.getServiceStatus() == ServiceStatus.COMPLETED)
                .count();

        String vehicleList = myRecords.stream()
                .collect(Collectors.toMap(
                        VehicleDto.ServiceRecordResponse::getVehicleId,
                        r -> r.getVehicleLicensePlate() + " (ID:" + r.getVehicleId() + ")",
                        (a, b) -> a
                ))
                .values().stream()
                .sorted()
                .collect(Collectors.joining(", "));

        String upcomingRepairs = myRecords.stream()
                .filter(r -> r.getServiceStatus() == ServiceStatus.SCHEDULED
                        || r.getServiceStatus() == ServiceStatus.IN_PROGRESS)
                .map(r -> String.format("  - [%s] %s | Véhicule: %s | Date planifiée: %s | Type: %s",
                        r.getServiceStatus(),
                        r.getDescription() != null ? r.getDescription() : "Pas de description",
                        r.getVehicleLicensePlate(),
                        r.getScheduledDate() != null ? r.getScheduledDate().toString() : "Non planifié",
                        r.getServiceType() != null ? r.getServiceType() : "N/A"))
                .collect(Collectors.joining("\n"));

        return String.format("""
                Tu es un assistant pour les mécaniciens d'un garage automobile.
                Réponds toujours en français de manière concise et professionnelle.

                === DONNÉES DU MÉCANICIEN ===
                Mécanicien: %s
                Date actuelle: %s

                === STATISTIQUES ===
                Total des réparations assignées: %d
                  - Planifiées: %d
                  - En cours: %d
                  - Terminées: %d

                === VÉHICULES ===
                %s

                === RÉPARATIONS EN COURS / PLANIFIÉES ===
                %s

                Réponds uniquement aux questions liées à ses véhicules, réparations et planning.
                """,
                mechanicUsername,
                LocalDate.now(),
                totalRepairs,
                scheduled,
                inProgress,
                completed,
                vehicleList.isEmpty() ? "Aucun véhicule" : vehicleList,
                upcomingRepairs.isEmpty() ? "Aucune réparation en cours ou planifiée" : upcomingRepairs
        );
    }
}
