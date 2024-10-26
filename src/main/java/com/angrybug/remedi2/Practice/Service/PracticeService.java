package com.angrybug.remedi2.Practice.Service;

import com.angrybug.remedi2.Practice.DTO.IdealAnswerDTO;
import com.angrybug.remedi2.Practice.DTO.QuestionDTO;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class PracticeService {

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

    //API1. 연습모드 Feedback 생성함수
    public String createFeedback(String requestBodyStr) {

        ObjectMapper objectMapper = new ObjectMapper();
        //feedback 생성 로직 수행
        // JSON 데이터 자체를 프롬프트에 포함하여 OpenAI에게 전달
        String prompt = "You are given the following JSON that contains information about a simulated medication guidance session. "
                + "Your task is to compare the user's response with the ideal answer provided for the specified part of the guidance process. "
                + "If the user's answer is correct, return only this JSON: {\"feedback_code\": 0, \"feedback\": \"\"}. "
                + "If the user's answer is incorrect, return only this JSON: {\"feedback_code\": 1, \"feedback\": \"explanation of the mistake in Korean\"}. "
                + "Please respond in valid JSON format and write the feedback explanation in Korean only, without any additional text."
                + "The feedback should address the user as \"약사님\" and be written in a professional and natural tone, as if given by a senior pharmacist mentoring a junior pharmacist. Avoid overly translated expressions and ensure it reads smoothly in Korean."
                + "The feedback should be clear and concise, limited to 2-3 sentences, focusing on core points without excessive detail or vagueness.";

        if (requestBodyStr.contains("\"ai_question\": \"\"")) {
            prompt += " There is no specific question from the AI. Evaluate the user's response based on the part-specific guidance: "
                    + "Part 1: Verify the patient's identity by asking their name and confirming their personal details.\n"
                    + "Part 2: Explain the medications, including their appearance and how they relate to the patient's condition.\n"
                    + "Part 3: Explain how the patient should take the medication, including the dosage frequency, method of taking, and duration.\n"
                    + "Part 4: Describe potential side effects of the medication and how to manage them if they occur.\n";
        } else {
            String aiQuestion = extractAiQuestionFromJson(requestBodyStr);
            prompt += " The question is: \"" + aiQuestion + "\". Please determine if the user's response correctly answers this question.";
        }

        // JSON 데이터 추가
        prompt += " Here is the JSON data for this session:\n" + requestBodyStr;

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

            String feedback = response.block().path("choices").get(0).path("message").path("content").asText();
            return feedback;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.";
        }
    }

    //API2. 연습모드 모범 답안 생성 함수
    public IdealAnswerDTO createAnswer(String requestBodyStr) {
        //-------------------
        //feedback 생성 로직 수행


        String idealAnswer = "";
        //-------------------

        return new IdealAnswerDTO(idealAnswer);
    }

    //API3. 연습모드 질문 생성
    public QuestionDTO createQuestion(String requestBodyStr) {

        //-------------------
        //question 생성 로직 수행
        String question = "";

        //-------------------

        return new QuestionDTO(question);
    }

    private String extractAiQuestionFromJson(String requestBodyStr) {
        int startIndex = requestBodyStr.indexOf("\"ai_question\": \"") + 15;
        int endIndex = requestBodyStr.indexOf("\"", startIndex);
        return requestBodyStr.substring(startIndex, endIndex);
    }

}
