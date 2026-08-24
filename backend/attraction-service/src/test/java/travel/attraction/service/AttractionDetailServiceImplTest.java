package travel.attraction.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import travel.attraction.service.impl.AttractionDetailServiceImpl;
import travel.common.entity.travel_recommendation.Attraction;
import travel.common.entity.travel_recommendation.AttractionReview;
import travel.common.exception.BusinessException;
import travel.common.mapper.travel_recommendation_mapper.AttractionReviewMapper;
import travel.common.mapper.travel_realtime_mapper.AttractionRealtimeStatusMapper;
import travel.common.utils.CacheUtil;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AttractionDetailServiceImplTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldUseAuthenticatedUserAndUpsertReview() {
        authenticate(42L);
        AttractionService attractionService = mock(AttractionService.class);
        AttractionReviewMapper reviewMapper = mock(AttractionReviewMapper.class);
        when(attractionService.getById(8)).thenReturn(new Attraction());
        when(reviewMapper.upsertReview(any(AttractionReview.class))).thenAnswer(invocation -> {
            AttractionReview review = invocation.getArgument(0);
            review.setId(10);
            return 1;
        });
        AttractionDetailServiceImpl service = new AttractionDetailServiceImpl(
                attractionService, mock(CacheUtil.class), reviewMapper,
                mock(AttractionRealtimeStatusMapper.class));

        Map<String, Object> result = service.saveAttractionReview(8, 7, 5, " great ");

        assertEquals(42, result.get("userId"));
        assertEquals("great", result.get("content"));
        verify(reviewMapper).upsertReview(any(AttractionReview.class));
    }

    @Test
    void shouldRejectInvalidRatingBeforeDatabaseAccess() {
        authenticate(42L);
        AttractionService attractionService = mock(AttractionService.class);
        AttractionReviewMapper reviewMapper = mock(AttractionReviewMapper.class);
        AttractionDetailServiceImpl service = new AttractionDetailServiceImpl(
                attractionService, mock(CacheUtil.class), reviewMapper,
                mock(AttractionRealtimeStatusMapper.class));

        BusinessException exception = assertThrows(
                BusinessException.class, () -> service.saveAttractionReview(8, 42, 6, "invalid"));

        assertEquals(4000, exception.getCode());
        verifyNoInteractions(attractionService, reviewMapper);
    }

    private void authenticate(Long userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, null, List.of()));
    }
}
