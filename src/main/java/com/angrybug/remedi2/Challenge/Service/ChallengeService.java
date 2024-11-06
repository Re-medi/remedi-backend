package com.angrybug.remedi2.Challenge.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class ChallengeService {

    @Value("${openai.api.key}")
    private String openaiApiKey;

    private WebClient webClient;

    @PostConstruct
    private void init() {
        this.webClient = WebClient.builder()
                .baseUrl("https://api.openai.com/v1/chat/completions")
                .defaultHeader("Authorization", "Bearer " + openaiApiKey)
                .build();
    }

    ObjectMapper objectMapper = new ObjectMapper();

    //API3. 질문 + 모범답안 생성 (실전 모드)
    public String createConversation(String requestBodyStr) {

        if(requestBodyStr == null){
            return "Error occurred during request.";
        }

        String prompt = "Based on the following JSON data, generate a follow-up question from the patient and the ideal pharmacist's answer in Korean. The JSON includes the previous dialogue, patient's information, character type, and prescription details.\n\n" +
                "Patient character types are as follows:\n" +
                "1: '과도한 요구형' - Makes abnormal requests regarding medications or disregards the doctor’s prescription in favor of their preferred medications.\n" +
                "2: '무리한 할인 요구형' - Requests repeated discounts on medications or complains frequently about high costs.\n" +
                "3: '의료 정보 전문가형' - Asserts extensive knowledge about their illness or medications based on information from sources like the internet.\n\n" +
                "The format should be:\n" +
                "{\n" +
                "  \"question\": \"환자의 질문\",\n" +
                "  \"ideal_answer\": \"약사의 모범답안\"\n" +
                "}\n\n" +
                "Use natural, professional language in Korean. Ensure the patient's question realistically reflects their character type and previous conversation, while the pharmacist's answer concise, no longer than 2 sentences, focusing directly on the patient's concern.\n\n" +
                "Here is the JSON data for this session:\n" + requestBodyStr;

        // message 생성
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt); // 프롬프트 추가

        // 전체 요청 생성
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "gpt-4");
        requestBody.set("messages", objectMapper.createArrayNode().add(message));

        try {
            // WebClient 요청
            Mono<JsonNode> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)  // 직렬화된 JSON 데이터 전송
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnError(WebClientResponseException.class, e -> {
                        // 상태 코드와 응답 본문 로그 출력
                        System.err.println("Status Code: " + e.getStatusCode());
                        System.err.println("Response Body: " + e.getResponseBodyAsString());
                    })
                    .onErrorReturn(JsonNodeFactory.instance.objectNode().put("error", "Error occurred during request."));

            return response.block().path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.";
        }
    }

    public String createOverview(String requestBodyStr) {

        if(requestBodyStr == null){
            return "Error occurred during request.";
        }

        String prompt = "Please generate an overall evaluation based on the provided data, which includes the patient’s information, prescription details, and the full conversation record between the pharmacist and the patient. The conversation contains the pharmacist’s instructions and responses to the patient’s questions. Assess the pharmacist’s clarity and responsiveness, noting strengths and areas for improvement in guiding the patient on medication use. Take into account how well the guidance was tailored to the patient’s age, gender, diagnosis, personality traits, and specific prescription details.\n"
                + "Identify parts where the pharmacist communicated effectively and any areas where additional clarification might benefit the patient. The evaluation should be written as a single paragraph in natural, professional Korean, addressing the pharmacist as ‘약사님’. Return the feedback in the following format:"
                + "{\n"
                + "\"overview\" : \"<insert your evaluation here>\"\n"
                + "}\n"
                + "The conversation record is as follows:\n" +requestBodyStr;

        // message 생성
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", prompt); // 프롬프트 추가

        // 전체 요청 생성
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "gpt-4");
        requestBody.set("messages", objectMapper.createArrayNode().add(message));

        try {
            // WebClient 요청
            Mono<JsonNode> response = webClient.post()
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)  // 직렬화된 JSON 데이터 전송
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .doOnError(WebClientResponseException.class, e -> {
                        // 상태 코드와 응답 본문 로그 출력
                        System.err.println("Status Code: " + e.getStatusCode());
                        System.err.println("Response Body: " + e.getResponseBodyAsString());
                    })
                    .onErrorReturn(JsonNodeFactory.instance.objectNode().put("error", "Error occurred during request."));

            return response.block().path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.";
        }
    }
}
