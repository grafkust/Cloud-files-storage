package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.model.dto.StorageItemDto;
import com.project.cloud.files.storage.service.file.impl.FileSearchServiceImpl;
import com.project.cloud.files.storage.util.hepler.StorageItemIconManager;
import com.project.cloud.files.storage.util.mapper.StorageItemMapper;
import com.project.cloud.files.storage.util.validator.PathUtil;
import io.minio.*;
import io.minio.messages.Item;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


import static org.junit.jupiter.api.Assertions.*;

public class FileSearchServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileSearchServiceImpl fileSearchService;

    @Autowired
    private MinioClient minioClient;

    @Autowired
    @SuppressWarnings("unused")
    private StorageItemIconManager storageItemIconManager;

    @Autowired
    @SuppressWarnings("unused")
    private StorageItemMapper storageItemMapper;

    @Autowired
    @SuppressWarnings("unused")
    private PathUtil pathUtil;

    @Value("${minio.bucket}")
    private String bucket;

    private static final String TEST_ROOT_PATH = "user-1-files/";
    private static final String TEST_FILE_CONTENT = "test content";

    @BeforeEach
    void setUp() throws Exception {
        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
        }
        clearBucket();
        setupTestFiles();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearBucket();
    }


    @Test
    @DisplayName("Should return list of directories excluding Trash and current directory")
    void listDirectoryTest() {

        List<StorageItemDto> result = fileSearchService.listDirectory(TEST_ROOT_PATH, "folder1");

        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.stream()
                .map(StorageItemDto::getName)
                .collect(Collectors.toSet())
                .containsAll(Set.of("folder2", "test-folder")));
        assertFalse(result.stream()
                .anyMatch(item -> item.getName().equals("Trash") ||
                        item.getName().equals("folder1")));
        assertTrue(result.stream().noneMatch(StorageItemDto::isFile));
    }

    @Test
    @DisplayName("Should return list of items matching search query")
    void searchFileOrDirectoryTest() {

        List<StorageItemDto> result = fileSearchService.searchFileOrDirectory(TEST_ROOT_PATH, "test");

        assertNotNull(result);
        assertEquals(3, result.size());
        assertTrue(result.stream()
                .map(StorageItemDto::getName)
                .allMatch(name -> name.contains("test")));
        assertEquals(2, result.stream()
                .filter(StorageItemDto::isFile)
                .count());
        assertEquals(1, result.stream()
                .filter(item -> !item.isFile())
                .count());
    }

    @Test
    @DisplayName("Should return content of folder excluding current directory and Trash")
    void getContentOfFolderTest() {

        String folderPath = TEST_ROOT_PATH + "test-folder/";
        List<StorageItemDto> result = fileSearchService.getContentOfFolder(folderPath);

        assertNotNull(result);
        assertEquals(4, result.size());
        assertFalse(result.stream()
                .anyMatch(item -> item.getName().equals("Trash")));
        assertTrue(result.stream()
                .map(StorageItemDto::getName)
                .collect(Collectors.toSet())
                .containsAll(Set.of("file1.txt", "file2.pdf", "subfolder")));
    }

    @Test
    @DisplayName("Should return empty list when search query is null or empty")
    void searchFileOrDirectoryEmptyQueryTest() {

        List<StorageItemDto> resultForNull = fileSearchService.searchFileOrDirectory(TEST_ROOT_PATH, null);
        List<StorageItemDto> resultForEmpty = fileSearchService.searchFileOrDirectory(TEST_ROOT_PATH, "");

        assertTrue(resultForNull.isEmpty(), "Result should be empty for null query");
        assertTrue(resultForEmpty.isEmpty(), "Result should be empty for empty query");
    }

    private void clearBucket() throws Exception {
        Iterable<Result<Item>> objects = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .recursive(true)
                        .build()
        );

        for (Result<Item> result : objects) {
            Item item = result.get();
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucket)
                            .object(item.objectName())
                            .build()
            );
        }
    }

    private void setupTestFiles() throws Exception {

        createTestDirectory(TEST_ROOT_PATH + "folder1/");
        createTestDirectory(TEST_ROOT_PATH + "folder2/");
        createTestDirectory(TEST_ROOT_PATH + "Trash/");
        createTestDirectory(TEST_ROOT_PATH + "test-folder/");

        createTestFile(TEST_ROOT_PATH + "test-file1.txt");
        createTestFile(TEST_ROOT_PATH + "test-file2.pdf");
        createTestFile(TEST_ROOT_PATH + "document.txt");

        createTestFile(TEST_ROOT_PATH + "test-folder/file1.txt");
        createTestFile(TEST_ROOT_PATH + "test-folder/file2.pdf");
        createTestDirectory(TEST_ROOT_PATH + "test-folder/subfolder/");
        createTestDirectory(TEST_ROOT_PATH + "test-folder/Trash/");
    }

    private void createTestFile(String path) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .stream(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()),
                                TEST_FILE_CONTENT.length(), -1)
                        .build()
        );
    }

    private void createTestDirectory(String path) throws Exception {
        minioClient.putObject(
                PutObjectArgs.builder()
                        .bucket(bucket)
                        .object(path)
                        .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                        .build()
        );
    }




}
