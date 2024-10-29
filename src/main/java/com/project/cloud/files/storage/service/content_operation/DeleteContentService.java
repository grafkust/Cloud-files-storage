package com.project.cloud.files.storage.service.content_operation;

import io.minio.Result;
import io.minio.errors.MinioException;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DeleteContentService {

    private final MinioOperation minioOperation;

    @Value("${minio.bucket}")
    private String MAIN_BUCKET;

    @Value("${application.date-pattern}")
    private String DATE_TIME_FORMAT_PATTERN;

    @Value("${application.trash.retention-days}")
    private Integer TRASH_RETENTION_DAYS;

    @SneakyThrows
    public void deleteFile(String path, String name) {
        String file = path + "/" + name;
        minioOperation.remove(MAIN_BUCKET, file);
    }

    @SneakyThrows
    public void deleteFolder(String path, String name) {
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

    public void removeExpiredTrashItems() {
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
        LocalDateTime lastModifiedDate = LocalDateTime.parse(
                minioOperation.getLastModifiedDate(MAIN_BUCKET, trashItemPath), formatter);
        return lastModifiedDate.isBefore(cleanupThreshold);
    }


}
