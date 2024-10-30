package com.project.cloud.files.storage.service.content_operation;

import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.util.ContentIconUtil;
import com.project.cloud.files.storage.util.PathUtil;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GetContentService {

    private final MinioOperation minioOperation;
    private final PathUtil pathUtil;

    @Value("${minio.bucket}")
    private String MAIN_BUCKET;


    //Отличия ->
    //getListFilesInFolder ->  boolean itemIsCurrentDir = fileName.equals(path.substring(path.lastIndexOf("/") + 1));
    //                         boolean itemIsTrash = fileName.equals("Trash");

    //getListDirectories   ->  boolean itemIsFile = !objectName.endsWith("/");
    //                         boolean itemIsTrash = folderName.equals("Trash");
    //                         boolean folderIsMovementItem = folderName.equals(name);

    //searchFiles          ->  boolean itemIsRootDirectory = fileName.equals(userRootPath);



    private ContentDto createContentDto(Item item, boolean includeLastModifiedDate, boolean includeFullPath) {

        String objectName = item.objectName();
        String fileName = Paths.get(objectName).getFileName().toString();
        boolean isFile = !objectName.endsWith("/");
        String iconPath = ContentIconUtil.getFileIcon(fileName);

        String lastModifiedDate = "";
        if (includeLastModifiedDate && isFile) {
            lastModifiedDate = minioOperation.getLastModifiedDate(MAIN_BUCKET, objectName);
        }

        String path = "";
        if (includeFullPath) {
            path = objectName.endsWith("/") ? objectName.substring(0, objectName.length() - 1) : objectName;
        }

        return includeFullPath ?
                new ContentDto(fileName, lastModifiedDate, iconPath, isFile, path) :
                new ContentDto(fileName, lastModifiedDate, iconPath, isFile);
    }

    private boolean shouldSkipItem(Item item, String skipName, String path) {
        String fileName = Paths.get(item.objectName()).getFileName().toString();

        if (fileName.equals("Trash")) return true;
        if (skipName != null && fileName.equals(skipName)) return true;

        if (path != null) {
            String pathLastPart = path.substring(path.lastIndexOf("/") + 1);
            return fileName.equals(pathLastPart);
        }

        return false;
    }


    public List<ContentDto> getListFilesInFolder(String path) throws Exception {

        List<ContentDto> files = new ArrayList<>();

        String prefix = pathUtil.correctPath(path);

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

            boolean itemIsCurrentDir = fileName.equals(path.substring(path.lastIndexOf("/") + 1));
            boolean itemIsTrash = fileName.equals("Trash");

            if (itemIsCurrentDir || itemIsTrash)
                continue;

            if (objectName.endsWith("/")) {
                lastModifiedDate = "";
                isFile = false;
            } else
                lastModifiedDate = minioOperation.getLastModifiedDate(MAIN_BUCKET, objectName);

            files.add(new ContentDto(fileName, lastModifiedDate, iconPath, isFile, objectName));
        }

        return files;
    }


    @SneakyThrows
    public List<ContentDto> getListDirectories(String path, String name) {

        String prefix = pathUtil.correctPath(path);

        Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, prefix, false);

        List<ContentDto> contentDto = new ArrayList<>();
        for (Result<Item> result : results) {

            Item item = result.get();
            String objectName = item.objectName();
            String folderName = Paths.get(objectName).getFileName().toString();

            boolean itemIsFile = !objectName.endsWith("/");
            boolean itemIsTrash = folderName.equals("Trash");

            boolean folderIsMovementItem = folderName.equals(name);


            if (itemIsFile || itemIsTrash || folderIsMovementItem ) {
                continue;
            }

            boolean itemIsRootDirectory = folderName.equals(path.substring(path.lastIndexOf("/") + 1));

            if (itemIsRootDirectory)
                folderName = "Файлы";

            String iconPath = ContentIconUtil.getFileIcon(folderName);

            contentDto.add(new ContentDto(folderName, iconPath, objectName));
        }
        return contentDto;
    }

    public List<ContentDto> searchFiles(String userRootPath, String searchName) throws Exception {

        List<ContentDto> searchResults = new ArrayList<>();

        Iterable<Result<Item>> results = minioOperation.getListOfObjects(MAIN_BUCKET, userRootPath, true);

        for (Result<Item> result : results) {

            Item item = result.get();
            String objectName = item.objectName();
            String fileName = Paths.get(objectName).getFileName().toString();
            boolean isFile = !objectName.endsWith("/");
            String lastModifiedDate, iconPath;

            if (fileName.toLowerCase().contains(searchName.toLowerCase())) {

                boolean itemIsRootDirectory = fileName.equals(userRootPath);

                if (itemIsRootDirectory)
                    continue;

                if (isFile) {
                    lastModifiedDate = minioOperation.getLastModifiedDate(MAIN_BUCKET, objectName);
                    iconPath = ContentIconUtil.getFileIcon(fileName);
                } else {
                    lastModifiedDate = "";
                    iconPath = "/icon/folder.png";
                }
                String path = objectName.endsWith("/") ? objectName.substring(0, objectName.length() - 1)
                        : objectName;

                searchResults.add(new ContentDto(fileName, lastModifiedDate, iconPath, isFile, path, true));
            }
        }

        return searchResults;
    }


}
