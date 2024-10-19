package com.angrybug.remedi2.Practice;

import org.springframework.stereotype.Service;

@Service
public class PracticeService {

    //Feedback 생성함수
    public FeedbackDTO createFeedback(String requestBodyStr) {

        //-------------------
        //feedback 생성 로직 수행


        int feedbackCode = 0;
        String feedback = "";
        //-------------------


        return new FeedbackDTO(feedbackCode, feedback);

    }
}
