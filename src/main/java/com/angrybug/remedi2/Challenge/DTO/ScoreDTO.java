package com.angrybug.remedi2.Challenge.DTO;

import java.util.Map;

public class ScoreDTO {

    private Integer id;

    private Map<String, Object> text;


    public ScoreDTO() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Map<String, Object> getText() {
        return text;
    }

    public void setText(Map<String, Object> text) {
        this.text = text;
    }

}
