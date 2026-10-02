package com.project.BharatConnect.service.social;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.BharatConnect.dto.social.LikeResponseDto;
import com.project.BharatConnect.dto.social.LikerDto;
import com.project.BharatConnect.entity.Comment;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentLike;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.error.exception.CommentNotFoundException;
import com.project.BharatConnect.error.exception.ConflictException;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.event.CommentLikedEvent;
import com.project.BharatConnect.event.ContentLikedEvent;
import com.project.BharatConnect.repo.CommentLikeRepository;
import com.project.BharatConnect.repo.CommentRepository;
import com.project.BharatConnect.repo.ContentLikeRepository;
import com.project.BharatConnect.repo.ContentRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.repo.counter.ContentCounterRepository;
import com.project.BharatConnect.service.ratelimit.RateLimiterService;
import com.project.BharatConnect.service.user.UserDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LikeServiceTest {

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ContentLikeRepository contentLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ContentCounterRepository contentCounterRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private LikeService likeService;

    private UUID userId;
    private Profile profile;
    private Content content;
    private UUID contentId;
    private SecurityContext securityContext;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        profile = Profile.builder().userId(userId).userName("user1").build();

        UserDetail userDetail = new UserDetail(user);
        authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetail);

        securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        contentId = UUID.randomUUID();
        content = Content.builder().id(contentId).likeCount(0L).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testLikeContent_FirstTime_Success() {
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(contentLikeRepository.existsByContentIdAndProfileUserId(contentId, userId)).thenReturn(false);
        when(profileRepository.getReferenceById(userId)).thenReturn(profile);

        // After atomic increment in DB, next read returns count 1
        when(contentRepository.findById(contentId)).thenReturn(
                Optional.of(content),
                Optional.of(Content.builder().id(contentId).likeCount(1L).build())
        );

        LikeResponseDto response = likeService.likeContent(contentId);

        assertTrue(response.isLiked());
        assertEquals(1L, response.getLikeCount());
        verify(contentLikeRepository, times(1)).saveAndFlush(any(ContentLike.class));
        verify(contentCounterRepository, times(1)).incrementContentLikeCount(contentId);
        verify(eventPublisher, times(1)).publishEvent(any(ContentLikedEvent.class));
    }

    @Test
    void testLikeContent_AlreadyLiked_Idempotent_NoIncrement() {
        Content contentWithLikes = Content.builder().id(contentId).likeCount(5L).build();
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(contentWithLikes));
        when(contentLikeRepository.existsByContentIdAndProfileUserId(contentId, userId)).thenReturn(true);

        LikeResponseDto response = likeService.likeContent(contentId);

        assertTrue(response.isLiked());
        assertEquals(5L, response.getLikeCount());
        verify(contentLikeRepository, never()).saveAndFlush(any());
        verify(contentCounterRepository, never()).incrementContentLikeCount(any());
    }

    @Test
    void testLikeContent_ConcurrentDuplicateCatch_NoIncrement() {
        when(contentRepository.findById(contentId)).thenReturn(
                Optional.of(content),
                Optional.of(Content.builder().id(contentId).likeCount(1L).build())
        );
        when(contentLikeRepository.existsByContentIdAndProfileUserId(contentId, userId)).thenReturn(false);
        when(profileRepository.getReferenceById(userId)).thenReturn(profile);
        when(contentLikeRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        LikeResponseDto response = likeService.likeContent(contentId);

        assertTrue(response.isLiked());
        assertEquals(1L, response.getLikeCount());
        verify(contentCounterRepository, never()).incrementContentLikeCount(any());
    }

    @Test
    void testUnlikeContent_Success_DecrementsCount() {
        when(contentRepository.findById(contentId)).thenReturn(
                Optional.of(Content.builder().id(contentId).likeCount(5L).build()),
                Optional.of(Content.builder().id(contentId).likeCount(4L).build())
        );
        when(contentLikeRepository.deleteByContentIdAndProfileUserId(contentId, userId)).thenReturn(1);

        LikeResponseDto response = likeService.unlikeContent(contentId);

        assertFalse(response.isLiked());
        assertEquals(4L, response.getLikeCount());
        verify(contentCounterRepository, times(1)).decrementContentLikeCount(contentId);
    }

    @Test
    void testUnlikeContent_NotLikedBefore_Idempotent_NoDecrement() {
        when(contentRepository.findById(contentId)).thenReturn(
                Optional.of(Content.builder().id(contentId).likeCount(5L).build()),
                Optional.of(Content.builder().id(contentId).likeCount(5L).build())
        );
        when(contentLikeRepository.deleteByContentIdAndProfileUserId(contentId, userId)).thenReturn(0);

        LikeResponseDto response = likeService.unlikeContent(contentId);

        assertFalse(response.isLiked());
        assertEquals(5L, response.getLikeCount());
        verify(contentCounterRepository, never()).decrementContentLikeCount(any());
    }

    @Test
    void testLikeComment_Success() {
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder().id(commentId).likeCount(0L).deleted(false).build();
        when(commentRepository.findById(commentId)).thenReturn(
                Optional.of(comment),
                Optional.of(Comment.builder().id(commentId).likeCount(1L).deleted(false).build())
        );
        when(commentLikeRepository.existsByCommentIdAndProfileUserId(commentId, userId)).thenReturn(false);
        when(profileRepository.getReferenceById(userId)).thenReturn(profile);

        LikeResponseDto response = likeService.likeComment(commentId);

        assertTrue(response.isLiked());
        assertEquals(1L, response.getLikeCount());
        verify(contentCounterRepository, times(1)).incrementCommentLikeCount(commentId);
        verify(eventPublisher, times(1)).publishEvent(any(CommentLikedEvent.class));
    }

    @Test
    void testLikeComment_DeletedComment_ThrowsConflictException() {
        UUID commentId = UUID.randomUUID();
        Comment deletedComment = Comment.builder().id(commentId).deleted(true).build();
        when(commentRepository.findById(commentId)).thenReturn(Optional.of(deletedComment));

        assertThrows(ConflictException.class, () -> likeService.likeComment(commentId));
        verify(contentCounterRepository, never()).incrementCommentLikeCount(any());
    }

    @Test
    void testGetContentLikers_MaxPageSizeEnforced_AndOnlyThreeKeys() throws Exception {
        when(contentRepository.existsById(contentId)).thenReturn(true);
        LikerDto liker = new LikerDto(userId, "harsh", "Harsh M");
        Page<LikerDto> mockPage = new PageImpl<>(List.of(liker));
        when(contentLikeRepository.findLikersByContentId(eq(contentId), any(Pageable.class))).thenReturn(mockPage);

        Page<LikerDto> result = likeService.getContentLikers(contentId, 0, 500);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(contentLikeRepository).findLikersByContentId(eq(contentId), argThat(p -> p.getPageSize() == 100));

        // Assert JSON contains exactly the 3 keys: id, userName, displayName
        ObjectMapper om = new ObjectMapper();
        String json = om.writeValueAsString(result.getContent().get(0));
        Map<String, Object> map = om.readValue(json, Map.class);
        assertEquals(3, map.keySet().size());
        assertTrue(map.containsKey("id"));
        assertTrue(map.containsKey("userName"));
        assertTrue(map.containsKey("displayName"));
    }

    @Test
    void testGetCommentLikers_MaxPageSizeEnforced() {
        UUID commentId = UUID.randomUUID();
        when(commentRepository.existsById(commentId)).thenReturn(true);
        LikerDto liker = new LikerDto(userId, "harsh", "Harsh M");
        Page<LikerDto> mockPage = new PageImpl<>(List.of(liker));
        when(commentLikeRepository.findLikersByCommentId(eq(commentId), any(Pageable.class))).thenReturn(mockPage);

        Page<LikerDto> result = likeService.getCommentLikers(commentId, 0, 500);

        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        verify(commentLikeRepository).findLikersByCommentId(eq(commentId), argThat(p -> p.getPageSize() == 100));
    }

    @Test
    void testConcurrentDuplicateLikesBySameUser() throws InterruptedException {
        int threads = 20;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        AtomicInteger successfulIncrements = new AtomicInteger(0);

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(profileRepository.getReferenceById(userId)).thenReturn(profile);

        // First thread sees exists=false, others may see exists=false or true
        when(contentLikeRepository.existsByContentIdAndProfileUserId(contentId, userId)).thenAnswer(inv -> successfulIncrements.get() > 0);

        doAnswer(inv -> {
            if (successfulIncrements.incrementAndGet() > 1) {
                throw new DataIntegrityViolationException("duplicate");
            }
            return null;
        }).when(contentLikeRepository).saveAndFlush(any());

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    SecurityContextHolder.setContext(securityContext);
                    likeService.likeContent(contentId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Counter repository increment called exactly once!
        verify(contentCounterRepository, times(1)).incrementContentLikeCount(contentId);
    }
}
