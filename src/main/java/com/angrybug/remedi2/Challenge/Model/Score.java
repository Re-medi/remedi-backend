package com.angrybug.remedi2.Challenge.Model;


import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "score")
@Data
@NoArgsConstructor
public class Score {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "score_id")
    private Integer scoreId;

    @Column(name = "user_id", nullable = false, unique = true, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Column(name = "score_detail", nullable = false, columnDefinition = "TEXT")
    private String scoreDetail;



}
