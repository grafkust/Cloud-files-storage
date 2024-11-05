package com.project.cloud.files.storage.service.file;

import java.io.InputStream;

public interface FileDownloadService {

    InputStream download(String path, boolean isFile);
}
