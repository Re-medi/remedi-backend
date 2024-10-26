package com.angrybug.remedi2.DataBackup.Repository;

import com.angrybug.remedi2.DataBackup.Model.Result;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DataBackupRepository extends JpaRepository<Result, Integer> {

}
