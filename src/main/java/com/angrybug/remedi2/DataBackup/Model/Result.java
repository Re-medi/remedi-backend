package com.angrybug.remedi2.DataBackup.Model;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "result")
@Data
@NoArgsConstructor
public class Result {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "practice_id")
    private Integer practiceId;

    @Lob
    @Column(name = "result", nullable = false, columnDefinition = "TEXT")
    private String result;

}
