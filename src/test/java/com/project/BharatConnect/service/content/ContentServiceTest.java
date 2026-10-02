package com.project.BharatConnect.service.content;

import com.project.BharatConnect.dto.content.ContentCreateRequest;
import com.project.BharatConnect.dto.content.ContentResponseDto;
import com.project.BharatConnect.entity.Content;
import com.project.BharatConnect.entity.ContentType;
import com.project.BharatConnect.entity.Poll;
import com.project.BharatConnect.entity.Profile;
import com.project.BharatConnect.entity.User;
import com.project.BharatConnect.error.exception.ContentNotFoundException;
import com.project.BharatConnect.error.exception.InvalidMediaException;
import com.project.BharatConnect.error.exception.InvalidRequestException;
import com.project.BharatConnect.mapper.ContentMapper;
import com.project.BharatConnect.repo.CommentLikeRepository;
import com.project.BharatConnect.repo.CommentRepository;
import com.project.BharatConnect.repo.ContentLikeRepository;
import com.project.BharatConnect.repo.ContentRepository;
import com.project.BharatConnect.repo.ProfileRepository;
import com.project.BharatConnect.service.media.MediaService;
import com.project.BharatConnect.service.user.UserDetail;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContentServiceTest {

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
    private MediaService mediaService;

    @Mock
    private PollService pollService;

    @Mock
    private ContentMapper contentMapper;

    @InjectMocks
    private ContentService contentService;

    private UUID userId;
    private Profile profile;
    private User user;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        user = User.builder().userId(userId).email("test@example.com").build();
        profile = Profile.builder().userId(userId).userName("testuser").displayName("Test User").build();

        UserDetail userDetail = new UserDetail(user);
        Authentication authentication = mock(Authentication.class);
        lenient().when(authentication.getPrincipal()).thenReturn(userDetail);
        lenient().doReturn(Collections.emptyList()).when(authentication).getAuthorities();

        SecurityContext securityContext = mock(SecurityContext.class);
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testUploadContent_NullContentType_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder().build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("contentType is required", ex.getMessage());
    }

    // --- TEXT TESTS ---
    @Test
    void testUploadText_Success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.TEXT)
                .text("This is my thought")
                .build();

        when(contentRepository.save(any(Content.class))).thenAnswer(i -> {
            Content c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(contentMapper.toResponse(any(Content.class), any(), any(), any(), anyBoolean())).thenReturn(ContentResponseDto.builder().build());

        ContentResponseDto response = contentService.uploadContent(request, null);

        assertNotNull(response);
        verify(contentRepository, times(1)).save(any(Content.class));
    }

    @Test
    void testUploadText_EmptyText_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.TEXT)
                .text("   ")
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Text cannot be empty", ex.getMessage());
    }

    @Test
    void testUploadText_WithFile_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        MockMultipartFile file = new MockMultipartFile("file", "test.txt", "text/plain", "abc".getBytes());
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.TEXT)
                .text("Some text")
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, file)
        );
        assertEquals("Text posts cannot have a file", ex.getMessage());
    }

    // --- IMAGE TESTS ---
    @Test
    void testUploadImage_Success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2, 3});
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.IMAGE)
                .text("Nice photo")
                .build();

        when(contentRepository.save(any(Content.class))).thenAnswer(i -> {
            Content c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(mediaService.uploadPostImage(any(), any())).thenReturn(Map.of("secure_url", "https://cloudinary.com/pic.png"));
        when(contentMapper.toResponse(any(Content.class), any(), any(), any(), anyBoolean())).thenReturn(ContentResponseDto.builder().build());

        ContentResponseDto response = contentService.uploadContent(request, file);

        assertNotNull(response);
        verify(mediaService, times(1)).uploadPostImage(eq(file), any());
    }

    @Test
    void testUploadImage_MissingFile_ThrowsInvalidMediaException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.IMAGE)
                .text("Nice photo")
                .build();

        InvalidMediaException ex = assertThrows(InvalidMediaException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Image file required", ex.getMessage());
    }

    @Test
    void testUploadImage_InvalidMime_ThrowsInvalidMediaException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        MockMultipartFile file = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2});
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.IMAGE)
                .build();

        InvalidMediaException ex = assertThrows(InvalidMediaException.class, () ->
                contentService.uploadContent(request, file)
        );
        assertEquals("Image file required", ex.getMessage());
    }

    // --- VIDEO TESTS ---
    @Test
    void testUploadVideo_Success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        MockMultipartFile file = new MockMultipartFile("file", "video.mp4", "video/mp4", new byte[]{1, 2, 3});
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.VIDEO)
                .text("Nice video")
                .build();

        when(contentRepository.save(any(Content.class))).thenAnswer(i -> {
            Content c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(mediaService.uploadVideo(any(), any())).thenReturn(Map.of("secure_url", "https://cloudinary.com/vid.mp4"));
        when(contentMapper.toResponse(any(Content.class), any(), any(), any(), anyBoolean())).thenReturn(ContentResponseDto.builder().build());

        ContentResponseDto response = contentService.uploadContent(request, file);

        assertNotNull(response);
        verify(mediaService, times(1)).uploadVideo(eq(file), any());
    }

    @Test
    void testUploadVideo_MissingFile_ThrowsInvalidMediaException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.VIDEO)
                .build();

        InvalidMediaException ex = assertThrows(InvalidMediaException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Video file required", ex.getMessage());
    }

    // --- POLL TESTS ---
    @Test
    void testUploadPoll_Success() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("What is your favorite cloud?")
                .pollOptions(List.of("AWS", "GCP", "Azure"))
                .pollDurationHours(12)
                .build();

        when(contentRepository.save(any(Content.class))).thenAnswer(i -> {
            Content c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(pollService.createPoll(any(), eq(request))).thenReturn(Poll.builder().build());
        when(contentMapper.toResponse(any(Content.class), any(), any(), any(), anyBoolean())).thenReturn(ContentResponseDto.builder().build());

        ContentResponseDto response = contentService.uploadContent(request, null);

        assertNotNull(response);
        verify(pollService, times(1)).createPoll(any(Content.class), eq(request));
    }

    @Test
    void testUploadPoll_MissingQuestion_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("")
                .pollOptions(List.of("Option A", "Option B"))
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Poll question is required", ex.getMessage());
    }

    @Test
    void testUploadPoll_WithFile_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2});
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("Poll question?")
                .pollOptions(List.of("Option A", "Option B"))
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, file)
        );
        assertEquals("Polls cannot have a file", ex.getMessage());
    }

    @Test
    void testUploadPoll_LessThanTwoOptions_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("Poll question?")
                .pollOptions(List.of("Only one option"))
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Poll needs 2 to 6 options", ex.getMessage());
    }

    @Test
    void testUploadPoll_MoreThanSixOptions_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("Poll question?")
                .pollOptions(List.of("1", "2", "3", "4", "5", "6", "7"))
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Poll needs 2 to 6 options", ex.getMessage());
    }

    @Test
    void testUploadPoll_DuplicateOrBlankOptions_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.POLL)
                .text("Poll question?")
                .pollOptions(List.of("Java", "java", "Python"))
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Poll options must be non-blank and unique", ex.getMessage());
    }

    // --- REPOST TESTS ---
    @Test
    void testUploadRepost_MissingParentContentId_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("parentContentId is required for repost", ex.getMessage());
    }

    @Test
    void testUploadRepost_ParentNotFound_ThrowsContentNotFoundException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UUID parentId = UUID.randomUUID();
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .parentContentId(parentId)
                .build();

        when(contentRepository.findById(parentId)).thenReturn(Optional.empty());

        assertThrows(ContentNotFoundException.class, () ->
                contentService.uploadContent(request, null)
        );
    }

    @Test
    void testUploadRepost_WithFile_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UUID parentId = UUID.randomUUID();
        Content parent = Content.builder().id(parentId).contentType(ContentType.TEXT).text("Original").build();

        when(contentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(contentRepository.existsPlainRepost(userId, ContentType.REPOST, parentId)).thenReturn(false);

        MockMultipartFile file = new MockMultipartFile("file", "image.png", "image/png", new byte[]{1, 2});
        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .parentContentId(parentId)
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, file)
        );
        assertEquals("Reposts cannot have a file", ex.getMessage());
    }

    @Test
    void testUploadRepost_QuoteTooLong_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UUID parentId = UUID.randomUUID();
        Content parent = Content.builder().id(parentId).contentType(ContentType.TEXT).text("Original").build();

        when(contentRepository.findById(parentId)).thenReturn(Optional.of(parent));

        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .parentContentId(parentId)
                .text("a".repeat(501))
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Quote text cannot exceed 500 characters", ex.getMessage());
    }

    @Test
    void testUploadRepost_PlainDuplicate_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UUID parentId = UUID.randomUUID();
        Content parent = Content.builder().id(parentId).contentType(ContentType.TEXT).text("Original").build();

        when(contentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(contentRepository.existsPlainRepost(userId, ContentType.REPOST, parentId)).thenReturn(true);

        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .parentContentId(parentId)
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("You have already reposted this content", ex.getMessage());
    }

    @Test
    void testUploadRepost_PlainSuccess_IncrementsRepostCount() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UUID parentId = UUID.randomUUID();
        Content parent = Content.builder().id(parentId).contentType(ContentType.TEXT).text("Original").build();

        when(contentRepository.findById(parentId)).thenReturn(Optional.of(parent));
        when(contentRepository.existsPlainRepost(userId, ContentType.REPOST, parentId)).thenReturn(false);
        when(contentRepository.save(any(Content.class))).thenAnswer(i -> {
            Content c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });
        when(contentMapper.toResponse(any(Content.class), any(), any(), any(), anyBoolean())).thenReturn(ContentResponseDto.builder().build());

        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .parentContentId(parentId)
                .build();

        ContentResponseDto response = contentService.uploadContent(request, null);

        assertNotNull(response);
        verify(contentRepository, times(1)).incrementRepostCount(parentId);
    }

    @Test
    void testUploadRepost_ChainResolvesToOriginalParent() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UUID originalId = UUID.randomUUID();
        Content original = Content.builder().id(originalId).contentType(ContentType.TEXT).text("Original").build();

        UUID intermediateRepostId = UUID.randomUUID();
        Content intermediateRepost = Content.builder()
                .id(intermediateRepostId)
                .contentType(ContentType.REPOST)
                .parentContent(original)
                .build();

        when(contentRepository.findById(intermediateRepostId)).thenReturn(Optional.of(intermediateRepost));
        when(contentRepository.existsPlainRepost(userId, ContentType.REPOST, originalId)).thenReturn(false);
        when(contentRepository.save(any(Content.class))).thenAnswer(i -> {
            Content c = i.getArgument(0);
            c.setId(UUID.randomUUID());
            assertEquals(original, c.getParentContent());
            return c;
        });
        when(contentMapper.toResponse(any(Content.class), any(), any(), any(), anyBoolean())).thenReturn(ContentResponseDto.builder().build());

        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .parentContentId(intermediateRepostId)
                .build();

        ContentResponseDto response = contentService.uploadContent(request, null);

        assertNotNull(response);
        verify(contentRepository, times(1)).incrementRepostCount(originalId);
    }

    @Test
    void testUploadRepost_ChainOriginalDeleted_ThrowsInvalidRequestException() {
        when(profileRepository.findById(userId)).thenReturn(Optional.of(profile));
        UUID intermediateRepostId = UUID.randomUUID();
        Content intermediateRepost = Content.builder()
                .id(intermediateRepostId)
                .contentType(ContentType.REPOST)
                .parentContent(null)
                .build();

        when(contentRepository.findById(intermediateRepostId)).thenReturn(Optional.of(intermediateRepost));

        ContentCreateRequest request = ContentCreateRequest.builder()
                .contentType(ContentType.REPOST)
                .parentContentId(intermediateRepostId)
                .build();

        InvalidRequestException ex = assertThrows(InvalidRequestException.class, () ->
                contentService.uploadContent(request, null)
        );
        assertEquals("Original content no longer exists", ex.getMessage());
    }

    // --- DELETE TESTS ---
    @Test
    void testDeleteContent_Repost_DecrementsParentRepostCount() {
        UUID parentId = UUID.randomUUID();
        Content parent = Content.builder().id(parentId).build();

        UUID contentId = UUID.randomUUID();
        Content content = Content.builder()
                .id(contentId)
                .profile(profile)
                .contentType(ContentType.REPOST)
                .parentContent(parent)
                .build();

        when(contentRepository.findById(contentId)).thenReturn(Optional.of(content));

        Boolean result = contentService.deleteContent(contentId);

        assertTrue(result);
        verify(contentRepository, times(1)).decrementRepostCount(parentId);
        verify(contentRepository, times(1)).clearParentContentReferences(contentId);
        verify(contentRepository, times(1)).delete(content);
    }
}
