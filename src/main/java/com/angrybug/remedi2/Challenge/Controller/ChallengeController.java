package com.angrybug.remedi2.Challenge.Controller;

import com.angrybug.remedi2.Challenge.DTO.ScoreDTO;
import com.angrybug.remedi2.Challenge.Service.ChallengeService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChallengeController {

    @Autowired
    private ChallengeService challengeService;

    //API3. [실전모드] - 질문 + 모범답안 생성
    @PostMapping("/challenge/qna")
    public String getPracticalQnA(@RequestBody String requestBodyStr){
        return challengeService.createConversation(requestBodyStr);
    }

    //API6. [평가] - 시뮬레이션 총평 생성
    @PostMapping("/challenge/overview")
    public String getOverview(@RequestBody String requestBodyStr){
        return challengeService.createOverview(requestBodyStr);
    }

    //API7. [평가] - 시뮬레이션 총평 생성
    @PostMapping("/challenge/score")
    public String getScore(@RequestBody ScoreDTO scoreDTO) throws Exception {
        return challengeService.createScore(scoreDTO);
    }

}
