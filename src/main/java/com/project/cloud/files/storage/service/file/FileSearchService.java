package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.model.dto.StorageItemDto;

import java.util.List;

public interface FileSearchService {

    List<StorageItemDto> listDirectory(String path, String excludeName);

    List<StorageItemDto> searchFileOrDirectory(String userRootPath, String searchQuery);

    List<StorageItemDto> getContentOfFolder(String path);
}
