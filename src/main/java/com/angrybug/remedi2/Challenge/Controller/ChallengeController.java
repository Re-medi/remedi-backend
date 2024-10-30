package com.angrybug.remedi2.Challenge.Controller;

import com.angrybug.remedi2.Challenge.Service.ChallengeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChallengeController {

    @Autowired
    ChallengeService challengeService;

    //API3. [실전모드] - 질문 + 모범답안 생성
    @PostMapping("/challenge/qna")
    public String getPracticalQnA(@RequestBody String requestBodyStr){
        return challengeService.createConversation(requestBodyStr);
    }
}
