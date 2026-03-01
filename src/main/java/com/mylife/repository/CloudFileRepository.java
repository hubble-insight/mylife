package com.mylife.repository;

import com.mylife.model.CloudFile;
import com.mylife.model.FileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CloudFileRepository extends JpaRepository<CloudFile, String> {

    List<CloudFile> findByFileTypeOrderByModifiedTimeDesc(FileType fileType);

    List<CloudFile> findTop10ByOrderByModifiedTimeDesc();

    long countByFileType(FileType fileType);

    long count();

    List<CloudFile> findByIsDirFalseOrderByModifiedTimeDesc();
}
