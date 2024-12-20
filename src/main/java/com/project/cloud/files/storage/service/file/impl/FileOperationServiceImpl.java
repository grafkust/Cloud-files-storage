package com.project.cloud.files.storage.service.file.impl;

import com.project.cloud.files.storage.model.dto.StorageItemDto;
import com.project.cloud.files.storage.service.file.*;
import com.project.cloud.files.storage.service.trash.TrashService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileOperationServiceImpl implements FileOperationService {

    private final TrashService trashService;
    private final FileSearchService searchService;
    private final FileUploadService uploadService;
    private final FileDownloadService downloadService;
    private final FileManagementService managementService;

    @Override
    public List<StorageItemDto> getPageContent(String rootPath, String path, String query) {
        if (query == null || query.isEmpty()) {
            return getContentOfFolder(path);
        } else return searchFileOrDirectory(rootPath, query);
    }

    private List<StorageItemDto> getContentOfFolder(String path) {
        return searchService.getContentOfFolder(path);
    }

    private List<StorageItemDto> searchFileOrDirectory(String rootPath, String query) {
        return searchService.searchFileOrDirectory(rootPath, query);
    }

    @Override
    public void uploadFileOrDirectory(MultipartFile file, String path) {
        uploadService.upload(file, path);
    }

    @Override
    public InputStream downloadFileOrDirectory(String path, boolean isFile) {
        return downloadService.download(path, isFile);
    }

    @Override
    public void createDirectory(String path) {
        managementService.createDirectory(path);
    }

    @Override
    public void deleteFileOrDirectory(String path, String name, boolean isFile) {
        if (isFile) {
            managementService.deleteFile(path, name);
        } else {
            managementService.deleteDirectory(path, name);
        }
    }

    @Override
    public List<StorageItemDto> listDirectory(String path, String name) {
        return searchService.listDirectory(path, name);
    }

    @Override
    public void moveFileOrDirectory(String oldPath, String newPath, boolean isFile) {
        managementService.moveContent(oldPath, newPath, isFile);
    }


    @Override
    public boolean isDirectoryMissing(String path) {
        return !managementService.isDirectoryExists(path);
    }

    @Override
    public boolean isDirectoryNameTaken(String path, String name) {
        return !managementService.isNameUnique(path, name);
    }


    @Scheduled(cron = "0 0 0/12 * * *")
    @Override
    public void deleteExpiredTrashItems() {
        trashService.removeExpiredTrashItems();
    }
}
