package com.project.BharatConnect.service.comment;

import com.project.BharatConnect.dto.comment.CommentCreateRequest;
import com.project.BharatConnect.dto.comment.CommentResponseDto;
import com.project.BharatConnect.dto.comment.CommentUpdateRequest;
import com.project.BharatConnect.dto.common.CursorPageResponse;
import com.project.BharatConnect.entity.Comment;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.error.exception.*;
import com.project.BharatConnect.event.CommentCreatedEvent;
import com.project.BharatConnect.mapper.CommentMapper;
import com.project.BharatConnect.repo.CommentLikeRepository;
import com.project.BharatConnect.repo.CommentRepository;
import com.project.BharatConnect.repo.ContentRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.repo.counter.ContentCounterRepository;
import com.project.BharatConnect.service.ratelimit.RateLimiterService;
import com.project.BharatConnect.service.user.UserDetail;
import com.project.BharatConnect.util.CursorUtil;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private ProfileRepository profileRepository;

    @Mock
    private ContentCounterRepository contentCounterRepository;

    @Mock
    private CommentMapper commentMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private RateLimiterService rateLimiterService;

    @InjectMocks
    private CommentService commentService;

    private UUID userId;
    private Profile profile;
    private Content content;
    private UUID contentId;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        User user = User.builder().userId(userId).build();
        profile = Profile.builder().userId(userId).userName("user1").build();

        UserDetail userDetail = new UserDetail(user);
        authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetail);
        lenient().doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        contentId = UUID.randomUUID();
        content = Content.builder().id(contentId).profile(profile).build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testCreateTopLevelComment_Success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> {
            Comment c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(commentMapper.toResponse(any(), eq(false))).thenReturn(CommentResponseDto.builder().build());

        CommentCreateRequest request = CommentCreateRequest.builder().text("Great post!").build();
        CommentResponseDto response = commentService.createComment(contentId, request);

        assertNotNull(response);
        verify(contentCounterRepository, times(1)).incrementContentCommentCount(contentId);
        verify(eventPublisher, times(1)).publishEvent(any(CommentCreatedEvent.class));
        verify(commentRepository, times(1)).save(any(Comment.class));
    }

    @Test
    void testCreateReply_FirstAndSecond_Success() {
        UUID parentCommentId = UUID.randomUUID();
        Comment parentComment = Comment.builder().id(parentCommentId).content(content).replyCount(0L).deleted(false).build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(commentRepository.findByIdWithProfileAndParent(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(contentCounterRepository.tryIncrementReplyCount(parentCommentId, 2)).thenReturn(1);
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentMapper.toResponse(any(), eq(false))).thenReturn(CommentResponseDto.builder().build());

        CommentCreateRequest request = CommentCreateRequest.builder().text("Reply 1").parentCommentId(parentCommentId).build();
        CommentResponseDto response = commentService.createComment(contentId, request);

        assertNotNull(response);
        verify(contentCounterRepository, times(1)).tryIncrementReplyCount(parentCommentId, 2);
        verify(contentCounterRepository, times(1)).incrementContentCommentCount(contentId);
    }

    @Test
    void testCreateReply_ThirdReply_ThrowsReplyLimitReachedException_409() {
        UUID parentCommentId = UUID.randomUUID();
        Comment parentComment = Comment.builder().id(parentCommentId).content(content).replyCount(2L).deleted(false).build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(commentRepository.findByIdWithProfileAndParent(parentCommentId)).thenReturn(Optional.of(parentComment));
        when(contentCounterRepository.tryIncrementReplyCount(parentCommentId, 2)).thenReturn(0);

        CommentCreateRequest request = CommentCreateRequest.builder().text("Reply 3").parentCommentId(parentCommentId).build();

        ReplyLimitReachedException ex = assertThrows(ReplyLimitReachedException.class, () ->
                commentService.createComment(contentId, request)
        );
        assertTrue(ex.getMessage().contains("maximum number of replies (2)"));
        verify(commentRepository, never()).save(any());
        verify(contentCounterRepository, never()).incrementContentCommentCount(any());
    }

    @Test
    void testCreateReply_ToReply_ThrowsNestingLimitExceededException_422() {
        UUID topLevelId = UUID.randomUUID();
        Comment topLevel = Comment.builder().id(topLevelId).content(content).build();

        UUID intermediateReplyId = UUID.randomUUID();
        Comment intermediateReply = Comment.builder()
                .id(intermediateReplyId)
                .content(content)
                .parentComment(topLevel)
                .build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(commentRepository.findByIdWithProfileAndParent(intermediateReplyId)).thenReturn(Optional.of(intermediateReply));

        CommentCreateRequest request = CommentCreateRequest.builder().text("Nested reply").parentCommentId(intermediateReplyId).build();

        NestingLimitExceededException ex = assertThrows(NestingLimitExceededException.class, () ->
                commentService.createComment(contentId, request)
        );
        assertEquals("Replies cannot be nested", ex.getMessage());
    }

    @Test
    void testCreateReply_DifferentContent_ThrowsInvalidRequestException_400() {
        UUID otherContentId = UUID.randomUUID();
        Content otherContent = Content.builder().id(otherContentId).build();

        UUID parentCommentId = UUID.randomUUID();
        Comment parentComment = Comment.builder().id(parentCommentId).content(otherContent).build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(commentRepository.findByIdWithProfileAndParent(parentCommentId)).thenReturn(Optional.of(parentComment));

        CommentCreateRequest request = CommentCreateRequest.builder().text("Reply cross-content").parentCommentId(parentCommentId).build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                commentService.createComment(contentId, request)
        );
        assertEquals("Parent comment does not belong to the specified content", ex.getMessage());
    }

    @Test
    void testCreateReply_SoftDeletedParent_ThrowsConflictException_409() {
        UUID parentCommentId = UUID.randomUUID();
        Comment parentComment = Comment.builder().id(parentCommentId).content(content).deleted(true).build();

        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));
        when(commentRepository.findByIdWithProfileAndParent(parentCommentId)).thenReturn(Optional.of(parentComment));

        CommentCreateRequest request = CommentCreateRequest.builder().text("Reply to deleted").parentCommentId(parentCommentId).build();

        ConflictException ex = assertThrows(ConflictException.class, () ->
                commentService.createComment(contentId, request)
        );
        assertEquals("Cannot reply to a deleted comment", ex.getMessage());
    }

    @Test
    void testEditComment_ByAuthor_Success() {
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder().id(commentId).profile(profile).text("Old text").deleted(false).edited(false).build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.save(any(Comment.class))).thenAnswer(inv -> inv.getArgument(0));
        when(commentLikeRepository.existsByCommentIdAndProfileUserId(commentId, userId)).thenReturn(false);
        when(commentMapper.toResponse(any(), eq(false))).thenReturn(CommentResponseDto.builder().build());

        CommentUpdateRequest request = CommentUpdateRequest.builder().text("Updated text").build();
        CommentResponseDto response = commentService.editComment(commentId, request);

        assertNotNull(response);
        assertEquals("Updated text", comment.getText());
        assertTrue(comment.isEdited());
        assertNotNull(comment.getUpdatedAt());
        verify(commentRepository, times(1)).save(comment);
    }

    @Test
    void testEditComment_ByNonAuthor_ThrowsAccessDeniedException_403() {
        UUID commentId = UUID.randomUUID();
        Profile otherProfile = Profile.builder().userId(UUID.randomUUID()).build();
        Comment comment = Comment.builder().id(commentId).profile(otherProfile).text("Old text").deleted(false).build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));

        CommentUpdateRequest request = CommentUpdateRequest.builder().text("Updated text").build();
        assertThrows(AccessDeniedException.class, () ->
                commentService.editComment(commentId, request)
        );
        verify(commentRepository, never()).save(any());
    }

    @Test
    void testEditComment_DeletedComment_ThrowsConflictException() {
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder().id(commentId).profile(profile).text("Old text").deleted(true).build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));

        CommentUpdateRequest request = CommentUpdateRequest.builder().text("Updated text").build();
        ConflictException ex = assertThrows(ConflictException.class, () ->
                commentService.editComment(commentId, request)
        );
        assertEquals("Cannot edit a deleted comment", ex.getMessage());
    }

    @Test
    void testDeleteReply_AlwaysHardDelete() {
        UUID parentId = UUID.randomUUID();
        Comment parent = Comment.builder().id(parentId).build();

        UUID replyId = UUID.randomUUID();
        Comment reply = Comment.builder()
                .id(replyId)
                .content(content)
                .profile(profile)
                .parentComment(parent)
                .build();

        when(commentRepository.findByIdWithProfileAndParent(replyId)).thenReturn(Optional.of(reply));

        commentService.deleteComment(replyId);

        verify(contentCounterRepository, times(1)).decrementContentCommentCount(contentId);
        verify(contentCounterRepository, times(1)).decrementCommentReplyCount(parentId);
        verify(commentLikeRepository, times(1)).deleteByCommentId(replyId);
        verify(commentRepository, times(1)).delete(reply);
        assertFalse(reply.isDeleted());
    }

    @Test
    void testDeleteTopLevel_WithReplies_SoftDeletes() {
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .content(content)
                .profile(profile)
                .replyCount(1L)
                .deleted(false)
                .build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));

        commentService.deleteComment(commentId);

        assertTrue(comment.isDeleted());
        assertEquals("[deleted]", comment.getText());
        verify(contentCounterRepository, times(1)).decrementContentCommentCount(contentId);
        verify(commentRepository, times(1)).save(comment);
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void testDeleteTopLevel_WithoutReplies_HardDeletes() {
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .content(content)
                .profile(profile)
                .replyCount(0L)
                .deleted(false)
                .build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.existsByParentCommentId(commentId)).thenReturn(false);

        commentService.deleteComment(commentId);

        verify(contentCounterRepository, times(1)).decrementContentCommentCount(contentId);
        verify(commentLikeRepository, times(1)).deleteByCommentId(commentId);
        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void testDeleteComment_ByContentOwner_Success() {
        UUID commentId = UUID.randomUUID();
        Profile commentAuthor = Profile.builder().userId(UUID.randomUUID()).build();
        // Current user is content owner
        Comment comment = Comment.builder()
                .id(commentId)
                .content(content)
                .profile(commentAuthor)
                .replyCount(0L)
                .deleted(false)
                .build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.existsByParentCommentId(commentId)).thenReturn(false);

        commentService.deleteComment(commentId);

        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void testDeleteComment_ByAdmin_Success() {
        UUID adminId = UUID.randomUUID();
        User adminUser = User.builder().userId(adminId).build();
        UserDetail adminDetail = new UserDetail(adminUser);

        Authentication adminAuth = mock(Authentication.class);
        lenient().when(adminAuth.getPrincipal()).thenReturn(adminDetail);
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))).when(adminAuth).getAuthorities();

        SecurityContext adminContext = mock(SecurityContext.class);
        when(adminContext.getAuthentication()).thenReturn(adminAuth);
        SecurityContextHolder.setContext(adminContext);

        UUID commentId = UUID.randomUUID();
        Profile randomAuthor = Profile.builder().userId(UUID.randomUUID()).build();
        Profile randomContentOwner = Profile.builder().userId(UUID.randomUUID()).build();
        Content randomContent = Content.builder().id(UUID.randomUUID()).profile(randomContentOwner).build();

        Comment comment = Comment.builder()
                .id(commentId)
                .content(randomContent)
                .profile(randomAuthor)
                .replyCount(0L)
                .deleted(false)
                .build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));
        when(commentRepository.existsByParentCommentId(commentId)).thenReturn(false);

        commentService.deleteComment(commentId);

        verify(commentRepository, times(1)).delete(comment);
    }

    @Test
    void testDeleteComment_ByRandomUser_ThrowsAccessDeniedException() {
        UUID randomUserId = UUID.randomUUID();
        User randomUser = User.builder().userId(randomUserId).build();
        UserDetail randomDetail = new UserDetail(randomUser);

        Authentication userAuth = mock(Authentication.class);
        lenient().when(userAuth.getPrincipal()).thenReturn(randomDetail);
        lenient().doReturn(Collections.emptyList()).when(userAuth).getAuthorities();

        SecurityContext userContext = mock(SecurityContext.class);
        when(userContext.getAuthentication()).thenReturn(userAuth);
        SecurityContextHolder.setContext(userContext);

        UUID commentId = UUID.randomUUID();
        Profile author = Profile.builder().userId(UUID.randomUUID()).build();
        Profile contentOwner = Profile.builder().userId(UUID.randomUUID()).build();
        Content c = Content.builder().id(UUID.randomUUID()).profile(contentOwner).build();

        Comment comment = Comment.builder()
                .id(commentId)
                .content(c)
                .profile(author)
                .replyCount(0L)
                .deleted(false)
                .build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));

        assertThrows(AccessDeniedException.class, () -> commentService.deleteComment(commentId));
        verify(commentRepository, never()).delete(any());
    }

    @Test
    void testDeleteComment_AlreadyDeleted_ThrowsConflictException() {
        UUID commentId = UUID.randomUUID();
        Comment comment = Comment.builder()
                .id(commentId)
                .content(content)
                .profile(profile)
                .deleted(true)
                .build();

        when(commentRepository.findByIdWithProfileAndParent(commentId)).thenReturn(Optional.of(comment));

        ConflictException ex = assertThrows(ConflictException.class, () -> commentService.deleteComment(commentId));
        assertEquals("Comment already deleted", ex.getMessage());
    }

    @Test
    void testGetTopLevelComments_CursorPagination() {
        when(contentRepository.existsById(contentId)).thenReturn(true);

        LocalDateTime now = LocalDateTime.now();
        UUID c1Id = UUID.randomUUID();
        UUID c2Id = UUID.randomUUID();
        UUID c3Id = UUID.randomUUID();

        Comment c1 = Comment.builder().id(c1Id).createdAt(now).build();
        Comment c2 = Comment.builder().id(c2Id).createdAt(now.minusMinutes(1)).build();
        Comment c3 = Comment.builder().id(c3Id).createdAt(now.minusMinutes(2)).build();

        // 3 items returned when limit is 2 -> hasMore = true
        when(commentRepository.findTopLevelCommentsInitial(eq(contentId), any(Pageable.class)))
                .thenReturn(List.of(c1, c2, c3));

        when(commentLikeRepository.findLikedCommentIdsByProfile(eq(userId), any())).thenReturn(List.of(c1Id));
        when(commentMapper.toResponse(eq(c1), any(Set.class))).thenReturn(CommentResponseDto.builder().id(c1Id).likedByMe(true).build());
        when(commentMapper.toResponse(eq(c2), any(Set.class))).thenReturn(CommentResponseDto.builder().id(c2Id).likedByMe(false).build());

        CursorPageResponse<CommentResponseDto> response = commentService.getTopLevelComments(contentId, null, 2);

        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertTrue(response.isHasMore());
        assertNotNull(response.getNextCursor());

        // Decode next cursor
        CursorUtil.Cursor decoded = CursorUtil.decode(response.getNextCursor());
        assertEquals(c2.getCreatedAt(), decoded.createdAt());
        assertEquals(c2.getId(), decoded.id());
    }

    @Test
    void testGetReplies_ReturnsAtMostMaxReplies_HasMoreFalse() {
        UUID parentId = UUID.randomUUID();
        when(commentRepository.existsById(parentId)).thenReturn(true);

        Comment r1 = Comment.builder().id(UUID.randomUUID()).build();
        Comment r2 = Comment.builder().id(UUID.randomUUID()).build();
        when(commentRepository.findRepliesByParentCommentId(parentId)).thenReturn(List.of(r1, r2));

        when(commentMapper.toResponse(any(Comment.class), any(Set.class))).thenReturn(CommentResponseDto.builder().build());

        CursorPageResponse<CommentResponseDto> response = commentService.getReplies(parentId);

        assertNotNull(response);
        assertEquals(2, response.getItems().size());
        assertFalse(response.isHasMore());
        assertNull(response.getNextCursor());
    }
}
