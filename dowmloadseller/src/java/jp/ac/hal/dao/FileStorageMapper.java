package jp.ac.hal.dao;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import jp.ac.hal.model.FileStorage;

public interface FileStorageMapper {

	List<FileStorage> selectByRegistPerson(@Param("registPerson") long registPerson, @Param("invalidFlag") String invalidFlag);

	FileStorage selectByPK(@Param("uploadId") long uploadId);

    boolean insertFileStorage(FileStorage fileStorage);
}
