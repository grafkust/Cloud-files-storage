package com.project.cloud.files.storage.service.file.impl;

import com.project.cloud.files.storage.model.dto.StorageItemDto;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.service.file.FileSearchService;
import com.project.cloud.files.storage.service.storage.StorageService;
import com.project.cloud.files.storage.util.hepler.StorageItemIconManager;
import com.project.cloud.files.storage.util.mapper.StorageItemMapper;
import com.project.cloud.files.storage.util.validator.PathUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileSearchServiceImpl implements FileSearchService {

    private final StorageService storageService;
    private final PathUtil pathUtil;
    private final StorageItemIconManager storageItemIconManager;
    private final StorageItemMapper storageItemMapper;

    @Override
    public List<StorageItemDto> listDirectory(String path, String excludeName) {
        String prefix = pathUtil.normalizeStoragePath(path);
        String userRootPath = path.contains("/") ? path.substring(0, path.indexOf("/") + 1) : path + "/";

        List<StorageItem> items = storageService.list(prefix, false);

        return items.stream()
                .filter(item -> isValidDirectory(item, excludeName))
                .map(item -> createDirectoryDto(item, userRootPath))
                .collect(Collectors.toList());
    }

    @Override
    public List<StorageItemDto> searchFileOrDirectory(String userRootPath, String searchQuery) {
        if (searchQuery == null || searchQuery.isEmpty()) {
            return List.of();
        }

        List<StorageItem> items = storageService.listWithCommonPrefixes(userRootPath);

        return items.stream()
                .filter(item -> matchesSearchCriteria(item, searchQuery))
                .map(item -> storageItemMapper.toContentDto(item, true))
                .collect(Collectors.toList());
    }

    @Override
    public List<StorageItemDto> getContentOfFolder(String path) {
        String prefix = pathUtil.normalizeStoragePath(path);
        String currentDirName = path.substring(path.lastIndexOf("/") + 1);

        List<StorageItem> items = storageService.list(prefix, false);

        return items.stream()
                .filter(item -> isValidItem(item, currentDirName))
                .map(item -> storageItemMapper.toContentDto(item, false))
                .collect(Collectors.toList());
    }

    private boolean isValidDirectory(StorageItem item, String excludeName) {
        String objectPath = item.getPath();
        String folderName = Paths.get(objectPath).getFileName().toString();

        return !item.isFile()
                && !folderName.equals("Trash")
                && !folderName.equals(excludeName);
    }

    private boolean isValidItem(StorageItem item, String currentDirName) {
        String fileName = Paths.get(item.getPath()).getFileName().toString();
        return !fileName.equals(currentDirName) && !fileName.equals("Trash");
    }

    private boolean matchesSearchCriteria(StorageItem item, String searchQuery) {
        String fileName = Paths.get(item.getPath()).getFileName().toString();
        String userFilesPattern = "user-\\d+-files";
        if (fileName.matches(userFilesPattern)) {
            return false;
        }
        return fileName.toLowerCase().contains(searchQuery.toLowerCase());
    }

    private StorageItemDto createDirectoryDto(StorageItem item, String userRootPath) {
        String objectPath = item.getPath();
        String folderName = Paths.get(objectPath).getFileName().toString();

        String displayName = objectPath.equals(userRootPath) ? "Файлы" : folderName;

        return new StorageItemDto(
                displayName,
                storageItemIconManager.resolveItemTypeIcon(folderName, false),
                objectPath
        );
    }


}
