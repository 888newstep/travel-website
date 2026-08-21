package travel.file.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.common.entity.travel_recommendation.ResourceFile;
import travel.common.exception.BusinessException;
import travel.file.service.impl.ResourceFileServiceImpl;
import travel.file.storage.FileStoragePolicy;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ResourceFileServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldAssignAuthenticatedUserWhenUploading() {
        authenticate(42L);
        FileStoragePolicy storagePolicy = mock(FileStoragePolicy.class);
        MockMultipartFile upload = new MockMultipartFile("file", "photo.jpg", "image/jpeg", new byte[]{1});
        when(storagePolicy.store(upload)).thenReturn(new FileStoragePolicy.StoredFile(
                Path.of("photo.jpg"), "photo.jpg", "stored.jpg", 1L, "jpg"));
        when(storagePolicy.toPublicPath("stored.jpg")).thenReturn("/uploads/stored.jpg");
        ResourceFileServiceImpl service = spy(createService(storagePolicy));
        doReturn(true).when(service).save(any(ResourceFile.class));

        ResourceFile result = service.uploadResourceFile(upload, "image", "test");

        assertEquals(42, result.getUploadUserId());
        assertEquals("photo.jpg", result.getFileName());
    }

    @Test
    void shouldRejectDeletingAnotherUsersFile() {
        authenticate(42L);
        ResourceFile anotherUsersFile = new ResourceFile();
        anotherUsersFile.setId(8);
        anotherUsersFile.setUploadUserId(7);
        ResourceFileServiceImpl service = spy(createService(mock(FileStoragePolicy.class)));
        doReturn(anotherUsersFile).when(service).getById(8);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.deleteResourceFile(8L));

        assertEquals(9009, exception.getCode());
    }

    @Test
    void shouldReturnOwnedFile() {
        authenticate(42L);
        ResourceFile ownedFile = new ResourceFile();
        ownedFile.setId(8);
        ownedFile.setUploadUserId(42);
        ResourceFileServiceImpl service = spy(createService(mock(FileStoragePolicy.class)));
        doReturn(ownedFile).when(service).getById(8);

        assertSame(ownedFile, service.getResourceFile(8L));
    }

    private ResourceFileServiceImpl createService(FileStoragePolicy storagePolicy) {
        return new ResourceFileServiceImpl(storagePolicy);
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
