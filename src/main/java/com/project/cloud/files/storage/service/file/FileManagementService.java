package com.project.cloud.files.storage.service.file;

public interface FileManagementService {

    void moveContent(String sourcePath, String destinationPath, boolean isFile);

    void deleteFile(String path, String name);

    void deleteDirectory(String path, String name);

    boolean isDirectoryExists(String path);

    boolean isNameUnique(String path, String name);

    void createDirectory(String path);


}
