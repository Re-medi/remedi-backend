package com.angrybug.remedi2.DataBackup.Service;

import com.angrybug.remedi2.Challenge.Model.Score;
import com.angrybug.remedi2.DataBackup.DTO.PracticeResultInputDTO;
import com.angrybug.remedi2.DataBackup.DTO.PracticeResultOutputDTO;
import com.angrybug.remedi2.DataBackup.Model.Result;
import com.angrybug.remedi2.DataBackup.Repository.DataBackupRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
@Service
public class DataBackupService {

    @Autowired
    DataBackupRepository dataBackupRepository;

    //API4.[공통] - 실습 완료 시 결과 저장하기
    public Result createResult(PracticeResultInputDTO practiceResultInputDTO) {
        try{
            Result result = new Result();
            result.setResult(practiceResultInputDTO.getResult());

            return dataBackupRepository.save(result);
        }
        catch(DataAccessException e){
            throw new RuntimeException("Data save failed", e);
        }
    }

    //API5.[공통] - 실습 정보 가져오기
    public Result readRResult(Integer practiceId) {
        try {
            return dataBackupRepository.findById(practiceId).orElseThrow(() -> new RuntimeException("Project not found with id: " + practiceId));
        }
        catch(DataAccessException e){
            throw new RuntimeException("Data read failed", e);
        }
    }



    public void saveLocalScoreDetail(Result savedResult) {

        // 데이터를 Map으로 구성하여 JSON 형식으로 변환
        Map<String, Object> data = new HashMap<>();
        data.put("practiceId", savedResult.getPracticeId());
        data.put("result", savedResult.getResult());


        // ObjectMapper를 사용하여 JSON 파일로 저장
        ObjectMapper objectMapper = new ObjectMapper();

        String fileName = savedResult.getPracticeId() + "----" + savedResult.getResult() + ".json";

        String path = "../../../../../backupData/" + fileName;

        try {
            objectMapper.writeValue(new File(path), data);
            log.info("{}이 성공적으로 저장되었습니다.", fileName);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




}
