package travel.collection.service;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import travel.collection.service.impl.TravelNoteServiceImpl;
import travel.common.entity.user_community.TravelNote;
import travel.common.exception.BusinessException;
import travel.common.mapper.user_community_mapper.TravelNoteCollectionMapper;
import travel.common.mapper.user_community_mapper.TravelNoteMapper;
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
import static org.mockito.Mockito.when;

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

    @Test
    void shouldMakeRepeatedLikeIdempotent() {
        TravelNoteMapper noteMapper = mock(TravelNoteMapper.class);
        TravelNoteCollectionMapper collectionMapper = mock(TravelNoteCollectionMapper.class);
        TravelNoteServiceImpl service = spy(new TravelNoteServiceImpl(
                mock(TravelNoteTagMapper.class), collectionMapper));
        ReflectionTestUtils.setField(service, "baseMapper", noteMapper);

        TravelNote note = note(42);
        note.setId(8);
        note.setLikesCount(3);
        when(noteMapper.selectByIdForUpdate(8)).thenReturn(note);
        when(collectionMapper.selectCount(any())).thenReturn(0L, 1L);
        when(collectionMapper.insert(any(travel.common.entity.user_community.TravelNoteCollection.class)))
                .thenReturn(1);
        when(noteMapper.incrementLikeCount(8)).thenReturn(1);

        assertEquals(true, service.likeTravelNote(8, 42));
        assertEquals(true, service.likeTravelNote(8, 42));

        verify(noteMapper).incrementLikeCount(8);
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
