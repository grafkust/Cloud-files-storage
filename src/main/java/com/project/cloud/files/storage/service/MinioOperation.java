package com.project.cloud.files.storage.service;

import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
public class MinioOperation {

    private final MinioClient minioClient;

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

    @SneakyThrows
    public void makeBucket(String bucket) {
        minioClient.makeBucket(MakeBucketArgs.builder()
                .bucket(bucket)
                .build());
    }

    @SneakyThrows
    public boolean isBucketExists(String bucket) {
        return minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());
    }


}
