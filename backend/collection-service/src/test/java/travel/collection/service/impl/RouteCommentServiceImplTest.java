package travel.collection.service.impl;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.builder.MapperBuilderAssistant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import travel.collection.service.RouteService;
import travel.collection.service.UserService;
import travel.common.entity.user_community.RouteComment;
import travel.collection.dto.CommentLikeToggleResponse;
import travel.common.mapper.route_planning_mapper.RouteCommentMapper;
import travel.common.service.DistributedLockService;
import travel.common.utils.CacheUtil;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteCommentServiceImplTest {

    @Mock
    private RouteService routeService;

    @Mock
    private UserService userService;

    @Mock
    private CacheUtil cacheUtil;

    @Mock
    private DistributedLockService distributedLockService;

    @Mock
    private RouteCommentMapper routeCommentMapper;

    private RouteCommentServiceImpl service;

    @BeforeEach
    @SuppressWarnings({"rawtypes", "unchecked"})
    void setUp() {
        if (TableInfoHelper.getTableInfo(RouteComment.class) == null) {
            TableInfoHelper.initTableInfo(
                    new MapperBuilderAssistant(new MybatisConfiguration(), "route-comment-test"),
                    RouteComment.class);
        }
        service = spy(new RouteCommentServiceImpl(
                routeService,
                userService,
                cacheUtil,
                distributedLockService));
        ReflectionTestUtils.setField(service, "baseMapper", routeCommentMapper);
        lenient().when(distributedLockService.executeWithLock(anyString(), any(Supplier.class)))
                .thenAnswer(invocation -> invocation.<Supplier<?>>getArgument(1).get());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldScopeRouteCommentPageToRequestedRoute() {
        Page<RouteComment> pageResult = new Page<>();
        pageResult.setRecords(List.of(comment(11, 8, 42, 4.5, true, null)));
        doReturn(pageResult).when(service).page(any(Page.class), any(Wrapper.class));

        List<RouteComment> comments = service.getRouteComments(8, 1, 20);

        assertEquals(1, comments.size());
        ArgumentCaptor<Wrapper<RouteComment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(service).page(any(Page.class), wrapperCaptor.capture());
        LambdaQueryWrapper<RouteComment> wrapper = (LambdaQueryWrapper<RouteComment>) wrapperCaptor.getValue();
        String sqlSegment = wrapper.getSqlSegment().toLowerCase();
        assertTrue(sqlSegment.contains("route_id"));
        assertTrue(sqlSegment.contains("is_published"));
        assertTrue(sqlSegment.contains("reply_to"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(8));
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void shouldIgnoreUnratedCommentsWhenCalculatingAverage() {
        RouteComment rated = comment(1, 8, 42, 4.5, true, null);
        RouteComment unrated = comment(2, 8, 43, null, true, null);
        doReturn(List.of(rated, unrated)).when(service).list(any(Wrapper.class));

        Map<String, Object> statistics = service.getCommentStatistics(8);

        assertEquals(2, statistics.get("totalComments"));
        assertEquals(4.5, (Double) statistics.get("averageRating"), 0.0001);
        assertEquals(Map.of(5, 1), statistics.get("ratingDistribution"));
        ArgumentCaptor<Wrapper<RouteComment>> wrapperCaptor = ArgumentCaptor.forClass(Wrapper.class);
        verify(service).list(wrapperCaptor.capture());
        LambdaQueryWrapper<RouteComment> wrapper = (LambdaQueryWrapper<RouteComment>) wrapperCaptor.getValue();
        assertTrue(wrapper.getSqlSegment().toLowerCase().contains("route_id"));
        assertTrue(wrapper.getParamNameValuePairs().containsValue(8));
    }

    @Test
    void shouldNotFabricateDefaultRatingForNewCommentEntity() {
        assertNull(new RouteComment().getRating());
    }

    @Test
    void shouldPersistLikeAndAtomicallyIncrementCounter() {
        RouteComment routeComment = comment(7, 8, 10, 4.5, true, null);
        doReturn(routeComment).when(service).getById(7);
        when(routeCommentMapper.countCommentLike(7, 42)).thenReturn(0);
        when(routeCommentMapper.insertCommentLike(7, 42)).thenReturn(1);
        when(routeCommentMapper.incrementCommentLikeCount(7)).thenReturn(1);
        when(routeCommentMapper.selectCommentLikeCount(7)).thenReturn(6);

        CommentLikeToggleResponse result = service.toggleLikeComment(7, 42);

        assertEquals(true, result.liked());
        assertEquals(6, result.likeCount());
        verify(distributedLockService).executeWithLock(
                org.mockito.ArgumentMatchers.eq("route-comment-like:7:42"),
                org.mockito.ArgumentMatchers.<Supplier<CommentLikeToggleResponse>>any());
        verify(routeCommentMapper).incrementCommentLikeCount(7);
        verify(cacheUtil).deleteByPattern("route_comment:route:8:*");
        verify(cacheUtil).deleteByPattern("route_comment:user:10:*");
    }

    @Test
    void shouldNotIncrementCounterWhenLikeAlreadyExists() {
        RouteComment routeComment = comment(7, 8, 10, 4.5, true, null);
        doReturn(routeComment).when(service).getById(7);
        when(routeCommentMapper.countCommentLike(7, 42)).thenReturn(1);

        boolean changed = service.likeComment(7, 42);

        assertFalse(changed);
        verify(routeCommentMapper, never()).insertCommentLike(7, 42);
        verify(routeCommentMapper, never()).incrementCommentLikeCount(7);
    }

    @Test
    void shouldSoftDeleteCommentWithoutPromotingReplies() {
        RouteComment routeComment = comment(7, 8, 42, 4.5, true, null);
        routeComment.setLikesCount(9);
        doReturn(routeComment).when(service).getById(7);
        doReturn(true).when(service).updateById(any(RouteComment.class));

        boolean deleted = service.deleteComment(7, 42);

        assertTrue(deleted);
        assertFalse(routeComment.getIsPublished());
        assertEquals(0, routeComment.getLikesCount());
        verify(routeCommentMapper).deleteAllCommentLikes(7);
        verify(service, never()).removeById(7);
        verify(cacheUtil).delete("route_comment:stats:8");
    }

    private RouteComment comment(
            int id,
            int routeId,
            int userId,
            Double rating,
            boolean published,
            Integer replyTo) {
        RouteComment comment = new RouteComment();
        comment.setId(id);
        comment.setRouteId(routeId);
        comment.setUserId(userId);
        comment.setRating(rating);
        comment.setIsPublished(published);
        comment.setReplyTo(replyTo);
        comment.setLikesCount(0);
        return comment;
    }
}
