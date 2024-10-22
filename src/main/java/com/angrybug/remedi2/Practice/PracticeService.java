package com.angrybug.remedi2.Practice;

import org.springframework.stereotype.Service;

@Service
public class PracticeService {

    //API1. 연습모드 Feedback 생성함수
    public FeedbackDTO createFeedback(String requestBodyStr) {

        //-------------------
        //feedback 생성 로직 수행


        int feedbackCode = 0;
        String feedback = "";
        //-------------------


        return new FeedbackDTO(feedbackCode, feedback);

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

    //API4.[공통] - 실습 완료 시 결과 저장하기
    public String saveResult(PracticeResult practiceResult) {

        //-------------------
        //실습 결과 저장 로직 수행

        //-------------------

        return "200 OK";
    }

}
