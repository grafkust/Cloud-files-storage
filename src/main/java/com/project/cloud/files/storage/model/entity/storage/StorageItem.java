package com.project.cloud.files.storage.model.entity.storage;

import io.minio.messages.Item;
import lombok.Getter;
import lombok.Setter;

import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Objects;

@Getter
public class StorageItem {

    private final String name;
    private final String path;
    private final LocalDateTime lastModified;
    @Setter
    private boolean isFile;


    public StorageItem(Item item) {
        this.path = item.objectName();
        this.name = Paths.get(path).getFileName().toString();
        this.isFile = !path.endsWith("/");
        this.lastModified = getModificationDate(item);
    }

    public StorageItem(String path) {
        this.path = path;
        this.name = Paths.get(path.replaceAll("/$", "")).getFileName().toString();
        this.isFile = false;
        this.lastModified = LocalDateTime.now();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StorageItem that = (StorageItem) o;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
    }


}
