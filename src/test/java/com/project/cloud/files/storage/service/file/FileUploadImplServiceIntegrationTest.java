package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.exception.FileUploadException;
import com.project.cloud.files.storage.service.file.impl.FileUploadServiceImpl;
import com.project.cloud.files.storage.util.validator.PathUtil;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
public class FileUploadImplServiceIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private FileUploadServiceImpl fileUploadService;

    @Autowired
    private MinioClient minioClient;

    @SpyBean
    private PathUtil pathUtil;

    @Value("${minio.bucket}")
    private String bucket;

    @BeforeEach
    void setUp() throws Exception {
        if (!minioClient.bucketExists(io.minio.BucketExistsArgs.builder().bucket(bucket).build())) {
            minioClient.makeBucket(io.minio.MakeBucketArgs.builder().bucket(bucket).build());
        }
    }

    @Test
    @DisplayName("Should successfully upload file to MinIO")
    void uploadFileTest() throws Exception {

        String content = "test content";
        MockMultipartFile testFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8)
        );
        String uploadPath = "user-1-files/documents/";

        fileUploadService.upload(testFile, uploadPath);

        String fullPath = uploadPath + "test.txt";
        InputStream storedFile = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(fullPath)
                        .build()
        );

        String storedContent = new String(storedFile.readAllBytes(), StandardCharsets.UTF_8);
        assertEquals(content, storedContent);
    }

    @Test
    @DisplayName("Should properly handle and wrap unknown exceptions during upload")
    void uploadFileWithExceptionTest() {

        MultipartFile testFile = new MockMultipartFile(
                "test.txt",
                "test.txt",
                "text/plain",
                "test content".getBytes()
        );
        String path = "user-1-files/documents/";

        doThrow(new RuntimeException("Unexpected error"))
                .when(pathUtil)
                .createFullPath(any(), any());

        FileUploadException exception = assertThrows(FileUploadException.class,
                () -> fileUploadService.upload(testFile, path)
        );

        assertTrue(exception.getMessage().contains("Failed to upload file"));
        assertNotNull(exception.getCause());
        assertEquals("Unexpected error", exception.getCause().getMessage());
    }


}





