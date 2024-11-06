package com.project.cloud.files.storage.util.mapper;

import com.project.cloud.files.storage.model.dto.StorageItemDto;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.util.hepler.StorageItemIconManager;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class StorageItemMapper {

    private final StorageItemIconManager storageItemIconManager;

    @Value("${application.date-pattern}")
    private String dateTimeFormatPattern;

    public StorageItemDto toContentDto(StorageItem item, boolean isSearchResult) {
        String objectPath = item.getPath();
        String fileName = Paths.get(objectPath).getFileName().toString();
        boolean isFile = item.isFile();

        String path = isSearchResult && !isFile ?
                objectPath.substring(0, objectPath.length() - 1) :
                objectPath;

        String formattedDate = isFile ? formatLastModifiedDate(item.getLastModified()) : "";

        return new StorageItemDto(
                fileName,
                formattedDate,
                storageItemIconManager.resolveItemTypeIcon(fileName, isFile),
                isFile,
                path,
                isSearchResult
        );
    }

    private String formatLastModifiedDate(LocalDateTime lastModified) {
        if (lastModified == null) {
            return "";
        }
        return lastModified.format(DateTimeFormatter.ofPattern(dateTimeFormatPattern));
    }
}
