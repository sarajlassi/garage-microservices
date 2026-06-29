package com.garage.vehicle.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.garage.vehicle.dto.VehicleDto;
import com.garage.vehicle.entity.ServiceStatus;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatbotService {

    private final VehicleService vehicleService;

    @Value("${anthropic.api.key:}")
    private String anthropicApiKey;

    private AnthropicClient anthropicClient;

    @PostConstruct
    public void init() {
        if (anthropicApiKey != null && !anthropicApiKey.isBlank()) {
            anthropicClient = AnthropicOkHttpClient.builder()
                    .apiKey(anthropicApiKey)
                    .build();
        } else {
            anthropicClient = AnthropicOkHttpClient.fromEnv();
        }
    }

    public String chat(String mechanicUsername, String userMessage) {
        String systemPrompt = buildSystemPrompt(mechanicUsername);

        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.CLAUDE_OPUS_4_8)
                .maxTokens(1024L)
                .addSystemMessage(systemPrompt)
                .addUserMessage(userMessage)
                .build();

        Message response = anthropicClient.messages().create(params);

        return response.content().stream()
                .flatMap(block -> block.text().stream())
                .map(textBlock -> textBlock.text())
                .collect(Collectors.joining("\n"));
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
                Tu es un assistant intelligent pour les mécaniciens d'un garage automobile.
                Tu aides le mécanicien à gérer et consulter ses réparations, véhicules et planning.
                Réponds toujours en français de manière concise et professionnelle.

                === DONNÉES DU MÉCANICIEN ===
                Mécanicien: %s
                Date actuelle: %s

                === STATISTIQUES ===
                Total des réparations assignées: %d
                  - Planifiées: %d
                  - En cours: %d
                  - Terminées: %d

                === VÉHICULES DANS SES RÉPARATIONS ===
                %s

                === RÉPARATIONS EN COURS / PLANIFIÉES ===
                %s

                Réponds aux questions du mécanicien sur ses véhicules, ses réparations et son planning.
                Si on te demande des informations que tu n'as pas, dis-le clairement.
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
