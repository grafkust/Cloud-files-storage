package com.project.cloud.files.storage.service;

import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.service.content_operation.*;
import com.project.cloud.files.storage.util.PathUtil;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    //TODO: Implement logging for this class

    private final MinioOperation minioOperation;

    private final LoadContentService loadContentService;
    private final DeleteContentService deleteContentService;
    private final MoveContentService moveContentService;
    private final GetContentService getContentService;

    private final PathUtil pathUtil;

    @Value("${minio.bucket}")
    private String MAIN_BUCKET;

    public List<ContentDto> getListFilesInFolder(String path) throws Exception {
        return getContentService.getListFilesInFolder(path);
    }

    @SneakyThrows
    public List<ContentDto> getListDirectories(String path, String name) {
        return getContentService.getListDirectories(path, name);
    }

    public List<ContentDto> searchFiles(String userRootPath, String searchName) throws Exception {
        return getContentService.searchFiles(userRootPath, searchName);
    }

    @SneakyThrows
    public void createFolder(String path) {
        path = pathUtil.correctPath(path);
        minioOperation.createDirectory(MAIN_BUCKET, path);
    }

    public void uploadContent(MultipartFile file, String path) throws Exception {
        loadContentService.upload(file, path);
    }

    public void deleteContent(String path, String name, boolean isFile) {
        if (isFile) {
            deleteContentService.deleteFile(path, name);
        } else deleteContentService.deleteFolder(path, name);
    }

    @SneakyThrows
    public void moveContent(String oldPath, String destinationPath, boolean isFile) {
        destinationPath = pathUtil.correctPath(destinationPath);
        if (isFile) {
            moveContentService.moveFile(oldPath, destinationPath);
        } else moveContentService.moveFolder(oldPath, destinationPath);
    }

    @SneakyThrows
    public InputStream downloadContent(String path, boolean isFile) {
        return isFile ? loadContentService.downloadFile(path) : loadContentService.downloadFolder(path);
    }

    @Scheduled(cron = "0 * * * * *")
    @SneakyThrows
    private void removeExpiredTrashItems() {
        deleteContentService.removeExpiredTrashItems();
    }

    public boolean isFolderNameUnique(String path, String name) throws Exception {
        List<ContentDto> listFilesInFolder = getListFilesInFolder(path);
        for (ContentDto item : listFilesInFolder) {
            if (item.isFile())
                continue;

            String folderName = item.getName();
            if (folderName.equals(name))
                return false;
        }
        return true;
    }

    public boolean directoryDoesNotExist(String directoryPath) {
        directoryPath = pathUtil.correctPath(directoryPath);
        return minioOperation.directoryDoesNotExist(MAIN_BUCKET, directoryPath);
    }


}











