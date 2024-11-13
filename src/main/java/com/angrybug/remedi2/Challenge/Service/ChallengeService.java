package com.angrybug.remedi2.Challenge.Service;

import com.angrybug.remedi2.Challenge.DTO.ScoreDTO;
import com.angrybug.remedi2.Challenge.Model.Score;
import com.angrybug.remedi2.Challenge.Repository.ChallengeRepository;

import com.fasterxml.jackson.core.JsonProcessingException;
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

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
                "Use natural, professional language in Korean. Ensure the patient's question realistically reflects their character type and previous conversation. Consider the conversational flow in the 'conversation' field of the JSON, where 'pharmacist' responses are derived from a simulated medication guidance session converted from voice to text via STT. Reflect the patient's character type in the generated question, aligning it with their behavior traits.\n\n" +
                "The pharmacist's answer should be concise, no longer than 2 sentences, addressing the question directly and professionally, and aligned with the provided context and prescription details.\n\n" +
                "Here is the JSON data for this session:\n" + requestBodyStr;

        // ObjectMapper 사용
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Please ensure that the output is always a valid JSON object. The response must be in strict JSON format.");

        // 사용자 메시지 생성
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt); // 프롬프트 추가

        // 전체 요청 생성
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.set("messages", objectMapper.createArrayNode().add(systemMessage).add(userMessage));
        requestBody.putObject("response_format").put("type", "json_object");

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

    public String createScore(ScoreDTO scoreDTO) throws Exception {

        UUID userId = scoreDTO.getId();
        Map<String, Object> requestBodyStr = scoreDTO.getText();

        //---------------------------------

        if (requestBodyStr == null) {
            return "Error occurred during request.(null)";
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
                + "   - **Part 1: Did the pharmacist verify the patient’s name and confirm their personal details?**\n"
                + "     - **0 Points:** No effort to verify the patient’s identity or personal details.\n"
                + "     - **5 Points:** Only partial verification (e.g., name checked but other details omitted).\n"
                + "     - **10 Points:** Name and details checked but done superficially without ensuring patient understanding.\n"
                + "     - **15 Points:** All relevant details are thoroughly verified, ensuring the patient is actively involved.\n"
                + "   - **Part 2: Did the pharmacist explain the medications, including their appearance and how they relate to the patient’s condition?**\n"
                + "     - **0 Points:** No explanation of medication appearance or relevance to the condition.\n"
                + "     - **5 Points:** Basic details are provided, but key information is missing or unclear.\n"
                + "     - **10 Points:** Most details are explained, but some complexity may cause patient confusion.\n"
                + "     - **15 Points:** Medications are fully explained, with clear relevance to the patient’s condition.\n"
                + "   - **Part 3: Did the pharmacist explain how the patient should take the medications, including dosage frequency, method, and duration?**\n"
                + "     - **0 Points:** No explanation of dosage, timing, or duration.\n"
                + "     - **5 Points:** Partial explanation provided, but crucial details are missing.\n"
                + "     - **10 Points:** Clear instructions are given but lack personalization or clarity for complex regimens.\n"
                + "     - **15 Points:** Comprehensive instructions tailored to the patient’s needs, leaving no ambiguity.\n"
                + "   - **Part 4: Did the pharmacist describe the potential side effects of the medications and how to manage them if they occur?**\n"
                + "     - **0 Points:** No mention of side effects or management strategies.\n"
                + "     - **5 Points:** Basic side effects are mentioned, but details are insufficient or incomplete.\n"
                + "     - **10 Points:** Major side effects and some management strategies are explained but not tailored to the patient.\n"
                + "     - **15 Points:** All relevant side effects and management strategies are clearly explained, focusing on the patient’s specific circumstances.\n\n"
                + "2. **Additional Criteria**: Evaluate based on the following criteria:\n"
                + "   - **Clarity:**\n"
                + "     - **0 Points:** Information is vague and difficult to understand, with unclear instructions.\n"
                + "     - **2 Points:** Information is partially clear but requires patient effort to comprehend.\n"
                + "     - **4 Points:** Basic clarity is present, but some instructions lack detail or simplicity.\n"
                + "     - **6 Points:** Mostly clear, but some instructions may confuse the patient.\n"
                + "     - **8 Points:** Clear and specific, with minor areas for improvement.\n"
                + "     - **10 Points:** Fully clear, detailed, and adapted for easy understanding.\n"
                + "   - **Appropriateness:**\n"
                + "     - **0 Points:** Information is irrelevant or critical details are missing.\n"
                + "     - **2 Points:** Some effort to tailor information, but unnecessary or irrelevant points remain.\n"
                + "     - **4 Points:** Mostly relevant, but some details don’t match the patient’s needs.\n"
                + "     - **6 Points:** Relevant and tailored but includes minor unnecessary details.\n"
                + "     - **8 Points:** Well-aligned with the patient’s needs, avoiding irrelevant information.\n"
                + "     - **10 Points:** Perfectly tailored, delivering all necessary details effectively.\n"
                + "   - **Consistency:**\n"
                + "     - **0 Points:** Terminology and instructions are inconsistent, causing confusion.\n"
                + "     - **2 Points:** Some consistency, but important variations may confuse the patient.\n"
                + "     - **4 Points:** Generally consistent, with minor lapses in terminology or redundancy.\n"
                + "     - **6 Points:** Mostly consistent, but slight variations exist.\n"
                + "     - **8 Points:** Terminology and instructions are consistent with minimal issues.\n"
                + "     - **10 Points:** Fully consistent, eliminating any potential confusion.\n"
                + "   - **Communication:**\n"
                + "     - **0 Points:** Patient questions are ignored or responses are irrelevant.\n"
                + "     - **2 Points:** Some responses are provided, but they are insufficient or unclear.\n"
                + "     - **4 Points:** Basic responses are given, but they lack depth or follow-up.\n"
                + "     - **6 Points:** Appropriate responses, with some effort to confirm understanding.\n"
                + "     - **8 Points:** Effective responses, including follow-up and clarifications as needed.\n"
                + "     - **10 Points:** Thorough, clear responses, proactively confirming and enhancing understanding.\n\n"
                + "### Output Format\n"
                + "Return the evaluation result strictly in the following JSON format:\n"
                + "{\n"
                + "    \"score\": {\n"
                + "        \"part_scores\": {\n"
                + "            \"part1\": Integer between 0 and 15,\n"
                + "            \"part2\": Integer between 0 and 15,\n"
                + "            \"part3\": Integer between 0 and 15,\n"
                + "            \"part4\": Integer between 0 and 15\n"
                + "        },\n"
                + "        \"criteria\": {\n"
                + "            \"clarity\": Integer between 0 and 10,\n"
                + "            \"appropriateness\": Integer between 0 and 10,\n"
                + "            \"consistency\": Integer between 0 and 10,\n"
                + "            \"communication\": Integer between 0 and 10\n"
                + "        }\n"
                + "    }\n"
                + "}\n\n"
                + "Ensure that all scores are integers. Ensure that the output contains only the JSON object above. Do not include any additional text, comments, or explanations outside of the JSON.\n\n"
                + "Here is the input data:\n"
                + requestBodyStr;

        // ObjectMapper 사용
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Please ensure that the output is always a valid JSON object. The response must be in strict JSON format.");

        // 사용자 메시지 생성
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt); // 프롬프트 추가

        // 전체 요청 생성
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.set("messages", objectMapper.createArrayNode().add(systemMessage).add(userMessage));
        requestBody.putObject("response_format").put("type", "json_object");

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
                    .onErrorReturn(JsonNodeFactory.instance.objectNode().put("error", "Error occurred during request.(request)"));


            //----------------------------------------
            log.info(String.valueOf(response));
//            log.info(String.valueOf(response.block()));
            //----------------------------------------

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

                //-------------------
                // score 결과 저장 로직

                Score score = new Score();

                score.setUserId(userId);
                score.setScoreDetail(parsedContentStr);

                Score savedScore = challengeRepository.save(score); //DB에 저장
//                saveLocalScoreDetail(savedScore); //local 파일에 저장

                //---------------------

                // 검증 성공 시 content 반환
                return parsedContentStr;

            } catch (JsonProcessingException e) {
                throw new IllegalArgumentException("Content is not a valid JSON", e);
            }


        } catch (Exception e) {
            e.printStackTrace();
            return "Error occurred during request.(final)";
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

        // ObjectMapper 사용
        ObjectNode systemMessage = objectMapper.createObjectNode();
        systemMessage.put("role", "system");
        systemMessage.put("content", "Please ensure that the output is always a valid JSON object. The response must be in strict JSON format.");

        // 사용자 메시지 생성
        ObjectNode userMessage = objectMapper.createObjectNode();
        userMessage.put("role", "user");
        userMessage.put("content", prompt); // 프롬프트 추가

        // 전체 요청 생성
        ObjectNode requestBody = objectMapper.createObjectNode();
        requestBody.put("model", "gpt-4o-mini");
        requestBody.set("messages", objectMapper.createArrayNode().add(systemMessage).add(userMessage));
        requestBody.putObject("response_format").put("type", "json_object");

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

    // 로컬에 Score Detail을 Json으로 저장
    public void saveLocalScoreDetail(Score savedScore) {

        // 데이터를 Map으로 구성하여 JSON 형식으로 변환
        Map<String, Object> data = new HashMap<>();
        data.put("scoreId", savedScore.getScoreId());
        data.put("userId", savedScore.getUserId());
        data.put("scoreDetail", savedScore.getScoreDetail());

        String fileName = "scoreDetail----" + savedScore.getScoreId() + ".json";

//        String directoryPath = "../../../../../backupData/";
//        String filePath = directoryPath + fileName;
        String filePath = fileName;

        try {
            // 디렉토리가 존재하지 않으면 생성
//            Path dirPath = Paths.get(directoryPath);
//            if (Files.notExists(dirPath)) {
//                Files.createDirectories(dirPath);  // 디렉토리를 먼저 생성
//            }

            // JSON 파일로 저장
            objectMapper.writeValue(new File(filePath), data);
            log.info("{}이 성공적으로 저장되었습니다.", fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String extractTextInBraces(String parsedContentStr) {
        Pattern pattern = Pattern.compile("\\{.*\\}", Pattern.DOTALL); // 중첩 포함, 여러 줄 허용
        Matcher matcher = pattern.matcher(parsedContentStr);

        // 중괄호로 둘러싸인 JSON 전체 반환
        if (matcher.find()) {
            return matcher.group(0);  // 중괄호 포함 전체 반환
        } else {
            throw new IllegalArgumentException("No valid JSON braces found in the content");
        }
    }

}



