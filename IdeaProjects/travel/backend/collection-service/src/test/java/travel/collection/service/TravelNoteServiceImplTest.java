package travel.collection.service;

import org.junit.jupiter.api.Test;
import travel.collection.service.impl.TravelNoteServiceImpl;
import travel.common.entity.user_community.TravelNote;
import travel.common.exception.BusinessException;
import travel.common.mapper.user_community_mapper.TravelNoteCollectionMapper;
import travel.common.mapper.user_community_mapper.TravelNoteTagMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class TravelNoteServiceImplTest {

    @Test
    void shouldRejectUpdateFromNonOwner() {
        TravelNoteServiceImpl service = service();
        TravelNote existing = note(7);
        doReturn(existing).when(service).getById(8);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.updateTravelNote(8, 42, note(42), null));

        assertEquals(28001, exception.getCode());
        verify(service, never()).updateById(any(TravelNote.class));
    }

    @Test
    void shouldInitializeServerOwnedFieldsWhenCreatingNote() {
        TravelNoteServiceImpl service = service();
        TravelNote request = note(999);
        request.setId(88);
        request.setViewsCount(100);
        request.setLikesCount(100);
        request.setCommentsCount(100);
        request.setIsPublic(null);
        doReturn(true).when(service).save(any(TravelNote.class));

        TravelNote created = service.createTravelNote(42, request, null);

        assertNull(created.getId());
        assertEquals(42, created.getUserId());
        assertEquals(0, created.getViewsCount());
        assertEquals(0, created.getLikesCount());
        assertEquals(0, created.getCommentsCount());
        assertFalse(created.getCreatedAt().isAfter(created.getUpdatedAt()));
    }

    private TravelNoteServiceImpl service() {
        return spy(new TravelNoteServiceImpl(
                mock(TravelNoteTagMapper.class),
                mock(TravelNoteCollectionMapper.class)));
    }

    private TravelNote note(Integer userId) {
        TravelNote note = new TravelNote();
        note.setUserId(userId);
        note.setTitle("title");
        note.setContent("content");
        return note;
    }
}
