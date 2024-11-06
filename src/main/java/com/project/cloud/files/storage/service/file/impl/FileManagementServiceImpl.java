package com.project.cloud.files.storage.service.file.impl;

import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.service.file.FileManagementService;
import com.project.cloud.files.storage.service.storage.StorageService;
import com.project.cloud.files.storage.util.validator.PathUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FileManagementServiceImpl implements FileManagementService {

    private final StorageService storageService;
    private final PathUtil pathUtil;

    @Override
    public void moveContent(String sourcePath, String destinationPath, boolean isFile) {
        if (isFile) {
            moveFile(sourcePath, destinationPath);
        } else moveFolder(sourcePath, destinationPath);
    }

    @Override
    public void deleteFile(String path, String name) {
        String file = path + "/" + name;
        storageService.delete(file);
    }

    @Override
    public void deleteDirectory(String path, String name) {
        String folder = path + "/" + name + "/";
        List<StorageItem> list = storageService.list(folder, true);
        for (StorageItem item : list) {
            storageService.delete(item.getPath());
        }
        storageService.delete(folder);
    }

    @Override
    public boolean isDirectoryExists(String path) {
        return storageService.exists(path);
    }

    @Override
    public boolean isNameUnique(String path, String name) {
        path = pathUtil.normalizeStoragePath(path);
        List<StorageItem> items = storageService.list(path, false);
        return items.stream()
                .filter(item -> !item.isFile())
                .map(StorageItem::getName)
                .noneMatch(folderName -> folderName.equals(name));
    }

    @Override
    public void createDirectory(String path) {
        path = pathUtil.normalizeStoragePath(path);
        storageService.createDirectory(path);
    }

    private void moveFile(String sourcePath, String destinationPath) {

        sourcePath = sourcePath.endsWith("/") ? sourcePath.substring(sourcePath.length() - 1) : sourcePath;
        String fileName = sourcePath.substring(sourcePath.lastIndexOf("/") + 1);
        String newPath = destinationPath + fileName;
        storageService.move(sourcePath, newPath);
    }

    private void moveFolder(String sourcePath, String destinationPath) {

        String folderName = sourcePath.substring(sourcePath.lastIndexOf("/", sourcePath.length() - 2) + 1);
        String newFolderPath = destinationPath + folderName;
        storageService.createDirectory(newFolderPath);

        List<StorageItem> results = storageService.list(sourcePath, true);

        for (StorageItem item : results) {
            try {
                String oldItemPath = item.getPath();
                String relativePath = oldItemPath.substring(sourcePath.length());
                String newItemPath = newFolderPath + relativePath;
                storageService.move(oldItemPath, newItemPath);
            } catch (Exception e) {
                throw new StorageOperationException("Failed to move folder in storage", e);
            }
        }
        storageService.delete(sourcePath);
    }


}
