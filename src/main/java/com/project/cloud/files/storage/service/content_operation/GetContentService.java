package com.project.cloud.files.storage.service.content_operation;

import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.model.dto.ContentDto;
import com.project.cloud.files.storage.util.ContentIconUtil;
import com.project.cloud.files.storage.util.PathUtil;
import io.minio.Result;
import io.minio.messages.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GetContentService {

    private final MinioOperation minioOperation;
    private final PathUtil pathUtil;
    private final ContentIconUtil contentIconUtil;

    @Value("${minio.bucket}")
    private String MAIN_BUCKET;


    public List<ContentDto> getPageContent(String userRootPath, String innerPath, String query) {
        if (query != null && !query.isEmpty()) {
            return searchFiles(userRootPath, query);
        } else {
            return getListFilesInFolder(innerPath);
        }
    }

    public List<ContentDto> getListFilesInFolder(String path) {
        String prefix = pathUtil.correctPath(path);
        String currentDirName = path.substring(path.lastIndexOf("/") + 1);

        return getMinioItems(prefix, false)
                .stream()
                .filter(item -> isValidItem(item, currentDirName))
                .map(item -> createContentDto(item, false))
                .collect(Collectors.toList());
    }

    public List<ContentDto> searchFiles(String userRootPath, String searchQuery) {
        return getMinioItems(userRootPath, true)
                .stream()
                .filter(item -> {
                    String fileName = Paths.get(item.objectName()).getFileName().toString();
                    return fileName.toLowerCase().contains(searchQuery.toLowerCase())
                            && !fileName.equals(userRootPath);
                })
                .map(item -> createContentDto(item, true))
                .collect(Collectors.toList());
    }

    public List<ContentDto> getListDirectories(String path, String excludeName) {
        String prefix = pathUtil.correctPath(path);
        String userRootPath = path.contains("/") ? path.substring(0, path.indexOf("/") + 1) : path + "/";

        return getMinioItems(prefix, false)
                .stream()
                .filter(item -> isValidDirectory(item, excludeName))
                .map(item -> {
                    String objectName = item.objectName();
                    String folderName = Paths.get(objectName).getFileName().toString();
                    String displayName = objectName.equals(userRootPath) ? "Файлы" : folderName;

                    return new ContentDto(
                            displayName,
                            contentIconUtil.getContentIcon(folderName, false),
                            objectName
                    );
                })
                .collect(Collectors.toList());
    }


    private List<Item> getMinioItems(String prefix, boolean recursive) {
        List<Item> items = new ArrayList<>();

        Iterable<Result<Item>> minioItems = minioOperation.getListOfObjects(MAIN_BUCKET, prefix, recursive);
        for (Result<Item> result : minioItems) {
            try {
                Item item = result.get();
                items.add(item);
            } catch (Exception e) {
                throw new StorageOperationException("Error by get item from minio", e);
            }
        }
        return items;
    }

    private boolean isValidItem(Item item, String currentDirName) {
        String fileName = Paths.get(item.objectName()).getFileName().toString();
        return !fileName.equals(currentDirName) && !fileName.equals("Trash");
    }

    private boolean isValidDirectory(Item item, String excludeName) {
        String objectName = item.objectName();
        String folderName = Paths.get(objectName).getFileName().toString();

        boolean itemIsFile = !objectName.endsWith("/");
        boolean itemIsTrash = folderName.equals("Trash");
        boolean folderIsMovementItem = folderName.equals(excludeName);

        return !itemIsFile && !itemIsTrash && !folderIsMovementItem;
    }

    private ContentDto createContentDto(Item item, boolean isSearchResult) {
        String objectName = item.objectName();
        String fileName = Paths.get(objectName).getFileName().toString();
        boolean isFile = !objectName.endsWith("/");
        String lastModified = isFile ? minioOperation.getLastModifiedDate(MAIN_BUCKET, objectName) : "";
        String path = isSearchResult && objectName.endsWith("/")
                ? objectName.substring(0, objectName.length() - 1)
                : objectName;

        return new ContentDto(
                fileName,
                lastModified,
                contentIconUtil.getContentIcon(fileName, isFile),
                isFile,
                path,
                isSearchResult
        );
    }


}
