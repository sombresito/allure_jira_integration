package ru.iopump.qa.allure.gui.view.wiremock;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DockerApi {

    private static final RestTemplate restTemplate = new RestTemplate();
    private static final String dockerApiUrl = "http://172.17.0.1:8079/api/containers/create"; // Адрес твоего Docker API
    private static final String checkPortUrl = "http://172.17.0.1:8079/api/containers/checkport?port=";
    private static final String usedPortsUrl = "http://172.17.0.1:8079/api/containers/used-ports";

    public static String createContainer(TeamConfig requestDto) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<TeamConfig> request = new HttpEntity<>(requestDto, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(dockerApiUrl, request, String.class);
            return response.getBody();
        } catch (HttpClientErrorException | HttpServerErrorException ex) {
            // Покажем ошибку и тело ответа
            return "Ошибка: " + ex.getStatusCode() + " — " + ex.getResponseBodyAsString();
        } catch (Exception ex) {
            return "Ошибка: " + ex.getMessage();
        }
    }
    public static List<String> getUsedPorts() {
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Map<String, List<Integer>>> response = restTemplate.exchange(
                    usedPortsUrl,
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {}
            );

            List<Integer> ports = response.getBody().get("ports");
            if (ports == null) return List.of("Нет портов");

            return ports.stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList());

        } catch (Exception e) {
            e.printStackTrace();
            return List.of("Ошибка запроса");
        }
    }
    public static boolean CheckPortOpen(int port) {
        RestTemplate restTemplate = new RestTemplate();
        try {
            ResponseEntity<Boolean> response = restTemplate.getForEntity(checkPortUrl+port, Boolean.class);
            return Boolean.TRUE.equals(response.getBody());
        } catch (RestClientException e) {
            e.printStackTrace();
            return false;
        }
    }
}
