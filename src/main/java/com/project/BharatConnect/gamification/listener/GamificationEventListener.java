package com.project.BharatConnect.gamification.listener;

import com.project.BharatConnect.entity.ContentType;
import com.project.BharatConnect.event.*;
import com.project.BharatConnect.gamification.config.GamificationProperties;
import com.project.BharatConnect.gamification.entity.*;
import com.project.BharatConnect.gamification.repo.ProfileStatsRepository;
import com.project.BharatConnect.gamification.service.AchievementEvaluator;
import com.project.BharatConnect.gamification.service.PointsService;
import com.project.BharatConnect.gamification.service.QuestService;
import com.project.BharatConnect.gamification.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class GamificationEventListener {

    private final PointsService pointsService;
    private final StreakService streakService;
    private final QuestService questService;
    private final AchievementEvaluator achievementEvaluator;
    private final ProfileStatsRepository profileStatsRepository;
    private final GamificationProperties properties;

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContentCreated(ContentCreatedEvent event) {
        try {
            UUID profileId = event.getProfileId();
            if (profileId == null) return;

            profileStatsRepository.incrementPostsCount(profileId);
            ProfileStats stats = pointsService.getOrCreateProfileStats(profileId);

            PointAction action = PointAction.POST_CREATED_TEXT;
            int points = properties.getPostText();

            ContentType type = event.getContentType();
            if (type != null) {
                switch (type) {
                    case IMAGE -> {
                        action = PointAction.POST_CREATED_IMAGE;
                        points = properties.getPostImage();
                    }
                    case VIDEO -> {
                        action = PointAction.POST_CREATED_VIDEO;
                        points = properties.getPostVideo();
                    }
                    case POLL -> {
                        action = PointAction.POST_CREATED_POLL;
                        points = properties.getPostPoll();
                    }
                    case QUIZ -> {
                        action = PointAction.POST_CREATED_QUIZ;
                        points = properties.getPostQuiz();
                    }
                    case REPOST -> {
                        action = PointAction.POST_CREATED_REPOST;
                        points = properties.getPostRepost();
                    }
                    default -> {
                        action = PointAction.POST_CREATED_TEXT;
                        points = properties.getPostText();
                    }
                }
            }

            pointsService.award(profileId, action, PointSourceType.CONTENT, event.getContentId(), points);

            // First post bonus check
            if (stats != null && stats.getPostsCount() == 1) {
                pointsService.award(profileId, PointAction.FIRST_POST_BONUS, PointSourceType.CONTENT, event.getContentId(), properties.getFirstPostBonus());
            }

            streakService.recordActivity(profileId);
            questService.recordQuestProgress(profileId, QuestMetric.POSTS_TODAY, 1);
            achievementEvaluator.evaluate(profileId, List.of(AchievementMetric.POSTS, AchievementMetric.STREAK, AchievementMetric.POINTS, AchievementMetric.LEVEL));
        } catch (Exception e) {
            log.error("Gamification error on ContentCreatedEvent (contentId={}): {}", event.getContentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContentDeleted(ContentDeletedEvent event) {
        try {
            UUID profileId = event.getProfileId();
            if (profileId == null) return;

            profileStatsRepository.decrementPostsCount(profileId);
            pointsService.reversePointsForSource(profileId, event.getContentId(), PointAction.POST_DELETED_REVERSAL, PointSourceType.CONTENT);
        } catch (Exception e) {
            log.error("Gamification error on ContentDeletedEvent (contentId={}): {}", event.getContentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContentLiked(ContentLikedEvent event) {
        try {
            UUID likerId = event.getProfileId();
            UUID ownerId = event.getOwnerId();
            if (likerId == null) return;

            profileStatsRepository.incrementLikesGiven(likerId);
            ProfileStats likerStats = pointsService.getOrCreateProfileStats(likerId);

            if (likerStats != null && likerStats.getLikesGiven() == 1) {
                pointsService.award(likerId, PointAction.FIRST_LIKE_GIVEN_BONUS, PointSourceType.CONTENT, event.getContentId(), properties.getFirstLikeGivenBonus());
            }

            questService.recordQuestProgress(likerId, QuestMetric.LIKES_GIVEN_TODAY, 1);
            achievementEvaluator.evaluate(likerId, List.of(AchievementMetric.LIKES_GIVEN, AchievementMetric.POINTS));

            // Award to owner if not self-like
            if (ownerId != null && !ownerId.equals(likerId)) {
                profileStatsRepository.incrementLikesReceived(ownerId);
                pointsService.award(ownerId, PointAction.LIKE_RECEIVED_CONTENT, PointSourceType.CONTENT, event.getContentId(), properties.getLikeReceivedContent());
                achievementEvaluator.evaluate(ownerId, List.of(AchievementMetric.LIKES_RECEIVED, AchievementMetric.POINTS));
            }
        } catch (Exception e) {
            log.error("Gamification error on ContentLikedEvent (contentId={}): {}", event.getContentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContentUnliked(ContentUnlikedEvent event) {
        try {
            UUID unlikerId = event.getProfileId();
            UUID ownerId = event.getOwnerId();
            if (unlikerId == null) return;

            profileStatsRepository.decrementLikesGiven(unlikerId);

            if (ownerId != null && !ownerId.equals(unlikerId)) {
                profileStatsRepository.decrementLikesReceived(ownerId);
                pointsService.reversePointsForSource(ownerId, event.getContentId(), PointAction.LIKE_REMOVED, PointSourceType.CONTENT);
            }
        } catch (Exception e) {
            log.error("Gamification error on ContentUnlikedEvent (contentId={}): {}", event.getContentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentCreated(CommentCreatedEvent event) {
        try {
            UUID authorId = event.getProfileId();
            UUID contentOwnerId = event.getContentOwnerId();
            if (authorId == null) return;

            profileStatsRepository.incrementCommentsCount(authorId);
            ProfileStats authorStats = pointsService.getOrCreateProfileStats(authorId);

            PointAction action = event.getParentCommentId() == null ? PointAction.COMMENT_CREATED : PointAction.REPLY_CREATED;
            int points = event.getParentCommentId() == null ? properties.getCommentCreate() : properties.getReplyCreate();

            pointsService.award(authorId, action, PointSourceType.COMMENT, event.getCommentId(), points);

            if (authorStats != null && authorStats.getCommentsCount() == 1) {
                pointsService.award(authorId, PointAction.FIRST_COMMENT_BONUS, PointSourceType.COMMENT, event.getCommentId(), properties.getFirstCommentBonus());
            }

            streakService.recordActivity(authorId);
            questService.recordQuestProgress(authorId, QuestMetric.COMMENTS_TODAY, 1);
            achievementEvaluator.evaluate(authorId, List.of(AchievementMetric.COMMENTS, AchievementMetric.STREAK, AchievementMetric.POINTS));

            // Content owner points
            if (contentOwnerId != null && !contentOwnerId.equals(authorId)) {
                pointsService.award(contentOwnerId, PointAction.COMMENT_RECEIVED_CONTENT, PointSourceType.CONTENT, event.getCommentId(), properties.getCommentReceivedContent());
                achievementEvaluator.evaluate(contentOwnerId, List.of(AchievementMetric.POINTS));
            }
        } catch (Exception e) {
            log.error("Gamification error on CommentCreatedEvent (commentId={}): {}", event.getCommentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentDeleted(CommentDeletedEvent event) {
        try {
            UUID authorId = event.getProfileId();
            if (authorId == null) return;

            profileStatsRepository.decrementCommentsCount(authorId);
            pointsService.reversePointsForSource(authorId, event.getCommentId(), PointAction.COMMENT_DELETED_REVERSAL, PointSourceType.COMMENT);
        } catch (Exception e) {
            log.error("Gamification error on CommentDeletedEvent (commentId={}): {}", event.getCommentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentLiked(CommentLikedEvent event) {
        try {
            UUID likerId = event.getProfileId();
            UUID authorId = event.getAuthorId();
            if (likerId == null) return;

            profileStatsRepository.incrementLikesGiven(likerId);
            questService.recordQuestProgress(likerId, QuestMetric.LIKES_GIVEN_TODAY, 1);
            achievementEvaluator.evaluate(likerId, List.of(AchievementMetric.LIKES_GIVEN, AchievementMetric.POINTS));

            if (authorId != null && !authorId.equals(likerId)) {
                profileStatsRepository.incrementLikesReceived(authorId);
                pointsService.award(authorId, PointAction.LIKE_RECEIVED_COMMENT, PointSourceType.COMMENT, event.getCommentId(), properties.getLikeReceivedComment());
                achievementEvaluator.evaluate(authorId, List.of(AchievementMetric.LIKES_RECEIVED, AchievementMetric.POINTS));
            }
        } catch (Exception e) {
            log.error("Gamification error on CommentLikedEvent (commentId={}): {}", event.getCommentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleCommentUnliked(CommentUnlikedEvent event) {
        try {
            UUID unlikerId = event.getProfileId();
            UUID authorId = event.getAuthorId();
            if (unlikerId == null) return;

            profileStatsRepository.decrementLikesGiven(unlikerId);

            if (authorId != null && !authorId.equals(unlikerId)) {
                profileStatsRepository.decrementLikesReceived(authorId);
                pointsService.reversePointsForSource(authorId, event.getCommentId(), PointAction.LIKE_REMOVED, PointSourceType.COMMENT);
            }
        } catch (Exception e) {
            log.error("Gamification error on CommentUnlikedEvent (commentId={}): {}", event.getCommentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handlePollVoted(PollVotedEvent event) {
        try {
            UUID voterId = event.getProfileId();
            if (voterId == null) return;

            profileStatsRepository.incrementPollVotes(voterId);
            pointsService.award(voterId, PointAction.POLL_VOTED, PointSourceType.POLL, event.getContentId(), properties.getPollVote());

            streakService.recordActivity(voterId);
            questService.recordQuestProgress(voterId, QuestMetric.POLL_VOTES_TODAY, 1);
            achievementEvaluator.evaluate(voterId, List.of(AchievementMetric.POLL_VOTES, AchievementMetric.STREAK, AchievementMetric.POINTS));
        } catch (Exception e) {
            log.error("Gamification error on PollVotedEvent (contentId={}): {}", event.getContentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuizAnswered(QuizAnsweredEvent event) {
        try {
            UUID userId = event.getProfileId();
            if (userId == null) return;

            profileStatsRepository.incrementQuizzesAnswered(userId, event.isCorrect());
            pointsService.award(userId, PointAction.QUIZ_ANSWERED, PointSourceType.QUIZ, event.getContentId(), properties.getQuizAnswer());

            if (event.isCorrect()) {
                pointsService.award(userId, PointAction.QUIZ_CORRECT_BONUS, PointSourceType.QUIZ, event.getContentId(), properties.getQuizCorrectBonus());
            }

            streakService.recordActivity(userId);
            questService.recordQuestProgress(userId, QuestMetric.QUIZ_ANSWERS_TODAY, 1);
            achievementEvaluator.evaluate(userId, List.of(AchievementMetric.QUIZZES_ANSWERED, AchievementMetric.QUIZZES_CORRECT, AchievementMetric.STREAK, AchievementMetric.POINTS));
        } catch (Exception e) {
            log.error("Gamification error on QuizAnsweredEvent (contentId={}): {}", event.getContentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleContentReposted(ContentRepostedEvent event) {
        try {
            UUID reposterId = event.getReposterId();
            UUID ownerId = event.getOwnerId();

            if (ownerId != null && !ownerId.equals(reposterId)) {
                profileStatsRepository.incrementRepostsReceived(ownerId);
                pointsService.award(ownerId, PointAction.REPOST_RECEIVED_CONTENT, PointSourceType.CONTENT, event.getContentId(), properties.getRepostReceivedContent());
                achievementEvaluator.evaluate(ownerId, List.of(AchievementMetric.REPOSTS_RECEIVED, AchievementMetric.POINTS));
            }
        } catch (Exception e) {
            log.error("Gamification error on ContentRepostedEvent (contentId={}): {}", event.getContentId(), e.getMessage(), e);
        }
    }

    @Async("gamificationTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleLevelUp(LevelUpEvent event) {
        try {
            achievementEvaluator.evaluate(event.getProfileId(), List.of(AchievementMetric.LEVEL, AchievementMetric.POINTS));
        } catch (Exception e) {
            log.error("Gamification error on LevelUpEvent (profileId={}): {}", event.getProfileId(), e.getMessage(), e);
        }
    }
}
