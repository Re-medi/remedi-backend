package com.angrybug.remedi2.Practice;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PracticeController {

    @Autowired
    PracticeService practiceService;

    @GetMapping("/test")
    public String test() {
        return "Hello";
    }

    //API1. [연습모드] - 채팅 피드백
    @PostMapping("/practice/feedback")
    public FeedbackDTO getFeedback(@RequestBody String requestBodyStr){
        return practiceService.createFeedback(requestBodyStr);
    }

    //API2. [연습모드] - 모범 답안 생성
    @PostMapping("/practice/answer")
    public IdealAnswerDTO getIdealAnswer(@RequestBody String requestBodyStr){
        return practiceService.createAnswer(requestBodyStr);
    }

    //API3. [연습모드] - 질문 생성
    @PostMapping("/practice/question")
    public QuestionDTO getQuestion(@RequestBody String requestBodyStr){
        return practiceService.createQuestion(requestBodyStr);
    }

    //API4. [공통] - 실습 완료 시 결과 저장하기
    @PostMapping("/practice/result")
    public String saveResult(@RequestBody PracticeResult practiceResult){
        return practiceService.saveResult(practiceResult);
    }


}
