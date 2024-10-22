package com.angrybug.remedi2.Practice;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IdealAnswerDTO {

    private String idealAnswer;

    public IdealAnswerDTO(String idealAnswer) {
        this.idealAnswer = idealAnswer;
    }
}
