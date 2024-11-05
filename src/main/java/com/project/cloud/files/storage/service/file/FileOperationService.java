package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.model.dto.ContentDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface FileOperationService {

    void upload(MultipartFile file, String path);

    InputStream download(String path, boolean isFile);


    void createDirectory(String path);

    void delete(String path, String name, boolean isFile);

    List<ContentDto> listDirectory(String path, String name);


    void moveContent(String oldPath, String newPath, boolean isFile);

    List<ContentDto> searchContent(String rootPath, String query);

    List<ContentDto> getFilesInFolder(String path);


    boolean directoryDoesNotExist(String path);

    boolean folderNameNotUnique(String path, String name);

    void deleteExpiredTrashItems();
}
