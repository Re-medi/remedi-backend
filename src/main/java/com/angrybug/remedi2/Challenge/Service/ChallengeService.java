package com.angrybug.remedi2.Challenge.Service;

import com.angrybug.remedi2.Challenge.DTO.ScoreDTO;
import com.angrybug.remedi2.Challenge.Model.Score;
import com.angrybug.remedi2.Challenge.Repository.ChallengeRepository;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.IOException;

@Slf4j
@Service
public class ChallengeService {

    @Autowired
    private ChallengeRepository challengeRepository;

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

        if (requestBodyStr == null) {
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

    public String createScore(ScoreDTO scoreDTO) throws Exception {

        UUID userId = scoreDTO.getId();
        Map<String, Object> requestBodyStr = scoreDTO.getText();

        //---------------------------------

        if (requestBodyStr == null) {
            return "Error occurred during request.";
        }

        String prompt = "You are tasked with evaluating a pharmacist's medication guidance simulation. "
                + "The data is provided in JSON format, containing the patient's information and the conversation between the pharmacist and the patient. "
                + "Based on this data, evaluate the quality and accuracy of the medication guidance. "
                + "The pharmacist's utterances are measured for timing, but note that these represent only the actual user's utterances as a pharmacist, "
                + "not the entire conversation. This is because the user evaluates the real pharmacist's performance in guiding the virtual patient.\n\n"
                + "### Important Instructions\n"
                + "1. Your evaluation should focus only on the utterances labeled as 'pharmacist' in the input data.\n"
                + "2. Return the evaluation strictly in JSON format. Do not include any explanations, interpretations, commentary, or additional text outside of the JSON.\n\n"
                + "### Evaluation Procedure\n"
                + "1. Parse the provided JSON data to extract patient information and the conversation.\n"
                + "2. Evaluate each part of the pharmacist's utterances according to the given criteria and assign scores.\n\n"
                + "### Evaluation Criteria\n"
                + "1. **Part Scores**: The conversation is divided into four parts, and each part is scored based on performance.\n"
                + "   - **Part 1**: Did the pharmacist verify the patient’s name and confirm their personal details? (15 points)\n"
                + "   - **Part 2**: Did the pharmacist explain the medications, including their appearance and how they relate to the patient’s condition? (15 points)\n"
                + "   - **Part 3**: Did the pharmacist explain how the patient should take the medications, including dosage frequency, method, and duration? (15 points)\n"
                + "   - **Part 4**: Did the pharmacist describe the potential side effects of the medications and how to manage them if they occur? (15 points)\n\n"
                + "2. **Additional Criteria**: Evaluate based on the following criteria:\n"
                + "   - **Clarity**: Was the information clearly and precisely delivered in a way the patient could easily understand? (10 points)\n"
                + "   - **Relevance**: Was the information appropriate to the patient’s condition and needs? Did the pharmacist avoid unnecessary details? (10 points)\n"
                + "   - **Consistency**: Was the terminology and phrasing consistent throughout the guidance? (10 points)\n"
                + "   - **Delivery**: Was the delivery speed appropriate, neither too fast nor too slow, based on the timing data provided? (10 points)\n\n"
                + "### Output Format\n"
                + "Return the evaluation result strictly in the following JSON format:\n"
                + "{\n"
                + "    \"score\": {\n"
                + "        \"part_scores\": {\n"
                + "            \"part1\": [score],\n"
                + "            \"part2\": [score],\n"
                + "            \"part3\": [score],\n"
                + "            \"part4\": [score]\n"
                + "        },\n"
                + "        \"criteria\": {\n"
                + "            \"clarity\": [score],\n"
                + "            \"relevance\": [score],\n"
                + "            \"consistency\": [score],\n"
                + "            \"delivery\": [score]\n"
                + "        }\n"
                + "    }\n"
                + "}\n\n"
                + "Important: Ensure that the output contains only the JSON object above. Do not include any additional text, comments, or explanations outside of the JSON.\n\n"
                + "Here is the input data:\n"
                + requestBodyStr;

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


            //----------------------------------------
            log.info(String.valueOf(response));
            log.info(String.valueOf(response.block()));
            //----------------------------------------



            String text = response.block().path("choices").get(0).path("message").path("content").asText();

            //-------------------
            // score 결과 저장 로직

            Score score = new Score();

            score.setUserId(userId);
            score.setScoreDetail(text);

            Score savedScore = challengeRepository.save(score); //DB에 저장
            saveLocalScoreDetail(savedScore); //local 파일에 저장

            //---------------------

            return text;

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.";
        }
    }

    public String createOverview(String requestBodyStr) {

        if (requestBodyStr == null) {
            return "Error occurred during request.";
        }

        String prompt = "Please generate an overall evaluation based on the provided data, which includes the patient’s information, prescription details, conversation record, and performance scores. The scores reflect the user's simulated interaction in a medication guidance session with the virtual patient.\n"
                + "Assess the pharmacist’s performance using the following evaluation criteria:\n"
                + "- Part Scores: Evaluate each of the four parts based on the provided scores:\n"
                + "  * Part 1: Verification of patient’s name and personal details.\n"
                + "  * Part 2: Explanation of medications, their appearance, and relevance to the patient’s condition.\n"
                + "  * Part 3: Instructions for medication intake, including dosage frequency, method, and duration.\n"
                + "  * Part 4: Description of potential side effects and how to manage them.\n"
                + "- Additional Criteria: Assess clarity, relevance, consistency, and delivery based on the scores provided.\n\n"
                + "The overall evaluation should:\n"
                + "1. Highlight the pharmacist’s strengths, noting areas where the guidance was clear and effective.\n"
                + "2. Identify any parts where additional clarification or improvement could benefit the patient.\n"
                + "3. Reflect how well the guidance was tailored to the patient’s specific details, including age, gender, diagnosis, personality traits, and prescription.\n"
                + "4. Be concise and written within 3 sentences, maintaining a natural, professional tone in Korean, addressing the pharmacist as ‘약사님’.\n"
                + "Return the feedback in the following format:\n"
                + "{\n"
                + "\"overview\" : \"<insert your concise evaluation here>\"\n"
                + "}\n\n"
                + "The provided data is as follows:\n"
                + requestBodyStr;

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

            //----------------------------------------
            log.info(String.valueOf(response));
            log.info(String.valueOf(response.block()));
            //----------------------------------------


            return response.block().path("choices").get(0).path("message").path("content").asText();

        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.";
        }
    }

    // 로컬에 Score Detail을 Json으로 저장
    public void saveLocalScoreDetail(Score savedScore) {

        // 데이터를 Map으로 구성하여 JSON 형식으로 변환
        Map<String, Object> data = new HashMap<>();
        data.put("scoreId", savedScore.getScoreId());
        data.put("userId", savedScore.getUserId());
        data.put("scoreDetail", savedScore.getScoreDetail());

        // ObjectMapper를 사용하여 JSON 파일로 저장
        ObjectMapper objectMapper = new ObjectMapper();

        String fileName = "scoreDetail----"+savedScore.getUserId() + "----" + savedScore.getScoreId() + ".json";

        String path = "../../../../../backupData/" + fileName;

        try {
            objectMapper.writeValue(new File(path), data);
            log.info("{}이 성공적으로 저장되었습니다.", fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}



