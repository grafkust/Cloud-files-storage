package com.project.cloud.files.storage.model.entity.storage;

import io.minio.messages.Item;
import lombok.Getter;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Getter
public class StorageItem {

    private final String name;
    private final String path;
    private final boolean isFile;
    private final LocalDateTime lastModified;

    public StorageItem(Item item) {
        this.path = item.objectName();
        this.name = Paths.get(path).getFileName().toString();
        this.isFile = !path.endsWith("/");
        this.lastModified = getModificationDate(item);
    }

    private LocalDateTime getModificationDate(Item item) {
        try {
            return item.lastModified() != null ?
                    item.lastModified()
                            .toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime() :
                    LocalDateTime.now();
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }


}
