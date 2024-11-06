package com.project.cloud.files.storage.service.storage;

import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import io.minio.*;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class MinioStorageServiceImpl implements StorageService {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String mainBucket;

    @Override
    public void store(InputStream content, String path) {
        try (content) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(path)
                    .stream(content, content.available(), -1)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to save object in storage", e);
        }
    }

    @Override
    public InputStream retrieve(String path) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to get object from storage", e);
        }
    }

    @Override
    public void delete(String path) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(mainBucket)
                    .object(path)
                    .build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to remove object from storage", e);
        }
    }

    @Override
    public boolean exists(String path) {
        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(mainBucket)
                        .prefix(path)
                        .maxKeys(1)
                        .build()
        );
        return results.iterator().hasNext();
    }

    @Override
    public List<StorageItem> list(String path, boolean recursive) {

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(mainBucket)
                        .prefix(path)
                        .recursive(recursive)
                        .build());

        return convertToStorageItems(results);
    }

    @Override
    public void copy(String sourcePath, String destinationPath) {
        try {
            minioClient.copyObject(
                    CopyObjectArgs.builder()
                            .bucket(mainBucket)
                            .object(destinationPath)
                            .source(
                                    CopySource.builder()
                                            .bucket(mainBucket)
                                            .object(sourcePath)
                                            .build()
                            ).build());
        } catch (Exception e) {
            throw new StorageOperationException("Failed to copy object from storage", e);
        }
    }

    @Override
    public void move(String sourcePath, String destinationPath) {
        try {
            copy(sourcePath, destinationPath);
            delete(sourcePath);
        } catch (Exception e) {
            throw new StorageOperationException("Failed to move object", e);
        }
    }

    @Override
    public void createDirectory(String path) {
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

    @Override
    public List<StorageItem> listWithCommonPrefixes(String path) {
        try {

            Set<StorageItem> uniqueResults = new LinkedHashSet<>();

            Iterable<Result<Item>> listObjects = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(mainBucket)
                            .prefix(path)
                            .recursive(true)
                            .build()
            );

            for (Result<Item> result : listObjects) {
                Item item = result.get();
                if (item != null) {
                    String objectPath = item.objectName();

                    uniqueResults.add(new StorageItem(item));

                    String parentPath = objectPath;
                    while (parentPath.contains("/")) {
                        parentPath = parentPath.substring(0, parentPath.lastIndexOf('/'));
                        if (!parentPath.isEmpty() && !parentPath.equals(path.replaceAll("/$", ""))) {
                            uniqueResults.add(new StorageItem(parentPath + "/"));
                        }
                    }
                }
            }
            return new ArrayList<>(uniqueResults);

        } catch (Exception e) {
            throw new StorageOperationException("Failed to list objects with prefixes", e);
        }
    }

    private List<StorageItem> convertToStorageItems(Iterable<Result<Item>> results) {

        return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return new StorageItem(result.get());
                    } catch (Exception e) {
                        throw new StorageOperationException("Failed to list objects", e);
                    }
                }).collect(Collectors.toList());
    }


}
