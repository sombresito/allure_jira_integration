package ru.iopump.qa.allure.service;

import com.google.gson.JsonPrimitive;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.ConversationMemberCollectionPage;
import com.microsoft.graph.requests.GraphServiceClient;
import com.azure.identity.ClientSecretCredential;
import ru.iopump.qa.allure.properties.TeamsProperties;
import com.azure.identity.ClientSecretCredentialBuilder;
import com.microsoft.graph.models.Chat;
import com.microsoft.graph.models.ChatType;
import com.microsoft.graph.serializer.AdditionalDataManager;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Arrays;
import java.util.stream.Stream;


import okhttp3.Request;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class MicrosoftTeamsService {

    private final GraphServiceClient<Request> graphServiceClient;
    private final TeamsProperties teamsProperties;

    public MicrosoftTeamsService(
            @Value("${azure.tenant-id}") String tenantId,
            @Value("${azure.client-id}") String clientId,
            @Value("${azure.client-secret}") String clientSecret,
            TeamsProperties teamsProperties
    ) {
        this.teamsProperties = teamsProperties;
        ClientSecretCredential credential = new ClientSecretCredentialBuilder()
                .tenantId(tenantId)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();

        TokenCredentialAuthProvider authProvider =
                new TokenCredentialAuthProvider(credential);

        this.graphServiceClient = GraphServiceClient
                .builder()
                .authenticationProvider(authProvider)
                .buildClient();

    }


    public String createLoadTestingChat(Map<String, String> formData) {
        try {
            // Получаем email ответственных лиц
            List<String> participants = Stream.concat(
                            teamsProperties.getDefaultUsers().stream(),
                            Arrays.stream(formData.get("Ответственные лица").split(","))
                    )
                    .map(String::trim)
                    .distinct()
                    .collect(Collectors.toList());

            // Проверяем, существуют ли пользователи
            List<String> validParticipants = participants.stream()
                    .filter(this::userExists)
                    .collect(Collectors.toList());

            // Если после проверки никого не осталось — не создаём чат
            if (validParticipants.isEmpty()) {
                System.out.println("Нет доступных участников для создания чата.");
                return null;
            }

            // Создаем чат
            Chat chat = new Chat();
            chat.chatType = ChatType.GROUP;
            chat.topic = "Анкета НТ - " + formData.get("Наименование проекта");

            // Формируем список участников
            List<ConversationMember> membersList = validParticipants.stream().map(email -> {
                AadUserConversationMember member = new AadUserConversationMember();
                member.oDataType = "#microsoft.graph.aadUserConversationMember";
                member.roles = Collections.singletonList("owner"); // Можно поменять на "member"

                // Привязываем пользователя
                AdditionalDataManager additionalData = member.additionalDataManager();
                additionalData.put("user@odata.bind", new JsonPrimitive("https://graph.microsoft.com/v1.0/users('" + email + "')"));

                return member;
            }).collect(Collectors.toList());

            // Преобразуем List в ConversationMemberCollectionPage
            chat.members = new ConversationMemberCollectionPage(membersList, null);

            // Создаем чат и получаем его ID
            Chat createdChat = graphServiceClient.chats()
                    .buildRequest()
                    .post(chat);

            if (createdChat.id == null) {
                throw new RuntimeException("Ошибка: Чат не был создан.");
            }

            return createdChat.id;
        } catch (Exception e) {
            System.err.println("Ошибка при создании чата Teams: " + e.getMessage());
            return null;
        }
    }

    // Метод для проверки существования пользователя
    private boolean userExists(String email) {
        try {
            graphServiceClient.users(email)
                    .buildRequest()
                    .get();
            return true;
        } catch (Exception e) {
            System.out.println("Пользователь не найден: " + email);
            return false;
        }
    }
}