package com.project.cloud.files.storage.util.hepler;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

@Slf4j
public class DeleteOnCloseFileInputStream extends FileInputStream {
    private final File file;

    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        try {
            super.close();
        } finally {
            if (!file.delete()) {
                log.error("Failed to delete file");
            }
        }
    }
}
