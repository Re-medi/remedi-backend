package com.angrybug.remedi2.DataBackup.Controller;

import com.angrybug.remedi2.DataBackup.DTO.PracticeResultInputDTO;
import com.angrybug.remedi2.DataBackup.Service.DataBackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
public class DataBackupController {

    @Autowired
    DataBackupService dataBackupService;

    //API4.[공통] - 실습 완료 시 결과 저장하기
    @PostMapping("/practice/result")
    public ResponseEntity<?> createResult(@RequestBody PracticeResultInputDTO practiceResultInputDTO){
        try {
            return ResponseEntity.ok(dataBackupService.createResult(practiceResultInputDTO));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

    //API5.[공통] - 실습 정보 가져오기
    @GetMapping("/practice/result/{practice_id}")
    public ResponseEntity<?> readResult(@PathVariable Integer practice_id){
        try {
            return ResponseEntity.ok(dataBackupService.readRResult(practice_id));
        }
        catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("An error occurred: " + e.getMessage());
        }
    }

}
