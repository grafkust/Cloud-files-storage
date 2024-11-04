package com.project.cloud.files.storage.service.content_operation;

import com.project.cloud.files.storage.exception.FileUploadException;
import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.util.DeleteOnCloseFileInputStream;
import com.project.cloud.files.storage.util.PathUtil;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class LoadContentService {


    private final MinioOperation minioOperation;
    private final PathUtil pathUtil;

    @Value("${minio.bucket}")
    private String MAIN_BUCKET;


    public InputStream downloadFile(String file) {
        return minioOperation.getObject(MAIN_BUCKET, file);
    }


    public InputStream downloadFolder(String folderPath) {
        try {
            folderPath = pathUtil.correctPath(folderPath);
            Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, folderPath, true);
            File tempZipFile = File.createTempFile("download_", ".zip");

            try (FileOutputStream outputStream = new FileOutputStream(tempZipFile);
                 ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {

                for (Result<Item> result : results) {
                    Item item = result.get();
                    String objectName = item.objectName();

                    if (objectName.equals(folderPath)) {
                        continue;
                    }
                    String relativePath = objectName.substring(folderPath.length());

                    ZipEntry zipEntry = new ZipEntry(relativePath);
                    zipOutputStream.putNextEntry(zipEntry);

                    try (InputStream objectStream = minioOperation.getObject(MAIN_BUCKET, objectName)) {
                        byte[] buffer = new byte[8192];
                        int len;
                        while ((len = objectStream.read(buffer)) > 0) {
                            zipOutputStream.write(buffer, 0, len);
                        }
                    }
                    zipOutputStream.closeEntry();
                }
            }
            return new DeleteOnCloseFileInputStream(tempZipFile);
        } catch (Exception e) {
            throw new StorageOperationException("Failed to create zip archive for folder", e);
        }
    }


    public void upload(MultipartFile file, String path) throws Exception {
        path = pathUtil.correctPath(path);

        String fileName = generateFileName(file, path);
        InputStream inputStream;
        try {
            inputStream = file.getInputStream();
        } catch (Exception e) {
            throw new FileUploadException("File upload failed: " + e.getMessage(), e);
        }
        minioOperation.save(MAIN_BUCKET, inputStream, fileName);
        inputStream.close();
    }

    private String generateFileName(MultipartFile file, String path) {
        String fileName = file.getOriginalFilename();
        return path + fileName;
    }


}
