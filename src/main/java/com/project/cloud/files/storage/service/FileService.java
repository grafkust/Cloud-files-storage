package com.project.cloud.files.storage.service;

import com.project.cloud.files.storage.exception.DirectoryNotExistsException;
import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.prop.MinioProperties;
import com.project.cloud.files.storage.util.ContentIconUtil;
import com.project.cloud.files.storage.util.DeleteOnCloseFileInputStream;
import io.minio.*;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.http.fileupload.FileUploadException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileService {

    //TODO: Implement logging for this class
    //TODO: Shrink this class

    private final MinioClient minioClient;
    private final MinioProperties minioProperties;
    private final MinioOperation minioOperation;
    private String MAIN_BUCKET;

    @Value("${application.date-pattern}")
    private String DATE_TIME_FORMAT_PATTERN;

    @Value("${application.trash.retention-days}")
    private Integer TRASH_RETENTION_DAYS;

    @PostConstruct
    private void init() {
        MAIN_BUCKET = minioProperties.getBucket();
    }

    public List<ContentDto> getListFilesInFolder(String path) throws Exception {

        List<ContentDto> files = new ArrayList<>();

        String prefix = correctPath(path);

        if (path.endsWith("/"))
            path = path.substring(0, path.length() - 1);

        Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, prefix, false);

        for (Result<Item> result : results) {
            Item item = result.get();
            String objectName = item.objectName();
            boolean isFile = true;

            String lastModifiedDate;
            String fileName = Paths.get(objectName).getFileName().toString();
            String iconPath = ContentIconUtil.getFileIcon(fileName);

            if (fileName.equals(path.substring(path.lastIndexOf("/") + 1)) || fileName.equals("Trash"))
                continue;

            if (objectName.endsWith("/")) {
                lastModifiedDate = "";
                isFile = false;
            } else
                lastModifiedDate = getLastModifiedDate(objectName);

            if (!fileName.isEmpty()) {
                files.add(new ContentDto(fileName, lastModifiedDate, iconPath, isFile));
            }
        }

        return files;
    }

    public String getLastModifiedDate(String objectName) {
        StatObjectResponse itemStat = minioOperation.getStatObject(MAIN_BUCKET, objectName);
        ZonedDateTime lastModified = itemStat.lastModified().withZoneSameInstant(ZoneId.systemDefault());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN);
        return lastModified.format(formatter);
    }

    public void upload(MultipartFile file, String path) throws IOException {

        path = correctPath(path);

        try {
            createBucketIfNotExist(MAIN_BUCKET);
        } catch (Exception e) {
            throw new FileUploadException("Create root bucket is impossible: " + e.getMessage());
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

        path = correctPath(path);

        minioClient.putObject(PutObjectArgs.builder()
                .bucket(MAIN_BUCKET)
                .object(path)
                .stream(new ByteArrayInputStream(new byte[]{}), 0, -1)
                .build());
    }

    public boolean isFolderNameUnique(String path, String name) throws Exception {
        List<ContentDto> listFilesInFolder = getListFilesInFolder(path);
        for (ContentDto item : listFilesInFolder) {
            if (item.isFile())
                continue;

            String folderName = item.getName();
            if (folderName.equals(name))
                return false;
        }
        return true;
    }

    @SneakyThrows
    private void createBucketIfNotExist(String bucket) {

        boolean bucketExist = minioOperation.isBucketExists(bucket);

        if (!bucketExist)
            minioOperation.makeBucket(bucket);
    }

    @SneakyThrows
    private void saveFile(InputStream inputStream, String fileName) {
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(MAIN_BUCKET)
                .object(fileName)
                .stream(inputStream, inputStream.available(), -1)
                .build());
    }

    private String generateFileName(MultipartFile file, String path) {
        String fileName = file.getOriginalFilename();
        return path + fileName;
    }

    public void deleteContent(String path, String name, boolean isFile) {
        if (isFile) {
            deleteFile(path, name);
        } else deleteFolder(path, name);
    }

    @SneakyThrows
    private void deleteFile(String path, String name) {
        String file = path + "/" + name;
        minioOperation.remove(MAIN_BUCKET, file);
    }

    @SneakyThrows
    private void deleteFolder(String path, String name) {
        String folder = path + "/" + name + "/";
        try {
            Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, folder, true);

            for (Result<Item> result : results) {
                String objectName = result.get().objectName();
                minioOperation.remove(MAIN_BUCKET, objectName);
            }
        } catch (MinioException e) {
            System.err.println("Ошибка при удалении папки: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Общая ошибка: " + e.getMessage());
        }
    }

    public List<ContentDto> searchFiles(String userRootPath, String searchName) throws Exception {

        List<ContentDto> searchResults = new ArrayList<>();

        try {
            Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, userRootPath, true);

            for (Result<Item> result : results) {

                Item item = result.get();
                String objectName = item.objectName();
                String fileName = Paths.get(objectName).getFileName().toString();
                boolean isFile = !objectName.endsWith("/");
                String lastModifiedDate, iconPath;


                if (fileName.toLowerCase().contains(searchName.toLowerCase())) {

                    if (fileName.equals(userRootPath))
                        continue;

                    if (isFile) {
                        lastModifiedDate = getLastModifiedDate(objectName);
                        iconPath = ContentIconUtil.getFileIcon(fileName);
                    } else {
                        lastModifiedDate = "";
                        iconPath = "/icon/folder.png";
                    }
                    String path = objectName.endsWith("/") ? objectName.substring(0, objectName.length() - 1)
                            : objectName;

                    searchResults.add(new ContentDto(fileName, lastModifiedDate, iconPath, false, path));
                }
            }
        } catch (Exception e) {
            throw new Exception("Error searching files: " + e.getMessage());
        }
        return searchResults;
    }

    public boolean isDirectoryExists(String directoryPath) {

        directoryPath = correctPath(directoryPath);
        try {
            Iterable<Result<Item>> results = minioClient.listObjects(
                    ListObjectsArgs.builder()
                            .bucket(MAIN_BUCKET)
                            .prefix(directoryPath)
                            .maxKeys(1)
                            .build()
            );
            return results.iterator().hasNext();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @SneakyThrows
    public void moveContent(String oldPath, String destinationPath, boolean isFile) {
        if (isFile) {
            moveFile(oldPath, destinationPath);
        } else moveFolder(oldPath, destinationPath);
    }

    @SneakyThrows
    private void moveFile(String oldPath, String destinationPath) {

        boolean exist = isDirectoryExists(destinationPath);
        if (!exist) {
            throw new DirectoryNotExistsException("папки, в которую вы хотите переместить файл не существует. Создайте ее.");
        }

        String fileName = oldPath.substring(oldPath.lastIndexOf("/") + 1);
        String newPath = destinationPath + "/" + fileName;

        minioOperation.copy(MAIN_BUCKET, newPath, oldPath);
        minioOperation.remove(MAIN_BUCKET, oldPath);
    }

    @SneakyThrows
    private void moveFolder(String oldPath, String destinationPath) {

        String folderName = oldPath.substring(oldPath.lastIndexOf("/", oldPath.length() - 2) + 1);
        String newFolderPath = destinationPath + folderName;
        createFolder(newFolderPath);

        Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, oldPath, true);

        for (Result<Item> result : results) {
            Item item = result.get();
            String oldItemPath = item.objectName();
            String relativePath = oldItemPath.substring(oldPath.length());
            String newItemPath = newFolderPath + relativePath;

            minioOperation.copy(MAIN_BUCKET, newItemPath, oldItemPath);
            minioOperation.remove(MAIN_BUCKET, oldItemPath);
        }
        minioOperation.remove(MAIN_BUCKET, oldPath);
    }

    private String correctPath(String path) {
        return path.endsWith("/") ? path : path + "/";
    }

    @SneakyThrows
    public InputStream downloadContent(String path, boolean isFile) {
        return isFile ? downloadFile(path) : downloadFolder(path);
    }

    @SneakyThrows
    private InputStream downloadFile(String file) {
        return minioOperation.getObject(MAIN_BUCKET, file);
    }


    @SneakyThrows
    private InputStream downloadFolder(String folderPath) {
        folderPath = correctPath(folderPath);

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
    }

    @Scheduled(cron = "0 * * * * *")
    @SneakyThrows
    private void removeExpiredTrashItems() {
        Iterable<Result<Item>> usersRootDirectories = minioOperation.getListOfObjects(MAIN_BUCKET, null, false);
        List<String> usersTrashDirectories = getUsersTrashDirectory(usersRootDirectories);
        for (String trashDirectory : usersTrashDirectories) {
            removeExpiredTrashItems(trashDirectory);
        }
    }

    @SneakyThrows
    private List<String> getUsersTrashDirectory(Iterable<Result<Item>> usersRootDirectories) {
        List<String> usersTrashDirectories = new ArrayList<>();
        for (Result<Item> rootDirectory : usersRootDirectories) {
            Item item = rootDirectory.get();
            String rootDirectoryPath = item.objectName();
            String trashDirectoryPath = String.format("%sTrash/", rootDirectoryPath);
            usersTrashDirectories.add(trashDirectoryPath);
        }
        return usersTrashDirectories;
    }

    @SneakyThrows
    private void removeExpiredTrashItems(String trashDirectory) {
        Iterable<Result<Item>> trashContent = minioOperation.getListOfObjects(MAIN_BUCKET, trashDirectory, true);
        for (Result<Item> result : trashContent) {
            Item item = result.get();
            String trashItemPath = item.objectName();

            if (trashItemPath.equals(trashDirectory))
                continue;

            if (isTrashItemExpired(trashItemPath))
                minioOperation.remove(MAIN_BUCKET, trashItemPath);
        }
    }

    private boolean isTrashItemExpired(String trashItemPath) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(DATE_TIME_FORMAT_PATTERN);
        LocalDateTime cleanupThreshold = LocalDateTime.now().minusDays(TRASH_RETENTION_DAYS);
        LocalDateTime lastModifiedDate = LocalDateTime.parse(getLastModifiedDate(trashItemPath), formatter);
        return lastModifiedDate.isBefore(cleanupThreshold);
    }


    @SneakyThrows
    public List<ContentDto> getListDirectories(String path) {

        String prefix = correctPath(path);

        Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, prefix, false);

        List<ContentDto> contentDto = new ArrayList<>();
        for (Result<Item> result : results) {

            Item item = result.get();
            String objectName = item.objectName();
            String folderName = Paths.get(objectName).getFileName().toString();

            if (folderName.equals(path.substring(path.lastIndexOf("/") + 1)))
                folderName = "Файлы";

            if (!objectName.endsWith("/") || objectName.contains("Trash")) {
                continue;
            }

            String iconPath = ContentIconUtil.getFileIcon(folderName);
            if (!folderName.isEmpty())
                contentDto.add(new ContentDto(folderName, iconPath, objectName));
        }
        return contentDto;
    }


}











