package com.angrybug.remedi2.DataBackup.DTO;

import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@NoArgsConstructor
public class PracticeResultInputDTO {

    @JsonRawValue
    private String result;


    public void setResult(Map<String, Object> resultMap) throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();
        this.result = mapper.writeValueAsString(resultMap);
    }
}