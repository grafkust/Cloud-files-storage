package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.exception.FileDownloadException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.service.file.impl.FileDownloadServiceImpl;
import com.project.cloud.files.storage.service.storage.StorageService;
import com.project.cloud.files.storage.util.validator.PathUtil;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@Slf4j
class FileDownloadServiceImplIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileDownloadServiceImpl fileDownloadService;

    @Autowired
    @SuppressWarnings("unused")
    private PathUtil pathUtil;

    @SpyBean
    private StorageService storageService;

    @Autowired
    private MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucket;

    private final String TEST_ROOT_PATH = "user-test-files/";
    private final String TEST_FILE_CONTENT = "Test file content";
    private final byte[] TEST_CONTENT_BYTES = TEST_FILE_CONTENT.getBytes(StandardCharsets.UTF_8);

    @BeforeEach
    void setUp() throws Exception {
        boolean bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build());
        if (!bucketExists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
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
    @DisplayName("Should successfully download single file")
    void downloadSingleFileTest() throws IOException {

        String fileName = "test-file.txt";
        String filePath = TEST_ROOT_PATH + fileName;
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), filePath);

        InputStream resultStream = fileDownloadService.download(filePath, true);

        byte[] downloadedContent = resultStream.readAllBytes();
        assertArrayEquals(TEST_CONTENT_BYTES, downloadedContent, "Downloaded content should match original content");
        resultStream.close();
    }

    @Test
    @DisplayName("Should successfully download folder with files")
    void downloadFolderWithFilesTest() throws IOException {

        String folderPath = TEST_ROOT_PATH + "test-folder/";
        String file1Path = folderPath + "file1.txt";
        String file2Path = folderPath + "nested/file2.txt";

        storageService.createDirectory(folderPath);
        storageService.createDirectory(folderPath + "nested/");
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), file1Path);
        storageService.store(new ByteArrayInputStream("Nested file content".getBytes()), file2Path);

        InputStream resultStream = fileDownloadService.download(folderPath, false);

        ZipInputStream zipStream = new ZipInputStream(resultStream);
        assertNotNull(zipStream.getNextEntry(), "Zip should contain at least one entry");

        int entryCount = 0;
        while (zipStream.getNextEntry() != null) {
            entryCount++;
        }
        assertEquals(2, entryCount, "Zip should contain exactly two files");

        zipStream.close();
        resultStream.close();
    }

    @Test
    @DisplayName("Should return empty InputStream when downloading an empty folder")
    void downloadEmptyFolderTest() throws IOException {

        String emptyFolderPath = TEST_ROOT_PATH + "empty-folder/";
        storageService.createDirectory(emptyFolderPath);

        InputStream resultStream = fileDownloadService.download(emptyFolderPath, false);

        ZipInputStream zipStream = new ZipInputStream(resultStream);
        assertNull(zipStream.getNextEntry(), "Zip should not contain any entries");

        zipStream.close();
        resultStream.close();
    }

    @Test
    @DisplayName("Should throw FileDownloadException when exception occurs during zip creation")
    void downloadFolderExceptionTest() {

        String folderPath = TEST_ROOT_PATH + "test-folder/";
        String filePath = folderPath + "test-file.txt";

        storageService.createDirectory(folderPath);
        storageService.store(new ByteArrayInputStream(TEST_CONTENT_BYTES), filePath);

        assertTrue(storageService.exists(folderPath), "Folder should exist before test");
        assertTrue(storageService.exists(filePath), "File should exist before test");

        doThrow(new RuntimeException("Simulated storage error"))
                .when(storageService)
                .retrieve(filePath);

        FileDownloadException exception = assertThrows(FileDownloadException.class,
                () -> fileDownloadService.download(folderPath, false),
                "Should throw FileDownloadException when storage operation fails"
        );

        assertTrue(exception.getMessage().contains("Failed to create zip archive"),
                "Exception message should indicate zip creation failure");
    }
}
