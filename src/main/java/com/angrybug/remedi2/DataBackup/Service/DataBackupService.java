package com.angrybug.remedi2.DataBackup.Service;

import com.angrybug.remedi2.DataBackup.DTO.PracticeResultInputDTO;
import com.angrybug.remedi2.DataBackup.DTO.PracticeResultOutputDTO;
import com.angrybug.remedi2.DataBackup.Model.Result;
import com.angrybug.remedi2.DataBackup.Repository.DataBackupRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import java.util.Optional;

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
}
