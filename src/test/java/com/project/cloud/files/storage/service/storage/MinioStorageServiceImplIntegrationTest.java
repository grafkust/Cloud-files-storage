package com.project.cloud.files.storage.service.storage;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import io.minio.*;
import io.minio.messages.DeleteObject;
import io.minio.messages.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class MinioStorageServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private MinioStorageServiceImpl storageService;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    private final String TEST_CONTENT = "Test content";
    private final String TEST_PATH = "test-folder/test-file.txt";
    private final byte[] TEST_CONTENT_BYTES = TEST_CONTENT.getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() throws Exception {

        if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            return;
        }

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucket)
                        .recursive(true)
                        .build()
        );

        List<DeleteObject> objects = new ArrayList<>();
        for (Result<Item> result : results) {
            Item item = result.get();
            objects.add(new DeleteObject(item.objectName()));
        }

        if (!objects.isEmpty()) {
            minioClient.removeObjects(
                    RemoveObjectsArgs.builder()
                            .bucket(bucket)
                            .objects(objects)
                            .build()
            );
        }
    }

    @Test
    @DisplayName("Should successfully store content")
    void storeContentTest() throws Exception {

        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT_BYTES);

        storageService.store(inputStream, TEST_PATH);

        assertTrue(storageService.exists(TEST_PATH), "Stored file should exist");
        try (InputStream retrievedContent = storageService.retrieve(TEST_PATH)) {
            byte[] retrievedBytes = retrievedContent.readAllBytes();
            assertArrayEquals(TEST_CONTENT_BYTES, retrievedBytes, "Retrieved content should match stored content");
        }
    }

    @Test
    @DisplayName("Should throw StorageOperationException when store fails")
    void storeContentFailureTest() {

        String invalidPath = "";
        InputStream inputStream = new ByteArrayInputStream(TEST_CONTENT_BYTES);

        assertThrows(StorageOperationException.class,
                () -> storageService.store(inputStream, invalidPath),
                "Should throw StorageOperationException for invalid path"
        );
    }

    @Test
    @DisplayName("Should successfully retrieve content")
    void retrieveContentTest() throws Exception {

        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), TEST_PATH);

        InputStream retrievedContent = storageService.retrieve(TEST_PATH);

        assertNotNull(retrievedContent, "Retrieved content should not be null");
        byte[] retrievedBytes = retrievedContent.readAllBytes();
        assertArrayEquals(TEST_CONTENT_BYTES, retrievedBytes, "Retrieved content should match original");
        retrievedContent.close();
    }

    @Test
    @DisplayName("Should successfully delete content")
    void deleteContentTest(){

        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), TEST_PATH);
        assertTrue(storageService.exists(TEST_PATH), "File should exist before deletion");

        storageService.delete(TEST_PATH);

        assertFalse(storageService.exists(TEST_PATH), "File should not exist after deletion");
    }

    @Test
    @DisplayName("Should check if path exists")
    void existsTest() {

        String nonExistentPath = "non-existent.txt";
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), TEST_PATH);

        assertTrue(storageService.exists(TEST_PATH), "Should return true for existing path");
        assertFalse(storageService.exists(nonExistentPath), "Should return false for non-existent path");
    }

    @Test
    @DisplayName("Should successfully copy content")
    void copyContentTest() throws Exception {

        String destinationPath = "test-folder/copy-file.txt";
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), TEST_PATH);

        storageService.copy(TEST_PATH, destinationPath);

        assertTrue(storageService.exists(TEST_PATH), "Source file should still exist");
        assertTrue(storageService.exists(destinationPath), "Destination file should exist");

        try (InputStream retrievedContent = storageService.retrieve(destinationPath)) {
            byte[] retrievedBytes = retrievedContent.readAllBytes();
            assertArrayEquals(TEST_CONTENT_BYTES, retrievedBytes, "Copied content should match original");
        }
    }

    @Test
    @DisplayName("Should successfully create directory")
    void createDirectoryTest() {

        String directoryPath = "test-directory/";

        storageService.createDirectory(directoryPath);

        assertTrue(storageService.exists(directoryPath), "Directory should exist");

        // В MinIO пустая директория представлена как объект с размером 0
        List<StorageItem> items = storageService.list(directoryPath, false);
        assertEquals(1, items.size(), "Directory should be represented as a single empty object");
        assertFalse(items.getFirst().isFile(), "Item should be a directory");
        assertEquals(directoryPath, items.getFirst().getPath(), "Path should match created directory");
    }

    @Test
    @DisplayName("Should list items with common prefixes")
    void listWithCommonPrefixesTest() {

        String basePath = "test-folder/";
        String subFolder = basePath + "subfolder/";
        String file1 = basePath + "file1.txt";
        String file2 = subFolder + "file2.txt";

        storageService.createDirectory(basePath);
        storageService.createDirectory(subFolder);
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), file1);
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), file2);

        List<StorageItem> items = storageService.listWithCommonPrefixes(basePath);

        assertFalse(items.isEmpty(), "Should return items");
        assertTrue(items.stream().anyMatch(item -> item.getPath().equals(file1)), "Should contain file1");
        assertTrue(items.stream().anyMatch(item -> item.getPath().equals(subFolder)), "Should contain subfolder");
        assertTrue(items.stream().anyMatch(item -> item.getPath().equals(file2)), "Should contain file2");
    }

    @Test
    @DisplayName("Should successfully list storage items")
    void listItemsTest()  {

        String basePath = "test-list-folder/";
        String file1 = "test-list-folder/file1.txt";
        String file2 = "test-list-folder/file2.txt";

        storageService.createDirectory(basePath);
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), file1);
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), file2);

        List<StorageItem> items = storageService.list(basePath, false);

        List<StorageItem> filteredItems = items.stream()
                .filter(StorageItem::isFile)
                .filter(item -> item.getPath().startsWith(basePath) &&
                        !item.getPath().substring(basePath.length()).contains("/"))
                .toList();

        assertEquals(2, filteredItems.size(), "Should return correct number of files in current directory");
        assertTrue(filteredItems.stream().anyMatch(item -> item.getPath().equals(file1)), "Should contain file1");
        assertTrue(filteredItems.stream().anyMatch(item -> item.getPath().equals(file2)), "Should contain file2");
    }

    @Test
    @DisplayName("Should successfully move content")
    void moveContentTest() throws Exception {

        String sourcePath = TEST_PATH;
        String destinationPath = "test-folder/moved-file.txt";
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), sourcePath);

        storageService.move(sourcePath, destinationPath);

        assertFalse(storageService.exists(sourcePath), "Source file should not exist");
        assertTrue(storageService.exists(destinationPath), "Destination file should exist");

        try (InputStream retrievedContent = storageService.retrieve(destinationPath)) {
            byte[] retrievedBytes = retrievedContent.readAllBytes();
            assertArrayEquals(TEST_CONTENT_BYTES, retrievedBytes, "Moved content should match original");
        }
    }

    @Test
    @DisplayName("Should handle errors for all operations")
    void errorHandlingTest() {

        String invalidPath = "";
        InputStream inputStream = new ByteArrayInputStream(new byte[0]);

        // Store operation
        StorageOperationException storeException = assertThrows(StorageOperationException.class,
                () -> storageService.store(inputStream, invalidPath),
                "Store should throw StorageOperationException for invalid path"
        );
        assertTrue(storeException.getMessage().contains("Failed to save object in storage"));

        // Retrieve operation
        StorageOperationException retrieveException = assertThrows(StorageOperationException.class,
                () -> storageService.retrieve(invalidPath),
                "Retrieve should throw StorageOperationException for invalid path"
        );
        assertTrue(retrieveException.getMessage().contains("Failed to get object from storage"));

        // Copy operation
        StorageOperationException copyException = assertThrows(StorageOperationException.class,
                () -> storageService.copy(invalidPath, TEST_PATH),
                "Copy should throw StorageOperationException for invalid source path"
        );
        assertTrue(copyException.getMessage().contains("Failed to copy object from storage"));

        // Move operation
        StorageOperationException moveException = assertThrows(StorageOperationException.class,
                () -> storageService.move(invalidPath, TEST_PATH),
                "Move should throw StorageOperationException for invalid source path"
        );
        assertTrue(moveException.getMessage().contains("Failed to move object"));

        // Create directory operation
        StorageOperationException createDirException = assertThrows(StorageOperationException.class,
                () -> storageService.createDirectory(invalidPath),
                "CreateDirectory should throw StorageOperationException for invalid path"
        );
        assertTrue(createDirException.getMessage().contains("Failed to create new directory in storage"));

        // Remove operation
        StorageOperationException exception = assertThrows(StorageOperationException.class,
                () -> storageService.delete(invalidPath),
                "Should throw StorageOperationException when delete fails"
        );
        assertEquals("Failed to remove object from storage", exception.getMessage());
    }

}
