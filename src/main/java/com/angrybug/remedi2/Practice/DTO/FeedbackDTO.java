package com.angrybug.remedi2.Practice.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FeedbackDTO {

    private int feedbackCode;
    private String feedback;

    public FeedbackDTO(int feedbackCode, String feedback) {
        this.feedbackCode = feedbackCode;
        this.feedback = feedback;
    }
}
