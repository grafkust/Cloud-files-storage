package com.project.cloud.files.storage.service;

import com.project.cloud.files.storage.exception.FileWithoutExtensionException;
import com.project.cloud.files.storage.model.entity.file.ContentDto;
import com.project.cloud.files.storage.prop.MinioProperties;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FileService {

    /** Структура файловой системы
     user-files - Для хранения файлов всех пользователей
     user-${id}-files - папка для отдельного пользователя
     Обработак добавление файла с таким же названием
     */

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;


    public List<String> getListFilesInFolder(String path) throws Exception {
        List<String> files = new ArrayList<>();

        String prefix = path.endsWith("/") ? path : path + "/";

        Iterable<Result<Item>> results = minioClient.listObjects(ListObjectsArgs.builder()
                .bucket(minioProperties.getBucket())
                .prefix(prefix)
                .build());

        for (Result<Item> result : results) {
            Item item = result.get();
            String fileName = Paths.get(item.objectName()).getFileName().toString();

            if (!fileName.isEmpty()) {
                files.add(fileName);
            }
        }
        return files;
    }

    private String getLastModifiedDate(String objectName) throws Exception {
        StatObjectResponse itemStat = minioClient.statObject(StatObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(objectName)
                .build());
        ZonedDateTime lastModified = itemStat.lastModified();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
        return  lastModified.format(formatter);
    }

    public void upload(MultipartFile file, String path) throws IOException {

        try {
            createBucket(minioProperties.getBucket());
        } catch (Exception e) {
            throw new FileUploadException("Create root bucket is impossible: " + e.getMessage());
        }

        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new FileUploadException("File can't be empty and must have a name.");
        }

        String fileName = generateFileName(file, path);
        InputStream inputStream;
        try {
            inputStream = file.getInputStream();
        } catch (Exception e) {
            throw new FileUploadException("File upload failed: " + e.getMessage());
        }
        saveFile(inputStream, fileName);
        inputStream.close();
    }

    @SneakyThrows
    public void createFolder(String path) {

        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(path)
                        .stream(new ByteArrayInputStream(new byte[] {}), 0, -1)
                .build());
    }

    @SneakyThrows
    private void createBucket(String bucket) {

        boolean bucketExist = minioClient.bucketExists(BucketExistsArgs.builder()
                .bucket(bucket)
                .build());

        if (!bucketExist)
            minioClient.makeBucket(MakeBucketArgs.builder()
                    .bucket(bucket)
                    .build());
    }

    @SneakyThrows
    private void saveFile(InputStream inputStream, String fileName) {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(fileName)
                .stream(inputStream, inputStream.available(), -1)
                .build());
    }

    private String generateFileName(MultipartFile file, String path) {

        String fileName = file.getOriginalFilename();

        return  path + fileName;
    }

    private String getExtension(MultipartFile file) {
        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && originalFilename.contains(".")) {
            return originalFilename.substring(originalFilename.lastIndexOf(".") + 1);
        } else throw new FileWithoutExtensionException("File must have extension.");
    }

    @SneakyThrows
    public void deleteFile(String file) {
        minioClient.removeObject(RemoveObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(file)
                .build());

    }

    public void deleteFolder(String folder) {
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(minioProperties.getBucket())
                            .prefix(folder)
                            .build()
            );

            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                minioClient.removeObject(RemoveObjectArgs.builder()
                        .bucket(minioProperties.getBucket())
                        .object(objectName)
                        .build());
                System.out.println("Удален объект: " + objectName);
            }
        } catch (MinioException e) {
            System.err.println("Ошибка при удалении папки: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Общая ошибка: " + e.getMessage());
        }
    }

    @SneakyThrows
    public InputStream download(String file) {
        return minioClient.getObject(GetObjectArgs.builder()
                .bucket(minioProperties.getBucket())
                .object(file)
                .build());

    }
}

