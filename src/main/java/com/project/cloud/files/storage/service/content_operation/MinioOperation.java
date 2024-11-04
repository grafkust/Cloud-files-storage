package com.project.cloud.files.storage.service.content_operation;

import com.project.cloud.files.storage.exception.StorageOperationException;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class MinioOperation {

    private final MinioClient minioClient;

    @Value("${application.date-pattern}")
    private String DATE_TIME_FORMAT_PATTERN;


    public void remove(String mainBucket, String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to remove object from storage", e);
        }
    }

    public Iterable<Result<Item>> getListOfObjects(String mainBucket, String path, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(mainBucket)
                        .prefix(path)
                        .recursive(recursive)
                        .build());
    }


    public InputStream getObject(String mainBucket, String path) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to get object from storage", e);
        }
    }

    public void copy(String mainBucket, String newPath, String oldPath) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(mainBucket)
                            .object(newPath)
                            .source(
                                    CopySource.builder()
                                            .bucket(mainBucket)
                                            .object(oldPath)
                                            .build()
                            ).build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to copy object from storage", e);
        }
    }

    public StatObjectResponse getStatObject(String mainBucket, String path) {
        try {
            return minioClient.statObject(StatObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to get state of object object from storage", e);
        }
    }

    public boolean directoryDoesNotExist(String mainBucket, String path) {

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(mainBucket)
                        .prefix(path)
                        .maxKeys(1)
                        .build()
        );
        return !results.iterator().hasNext();
    }


    public void save(String mainBucket, InputStream inputStream, String fileName) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(fileName)
                    .stream(inputStream, inputStream.available(), -1)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to save object in storage", e);
        }
    }

    public String getLastModifiedDate(String mainBucket, String objectName) {
        StatObjectResponse itemStat = getStatObject(mainBucket, objectName);
        ZonedDateTime lastModified = itemStat.lastModified().withZoneSameInstant(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN);
        return lastModified.format(formatter);
    }


    public void createDirectory(String mainBucket, String path) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(path)
                    .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to create new directory in storage", e);
        }
    }


}
