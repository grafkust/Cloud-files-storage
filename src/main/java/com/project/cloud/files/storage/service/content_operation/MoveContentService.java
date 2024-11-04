package com.project.cloud.files.storage.service.content_operation;

import com.project.cloud.files.storage.exception.StorageOperationException;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class MoveContentService {

    private final MinioOperation minioOperation;

    @Value("${minio.bucket}")
    private String MAIN_BUCKET;

    public void moveFile(String oldPath, String destinationPath) {
        oldPath = oldPath.endsWith("/") ? oldPath.substring(oldPath.length() - 1) : oldPath;

        String fileName = oldPath.substring(oldPath.lastIndexOf("/") + 1);
        String newPath = destinationPath + fileName;

        minioOperation.copy(MAIN_BUCKET, newPath, oldPath);
        minioOperation.remove(MAIN_BUCKET, oldPath);
    }

    public void moveFolder(String oldPath, String destinationPath) {

        String folderName = oldPath.substring(oldPath.lastIndexOf("/", oldPath.length() - 2) + 1);
        String newFolderPath = destinationPath + folderName;
        minioOperation.createDirectory(MAIN_BUCKET, newFolderPath);

        Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, oldPath, true);

        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                String oldItemPath = item.objectName();
                String relativePath = oldItemPath.substring(oldPath.length());
                String newItemPath = newFolderPath + relativePath;

                minioOperation.copy(MAIN_BUCKET, newItemPath, oldItemPath);
                minioOperation.remove(MAIN_BUCKET, oldItemPath);
            } catch (Exception e) {
                throw new StorageOperationException("Failed to move folder in storage", e);
            }
        }
        minioOperation.remove(MAIN_BUCKET, oldPath);
    }


}
