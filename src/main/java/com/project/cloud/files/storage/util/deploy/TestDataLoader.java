package com.project.cloud.files.storage.util.deploy;

import com.project.cloud.files.storage.service.file.FileOperationService;
import com.project.cloud.files.storage.service.storage.StorageService;
import com.project.cloud.files.storage.service.user.UserService;
import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class TestDataLoader {

    private final UserService userService;
    private final StorageService storageService;
    private final MinioClient minioClient;
    private final FileOperationService fileOperationService;

    @Value("${minio.bucket}")
    private String mainBucket;

    private final String testDataFolder = "test-data/";

    @PostConstruct
    public void initTestData() {
        try {

            String testUserRootPath = getTestUserRootPath();

            prepareUserTestStorage(testUserRootPath);

            Resource[] testData = getTestData();

            uploadTestData(testData, testUserRootPath);

        } catch (Exception e) {
            log.error("Error during test data load", e);
        }
    }

    private String getTestUserRootPath() {
        Long testUserId = userService.getIdByUsername("User");
        return String.format("user-%s-files", testUserId);
    }

    private Resource[] getTestData() {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        try {
            return resolver.getResources(String.format("classpath*:%s**", testDataFolder));
        } catch (Exception e) {
            log.error("Error to get test data from classpath*:{}**", testDataFolder);
        }
        log.warn("Return empty test resources[]");
        return new Resource[0];
    }

    private void prepareUserTestStorage(String userRootPath) {

        String trashPath = userRootPath + "/Trash";

        if (fileOperationService.isDirectoryMissing(userRootPath)) {
            fileOperationService.createDirectory(userRootPath);
            log.info("Test user storage created successfully: {}", userRootPath);
        } else {
            clearTestUserFolder(userRootPath);
            log.info("Successfully clear the test user storage");
        }

        if (fileOperationService.isDirectoryMissing(trashPath)) {
            fileOperationService.createDirectory(trashPath);
            log.info("Test user trash created successfully: {}", trashPath);
        }
    }

    private void clearTestUserFolder(String testUserRootPath) {
        try {
            Iterable<Result<Item>> items = getListObjects(testUserRootPath);
            for (Result<Item> itemResult : items) {
                Item item = itemResult.get();
                removeItem(item, testUserRootPath);
            }
        } catch (Exception e) {
            log.error("Error during clear test folder", e);
        }
    }

    private Iterable<Result<Item>> getListObjects(String testUserRootPath) {
        return minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(mainBucket)
                        .prefix(testUserRootPath)
                        .recursive(true)
                        .build());
    }

    private void removeItem(Item item, String testUserRootPath) {

        String objectName = item.objectName();
        if (objectName.equals(testUserRootPath + "/") || objectName.equals(testUserRootPath + "/Trash/"))
            return;
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(mainBucket)
                            .object(objectName)
                            .build()
            );
            log.info("Successfully removed object '{}' from test user storage", objectName);

        } catch (Exception e) {
            log.error("Error during remove item from test folder", e);
        }
    }

    // This method allows you to save test data after building a jar file
    private void uploadTestData(Resource[] resources, String testUserRootPath) {
        for (Resource resource : resources) {
            try {
                String resourcePath = resource.getURL().toString();

                if (resourcePath.endsWith("/")) {
                    continue;
                }

                String relativePath = resourcePath.substring(
                        resourcePath.indexOf(testDataFolder) + testDataFolder.length()
                );

                String fullPath = testUserRootPath + "/" + relativePath;

                try (InputStream inputStream = resource.getInputStream()) {
                    storageService.store(inputStream, fullPath);
                    log.info("Successfully store the test file: {}", fullPath);
                }
            } catch (Exception e) {
                log.error("Error during store test file: {}", resource.getDescription(), e);
            }
        }
    }

}
