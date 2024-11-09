package com.angrybug.remedi2.Challenge.Repository;

import com.angrybug.remedi2.Challenge.Model.Score;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRepository extends JpaRepository<Score, Integer> {
}
