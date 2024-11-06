package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.model.dto.StorageItemDto;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

public interface FileOperationService {

    void uploadFileOrDirectory(MultipartFile file, String path);

    InputStream downloadFileOrDirectory(String path, boolean isFile);


    void createDirectory(String path);

    void deleteFileOrDirectory(String path, String name, boolean isFile);

    List<StorageItemDto> listDirectory(String path, String name);


    void moveFileOrDirectory(String oldPath, String newPath, boolean isFile);

    List<StorageItemDto> searchFileOrDirectory(String rootPath, String query);

    List<StorageItemDto> getContentOfFolder(String path);

    List<StorageItemDto> getPageContent(String rootPath, String path, String query);

    boolean isDirectoryMissing(String path);

    boolean isDirectoryNameTaken(String path, String name);

    void deleteExpiredTrashItems();
}
