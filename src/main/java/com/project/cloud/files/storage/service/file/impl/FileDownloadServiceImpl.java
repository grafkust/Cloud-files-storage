package com.project.cloud.files.storage.service.file.impl;

import com.project.cloud.files.storage.exception.FileDownloadException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.service.file.FileDownloadService;
import com.project.cloud.files.storage.service.storage.StorageService;
import com.project.cloud.files.storage.util.hepler.DeleteOnCloseFileInputStream;
import com.project.cloud.files.storage.util.validator.PathUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileDownloadServiceImpl implements FileDownloadService {

    private static final int BUFFER_SIZE = 8192;
    private final StorageService storageService;
    private final PathUtil pathUtil;

    @Override
    public InputStream download(String path, boolean isFile) {
        return isFile ? downloadFile(path) : downloadFolder(path);
    }

    private InputStream downloadFile(String filePath) {
        return storageService.retrieve(filePath);
    }

    private InputStream downloadFolder(String folderPath) {
        String normalizedPath = pathUtil.normalizeStoragePath(folderPath);
        List<StorageItem> items = storageService.list(normalizedPath, true);

        File tempZipFile = createTempFile();
        try {
            createZipArchive(tempZipFile, items, normalizedPath);
            return new DeleteOnCloseFileInputStream(tempZipFile);
        } catch (Exception e) {
            deleteTempFile(tempZipFile);
            throw new FileDownloadException("Failed to create zip archive for folder", e);
        }
    }

    private File createTempFile() {
        try {
            return File.createTempFile("download_", ".zip");
        } catch (Exception e) {
            throw new FileDownloadException("Failed to create temporary file", e);
        }
    }

    private void createZipArchive(File tempZipFile, List<StorageItem> items, String basePath) throws IOException {
        try (FileOutputStream fileOutputStream = new FileOutputStream(tempZipFile);
             BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(fileOutputStream);
             ZipOutputStream zipOutputStream = new ZipOutputStream(bufferedOutputStream)) {

            for (StorageItem item : items) {
                addItemToZip(item, zipOutputStream, basePath);
            }
            zipOutputStream.flush();
        }
    }

    private void addItemToZip(StorageItem item, ZipOutputStream zipOutputStream, String basePath) throws IOException {

        if (item.getPath().equals(basePath)) {
            return;
        }

        String relativePath = item.getPath().substring(basePath.length());
        ZipEntry zipEntry = new ZipEntry(relativePath);
        zipOutputStream.putNextEntry(zipEntry);


        if (item.isFile()) {
            writeContentToZip(item, zipOutputStream);
        }

        zipOutputStream.closeEntry();
    }


    private void writeContentToZip(StorageItem item, ZipOutputStream zipOutputStream) throws IOException {
        try (InputStream contentStream = storageService.retrieve(item.getPath())) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = contentStream.read(buffer)) != -1) {
                zipOutputStream.write(buffer, 0, bytesRead);
            }
        }
    }

    private void deleteTempFile(File file) {
        if (file != null && file.exists()) {
            if (!file.delete()) {
                log.warn("Failed to delete temporary file: {}", file.getAbsolutePath());
            }
        }
    }


}
