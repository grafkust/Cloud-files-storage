package com.project.cloud.files.storage.service.trash;

import com.project.cloud.files.storage.BaseIntegrationTest;
import com.project.cloud.files.storage.exception.StorageOperationException;
import com.project.cloud.files.storage.model.entity.storage.StorageItem;
import com.project.cloud.files.storage.service.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringBootTest
class TrashServiceImplIntegrationTest extends BaseIntegrationTest {

    @MockBean
    private StorageService storageService;

    @Autowired
    private TrashServiceImpl trashService;

    private final String TEST_ROOT_PATH = "test-user-files/";
    private final String TRASH_PATH = TEST_ROOT_PATH + "Trash/";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(trashService, "trashRetentionDays", 30);

        when(storageService.list("", false))
                .thenReturn(Collections.singletonList(new StorageItem(TEST_ROOT_PATH)));
    }

    @Test
    @DisplayName("Should remove expired items from trash")
    void removeExpiredTrashItemsTest() {

        StorageItem expiredItem = mock(StorageItem.class);
        when(expiredItem.getPath()).thenReturn(TRASH_PATH + "expired-file.txt");
        when(expiredItem.getLastModified()).thenReturn(LocalDateTime.now().minusDays(31));
        when(expiredItem.isFile()).thenReturn(true);

        StorageItem activeItem = mock(StorageItem.class);
        when(activeItem.getPath()).thenReturn(TRASH_PATH + "active-file.txt");
        when(activeItem.getLastModified()).thenReturn(LocalDateTime.now().minusDays(1));
        when(activeItem.isFile()).thenReturn(true);

        when(storageService.list(TRASH_PATH, true))
                .thenReturn(Arrays.asList(
                        new StorageItem(TRASH_PATH),
                        expiredItem,
                        activeItem
                ));

        trashService.removeExpiredTrashItems();

        verify(storageService, times(1)).delete(expiredItem.getPath());
        verify(storageService, never()).delete(activeItem.getPath());
        verify(storageService, never()).delete(TRASH_PATH);
    }

    @Test
    @DisplayName("Should not remove items when no expired trash items found")
    void removeExpiredTrashItemsNoExpiredTest() {

        StorageItem activeItem = mock(StorageItem.class);
        when(activeItem.getPath()).thenReturn(TRASH_PATH + "active-file.txt");
        when(activeItem.getLastModified()).thenReturn(LocalDateTime.now().minusDays(1));
        when(activeItem.isFile()).thenReturn(true);

        when(storageService.list(TRASH_PATH, true))
                .thenReturn(Arrays.asList(
                        new StorageItem(TRASH_PATH),
                        activeItem
                ));

        trashService.removeExpiredTrashItems();

        verify(storageService, never()).delete(anyString());
    }

    @Test
    @DisplayName("Should throw StorageOperationException when error occurs during deletion")
    void removeExpiredTrashItemsExceptionTest() {

        when(storageService.list(anyString(), anyBoolean()))
                .thenThrow(new StorageOperationException("Test exception", new RuntimeException()));

        assertThrows(StorageOperationException.class, () -> trashService.removeExpiredTrashItems());
    }
}