package com.angrybug.remedi2.Practice.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    ObjectMapper objectMapper = new ObjectMapper();

    //API1. 연습모드 Feedback 생성함수
    public String createFeedback(String requestBodyStr) {

        if(requestBodyStr == null){
            return "Error occurred during request.";
        }

        // feedback 생성 로직 수행
        // JSON 데이터 자체를 프롬프트에 포함하여 OpenAI에게 전달
        String prompt = "You are given the following JSON that contains information about a simulated medication guidance session. "
                + "Your task is to compare the user's response with the ideal answer and specified part of the guidance process. "
                + "If the user's answer includes the essential points of the guidance, consider it correct even if the wording differs from the ideal answer. "
                + "Only mark the answer as incorrect if it omits critical information or includes serious errors. "
                + "Please note that the 'user_talk' field contains text transcribed through speech-to-text (STT) recognition. "
                + "Because this text is generated from voice input, it may include minor errors such as mispronunciations, incorrect word choices, or grammatical issues. "
                + "When evaluating the response, focus on the meaning and intent rather than penalizing minor inaccuracies, unless they lead to significant misunderstandings or critical errors. "
                + "If the user's answer is correct, return only this JSON: {\"feedback_code\": 0, \"feedback\": \"\"}. "
                + "If the user's answer is incorrect, return only this JSON: {\"feedback_code\": 1, \"feedback\": \"explanation of the mistake in Korean\"}. "
                + "The following are the part-specific guidelines: "
                + "Part 2: Explain the medications, including their appearance and how they relate to the patient's condition.\n"
                + "Part 3: Explain how the patient should take the medication, including the dosage frequency, method of taking, and duration.\n"
                + "Part 4: Describe potential side effects of the medication and how to manage them if they occur.\n"
                + "Part 5: Focus on communication skills. Evaluate how well the user handled the patient's questions. \n"
                + "Please respond in valid JSON format and write the feedback explanation in Korean only, without any additional text."
                + "The feedback should address the user as \"약사님\" and be written in a professional and natural tone, as if given by a senior pharmacist mentoring a junior pharmacist. Avoid overly translated expressions and ensure it reads smoothly in Korean."
                + "The feedback should be clear and concise, limited to 2-3 sentences, focusing on core points without excessive detail or vagueness.";

        if (requestBodyStr.contains("\"ai_question\": \"\"")) {
            prompt += " There is no specific question from the patient. Evaluate the user's response based on the part-specific guidance. ";
        } else {
            String aiQuestion = extractAiQuestionFromJson(requestBodyStr);
            prompt += " The question is: \"" + aiQuestion + "\". Please determine if the user's response correctly answers this question."
                    + "Do not focus on matching the ideal answer precisely, but instead assess if the user responded flexibly and appropriately to the patient's concerns. "
                    + "If the response is adaptable and suitable for the patient's needs, consider it correct.";
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

            String content = response.block().path("choices").get(0).path("message").path("content").asText();

            // content를 JSON으로 파싱 및 검증
            try {

                // 1. 이상한 문자가 json 앞 뒤에 추가되는 것 파싱
                // 2. 개행문자 제거 파싱

                String contentStr = content;
                if(contentStr.charAt(0) != '{') {
                    contentStr = extractTextInBraces(contentStr);
                }
                JsonNode parsedContent = objectMapper.readTree(contentStr);
                String parsedContentStr = parsedContent.toString();


                // 검증 성공 시 content 반환
                return parsedContentStr;

            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Content is not a valid JSON", e);
            }

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.";
        }
    }

    private String extractTextInBraces(String parsedContentStr) {
        Pattern pattern = Pattern.compile("\\{([^}]*)\\}");
        Matcher matcher = pattern.matcher(parsedContentStr);

        // 중괄호 내부 문자열을 찾으면 반환
        if (matcher.find()) {
            return matcher.group(0);  // 중괄호 포함
        } else {
            return null;  // 중괄호가 없을 경우
        }

    }




    //API2. 질문 + 모범답안 생성 (연습모드)
    public String createQuestion(String requestBodyStr) {

        if(requestBodyStr == null){
            return "Error occurred during request.";
        }

        String prompt = "Using the following JSON data with patient information and details about a specific part of a medication guidance session, "
                + "generate a realistic follow-up question that the patient might ask, along with a concise, professional answer. "
                + "Provide only one question-answer pair in this JSON format:\n"
                + "{ \"question\": \"the patient's follow-up question in Korean\", \"ideal_answer\": \"the pharmacist's ideal answer in Korean\" }.\n"
                + "Each question should address a unique concern and should not simply repeat standard instructions. "
                + "In both the question and answer, DO NOT using medication names; when appropriate, use general terms like 'prescription' or 'medication' instead of specific descriptors (e.g., color or shape). "
                + "Keep the answer clear and limited to 2-3 sentences."
                + "Avoid overly translated expressions and ensure it reads smoothly in Korean.";

        if (requestBodyStr.contains("\"part_number\": 3")) {
            prompt += " For Part 3, consider patient concerns such as whether they can take it with other drinks, what to do if they miss a dose, when to take it if their meals are limited to lunch and dinner, or if they can stop taking it once they feel better. ";
        } else if (requestBodyStr.contains("\"part_number\": 4")) {
            prompt += " For Part 4, consider patient concerns about foods/activities to avoid, and suitable health supplements to take with the medication. Keep the tone calm and reassuring.";
        }

        // Append JSON data for context
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

            String question = response.block().path("choices").get(0).path("message").path("content").asText();

            return question;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.";
        }
    }

    private String extractAiQuestionFromJson(String requestBodyStr) {
        int startIndex = requestBodyStr.indexOf("\"ai_question\": \"") + 15;
        int endIndex = requestBodyStr.indexOf("\"", startIndex);
        return requestBodyStr.substring(startIndex, endIndex);
    }

}
