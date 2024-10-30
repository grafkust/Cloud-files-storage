package com.project.cloud.files.storage.service.content_operation;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
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


    @SneakyThrows
    public void remove(String mainBucket, String path) {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(mainBucket)
                .object(path)
                .build());
    }

    public Iterable<Result<Item>> getListOfObjects(String mainBucket, String path, boolean recursive) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(mainBucket)
                        .prefix(path)
                        .recursive(recursive)
                        .build());
    }

    @SneakyThrows
    public InputStream getObject(String mainBucket, String path) {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(mainBucket)
                .object(path)
                .build());
    }

    @SneakyThrows
    public void copy(String mainBucket, String newPath, String oldPath) {
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
    }

    @SneakyThrows
    public StatObjectResponse getStatObject(String mainBucket, String path) {
        return minioClient.statObject(StatObjectArgs.builder()
                .bucket(mainBucket)
                .object(path)
                .build());
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


    @SneakyThrows
    public void save(String mainBucket, InputStream inputStream, String fileName) {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(mainBucket)
                .object(fileName)
                .stream(inputStream, inputStream.available(), -1)
                .build());
    }

    public String getLastModifiedDate(String mainBucket, String objectName) {
        StatObjectResponse itemStat = getStatObject(mainBucket, objectName);
        ZonedDateTime lastModified = itemStat.lastModified().withZoneSameInstant(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN);
        return lastModified.format(formatter);
    }

    @SneakyThrows
    public void createDirectory(String mainBucket, String path) {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(mainBucket)
                .object(path)
                .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                .build());
    }


}
