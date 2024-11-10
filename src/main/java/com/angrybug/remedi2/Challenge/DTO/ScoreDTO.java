package com.angrybug.remedi2.Challenge.DTO;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
import java.util.UUID;


@Getter
@Setter
public class ScoreDTO {

    private UUID id;

    private Map<String, Object> text;
}
