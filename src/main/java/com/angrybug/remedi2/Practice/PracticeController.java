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
    public String getUserName() {
        return "Hello";
    }

    @PostMapping("/practice/answer")
    public FeedbackDTO getFeedback(@RequestBody String requestBodyStr){

        return practiceService.createFeedback(requestBodyStr);
    }


}
