package com.project.cloud.files.storage.service.trash;

import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.service.storage.StorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TrashServiceImpl implements TrashService {

    private final StorageService storageService;

    @Value("${application.trash.retention-days}")
    private Integer trashRetentionDays;

    @Override
    public void removeExpiredTrashItems() {
        List<StorageItem> rootDirectories = storageService.list("", false);
        List<String> trashDirectories = getUsersTrashDirectories(rootDirectories);
        trashDirectories.forEach(this::removeExpiredTrashItems);

    }

    private List<String> getUsersTrashDirectories(List<StorageItem> rootDirectories) {
        return rootDirectories.stream()
                .filter(item -> !item.isFile())
                .map(item -> String.format("%sTrash/", item.getPath()))
                .collect(Collectors.toList());
    }


    private void removeExpiredTrashItems(String trashDirectory) {
        List<StorageItem> trashContents = storageService.list(trashDirectory, true);
        try {
            trashContents.stream()
                    .filter(item -> !item.getPath().equals(trashDirectory))
                    .filter(this::isItemExpired)
                    .forEach(item -> storageService.delete(item.getPath()));
        } catch (Exception e) {
            throw new StorageOperationException("Failed to remove expired object from storage", e);
        }
    }

    private boolean isItemExpired(StorageItem item) {
        LocalDateTime lastModified = item.getLastModified();
        LocalDateTime threshold = LocalDateTime.now().minusDays(trashRetentionDays);
        return lastModified.isBefore(threshold);
    }


}
