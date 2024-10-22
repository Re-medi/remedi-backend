package com.angrybug.remedi2.Practice;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "practice_result")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PracticeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 기본 키

    @Column(name = "practice_id", nullable = false)
    private Long practiceId; // practice_id를 Long 타입으로 저장

    @Column(name = "user_id", nullable = false)
    private Long userId; // user_id를 Long 타입으로 저장

    @Lob
    @Column(name = "result", nullable = false, columnDefinition = "TEXT") // TEXT 타입으로 result 저장
    private String result; // result는 JSON 문자열로 저장됨
}