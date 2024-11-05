package com.project.cloud.files.storage.service.file;

import com.project.cloud.files.storage.model.dto.ContentDto;

import java.util.List;

public interface FileSearchService {

    List<ContentDto> listDirectory(String path, String excludeName);

    List<ContentDto> searchFiles(String userRootPath, String searchQuery);

    List<ContentDto> getListFilesInFolder(String path);
}
