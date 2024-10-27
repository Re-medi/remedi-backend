package com.angrybug.remedi2.Practice.Controller;

import com.angrybug.remedi2.Practice.Service.PracticeService;
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
    public String getFeedback(@RequestBody String requestBodyStr){
        return practiceService.createFeedback(requestBodyStr);
    }

    //API2. [연습모드] - 질문 + 모범답안 생성
    @PostMapping("/practice/qna")
    public String getQuestion(@RequestBody String requestBodyStr){
        return practiceService.createQuestion(requestBodyStr);
    }

}
