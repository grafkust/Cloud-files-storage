package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.service.file.impl.FileManagementServiceImpl;
import com.project.cloud.files.storage.service.storage.StorageService;
import com.project.cloud.files.storage.util.validator.PathUtil;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.spy;

@SpringBootTest
@Slf4j
class FileManagementServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileManagementServiceImpl fileManagementService;

    @Autowired
    private PathUtil pathUtil;

    @Autowired
    private StorageService storageService;

    @Autowired
    private MinioClient minioClient;


    @Value("${minio.bucket}")
    private String bucket;

    private final String TEST_ROOT_PATH = "test-user-files/";
    private final String TEST_FILE_CONTENT = "Test file content";
    private final String TEST_FILE_NAME = "test-file.txt";

    @BeforeEach
    void setUp() throws Exception {
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build());
        }

        try {
            List<StorageItem> existingItems = storageService.list(TEST_ROOT_PATH, true);
            for (StorageItem item : existingItems) {
                storageService.delete(item.getPath());
            }
        } catch (Exception e) {
            log.error(e.getMessage());
        }
        storageService.createDirectory(TEST_ROOT_PATH);
    }

    @Test
    @DisplayName("Should move file")
    void moveContentFileTest()  {

        String destinationFolderName = "destination/";
        String sourcePath = TEST_ROOT_PATH + TEST_FILE_NAME;
        String destinationPath = TEST_ROOT_PATH + destinationFolderName;

        storageService.store(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()), sourcePath);
        storageService.createDirectory(destinationPath);

        fileManagementService.moveContent(sourcePath, destinationPath, true);

        assertFalse(storageService.exists(sourcePath), "Source file should not exist");
        assertTrue(storageService.exists(destinationPath + TEST_FILE_NAME), "File should exist in destination");
    }

    @Test
    @DisplayName("Should move folder and its content")
    void moveContentFolderTest(){

        String sourceFolderName = "source-folder/";
        String destinationFolderName = "destination/";

        String sourceFolderPath = TEST_ROOT_PATH + sourceFolderName;
        String destinationPath = TEST_ROOT_PATH + destinationFolderName;
        String testFilePath = sourceFolderPath + TEST_FILE_NAME;

        storageService.createDirectory(sourceFolderPath);
        storageService.store(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()), testFilePath);
        storageService.createDirectory(destinationPath);

        fileManagementService.moveContent(sourceFolderPath, destinationPath, false);

        assertFalse(storageService.exists(sourceFolderPath), "Source folder should not exist");
        assertTrue(storageService.exists(destinationPath + sourceFolderName), "Folder should exist in destination");
        assertTrue(storageService.exists(destinationPath + sourceFolderName + TEST_FILE_NAME), "File should exist in destination folder");
    }

    @Test
    @DisplayName("Should delete file")
    void deleteFileTest(){

        String filePath = TEST_ROOT_PATH + TEST_FILE_NAME;
        storageService.store(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()), filePath);

        fileManagementService.deleteFile(TEST_ROOT_PATH, TEST_FILE_NAME);

        assertFalse(storageService.exists(filePath), "File should be deleted");
    }

    @Test
    @DisplayName("Should delete directory and its contents")
    void deleteDirectoryTest() {

        String folderName = "test-folder";
        String folderPath = TEST_ROOT_PATH + folderName;
        String testFilePath = folderPath + "/" + TEST_FILE_NAME;

        storageService.createDirectory(folderPath + "/");
        storageService.store(new ByteArrayInputStream(TEST_FILE_CONTENT.getBytes()), testFilePath);

        fileManagementService.deleteDirectory(TEST_ROOT_PATH, folderName);

        assertFalse(storageService.exists(folderPath + "/"), "Folder should be deleted");
        assertFalse(storageService.exists(testFilePath), "File in folder should be deleted");
    }

    @Test
    @DisplayName("Should return true when directory exists")
    void isDirectoryExistsTest(){

        String folderPath = TEST_ROOT_PATH + "existing-folder/";
        storageService.createDirectory(folderPath);

        boolean exists = fileManagementService.isDirectoryExists(folderPath);

        assertTrue(exists, "Directory should exist");
    }

    @Test
    @DisplayName("Should return false when directory does not exist")
    void isDirectoryNotExistsTest() {

        String nonExistentPath = TEST_ROOT_PATH + "non-existent-folder/";

        boolean exists = fileManagementService.isDirectoryExists(nonExistentPath);

        assertFalse(exists, "Directory should not exist");
    }

    @Test
    @DisplayName("Should return true when name is unique")
    void isNameUniqueTest(){
        String uniqueName = "unique-folder";

        boolean isUnique = fileManagementService.isNameUnique(TEST_ROOT_PATH, uniqueName);

        assertTrue(isUnique, "Name should be unique");
    }

    @Test
    @DisplayName("Should return false when name is not unique")
    void isNameNotUniqueTest()  {

        String folderPath = TEST_ROOT_PATH;
        String existingName = "existing-folder";
        storageService.createDirectory(folderPath + existingName + "/");

        boolean isUnique = fileManagementService.isNameUnique(folderPath, existingName);

        assertFalse(isUnique, "Name should not be unique");
    }

    @Test
    @DisplayName("Should create directory")
    void createDirectoryTest() {

        String newDirectoryPath = TEST_ROOT_PATH + "new-directory/";

        fileManagementService.createDirectory(newDirectoryPath);

        assertTrue(storageService.exists(newDirectoryPath), "Directory should be created");
    }

    @Test
    @DisplayName("Should throw StorageOperationException when an exception occurs while moving folder")
    void moveFolderExceptionTest() {

        StorageService spyStorageService = spy(storageService);
        doThrow(new RuntimeException("Storage error")).when(spyStorageService).move(any(), any());

        FileManagementServiceImpl serviceWithSpyStorage = new FileManagementServiceImpl(spyStorageService, pathUtil);

        String sourcePath = TEST_ROOT_PATH + "source-folder/";
        String destinationPath = TEST_ROOT_PATH + "destination/";

        storageService.createDirectory(sourcePath);
        storageService.createDirectory(destinationPath);

        assertThrows(StorageOperationException.class, () ->
                        serviceWithSpyStorage.moveContent(sourcePath, destinationPath, false),
                "Should throw StorageOperationException"
        );
    }
}
