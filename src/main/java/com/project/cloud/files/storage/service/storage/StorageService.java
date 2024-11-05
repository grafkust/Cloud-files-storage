package com.project.cloud.files.storage.service.storage;

import com.project.cloud.files.storage.model.entity.storage.StorageItem;

import java.io.InputStream;
import java.util.List;

public interface StorageService {

    void store(InputStream content, String path);

    InputStream retrieve(String path);

    void delete(String path);

    boolean exists(String path);

    List<StorageItem> list(String path, boolean recursive);

    void copy(String sourcePath, String destinationPath);

    void move(String sourcePath, String destinationPath);

    void createDirectory(String path);
}
