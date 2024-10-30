package com.project.cloud.files.storage.service.content_operation;

import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoveContentService {

    private final MinioOperation minioOperation;

    @Value("${minio.bucket}")
    private String MAIN_BUCKET;

    @SneakyThrows
    public void moveFile(String oldPath, String destinationPath) {
        oldPath = oldPath.endsWith("/") ? oldPath.substring(oldPath.length() - 1) : oldPath;

        String fileName = oldPath.substring(oldPath.lastIndexOf("/") + 1);
        String newPath = destinationPath + fileName;

        minioOperation.copy(MAIN_BUCKET, newPath, oldPath);
        minioOperation.remove(MAIN_BUCKET, oldPath);
    }

    @SneakyThrows
    public void moveFolder(String oldPath, String destinationPath) {

        String folderName = oldPath.substring(oldPath.lastIndexOf("/", oldPath.length() - 2) + 1);
        String newFolderPath = destinationPath + folderName;
        minioOperation.createDirectory(MAIN_BUCKET, newFolderPath);

        Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, oldPath, true);

        for (Result<Item> result : results) {
            Item item = result.get();
            String oldItemPath = item.objectName();
            String relativePath = oldItemPath.substring(oldPath.length());
            String newItemPath = newFolderPath + relativePath;

            minioOperation.copy(MAIN_BUCKET, newItemPath, oldItemPath);
            minioOperation.remove(MAIN_BUCKET, oldItemPath);
        }
        minioOperation.remove(MAIN_BUCKET, oldPath);
    }


}
