package com.chamchamcham.application.community;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.application.exception.ErrorCode;
import com.chamchamcham.application.exception.business.BusinessException;
import com.chamchamcham.domain.community.*;
import com.chamchamcham.domain.crop.*;
import com.chamchamcham.domain.farming.*;
import com.chamchamcham.domain.media.*;
import com.chamchamcham.domain.member.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.lang.Nullable;
import java.util.*;

@Service
@Transactional
public class CommunityPostService {
    private final MemberRepository memberRepository;
    private final CropRepository cropRepository;
    private final FarmingRecordRepository farmingRecordRepository;
    private final UploadedMediaRepository uploadedMediaRepository;
    private final CommunityPostRepository communityPostRepository;
    private final CommunityPostMediaRepository communityPostMediaRepository;
    private final CommunityCommentRepository communityCommentRepository;
    private final CommunityPostLikeRepository communityPostLikeRepository;
    private final MemberCropRepository memberCropRepository;
    private final CommunityPostQueryRepository communityPostQueryRepository;
    private final OpaqueCursorCodec cursorCodec;
    private static final int BODY_PREVIEW_LENGTH = 80;
    private static final int COUNT_QUERY_SIZE = 1;

    public CommunityPostService(MemberRepository memberRepository, CropRepository cropRepository,
                                FarmingRecordRepository farmingRecordRepository, UploadedMediaRepository uploadedMediaRepository,
                                CommunityPostRepository communityPostRepository, CommunityPostMediaRepository communityPostMediaRepository,
                                CommunityCommentRepository communityCommentRepository, CommunityPostLikeRepository communityPostLikeRepository,
                                MemberCropRepository memberCropRepository, CommunityPostQueryRepository communityPostQueryRepository,
                                OpaqueCursorCodec cursorCodec) {
        this.memberRepository = Objects.requireNonNull(memberRepository, "memberRepository");
        this.cropRepository = Objects.requireNonNull(cropRepository, "cropRepository");
        this.farmingRecordRepository = Objects.requireNonNull(farmingRecordRepository, "farmingRecordRepository");
        this.uploadedMediaRepository = Objects.requireNonNull(uploadedMediaRepository, "uploadedMediaRepository");
        this.communityPostRepository = Objects.requireNonNull(communityPostRepository, "communityPostRepository");
        this.communityPostMediaRepository = Objects.requireNonNull(communityPostMediaRepository, "communityPostMediaRepository");
        this.communityCommentRepository = Objects.requireNonNull(communityCommentRepository, "communityCommentRepository");
        this.communityPostLikeRepository = Objects.requireNonNull(communityPostLikeRepository, "communityPostLikeRepository");
        this.memberCropRepository = Objects.requireNonNull(memberCropRepository, "memberCropRepository");
        this.communityPostQueryRepository = Objects.requireNonNull(communityPostQueryRepository, "communityPostQueryRepository");
        this.cursorCodec = Objects.requireNonNull(cursorCodec, "cursorCodec");
    }

    @Transactional(readOnly = true)
    public List<CommunityPostResult.Board> listBoards(UUID memberId) {
        Objects.requireNonNull(memberId, "memberId");
        var seen = new LinkedHashSet<UUID>();
        var boards = new ArrayList<CommunityPostResult.Board>();
        for (var memberCrop : memberCropRepository.findByMemberId(memberId)) {
            var crop = memberCrop.getCrop();
            var id = required(crop.getId(), "Persisted crop id is required");
            if (seen.add(id)) boards.add(new CommunityPostResult.Board(id, crop.getName()));
        }
        return boards;
    }

    /** 현재 재배 여부와 관계없이 작성자가 게시글을 작성한 작물을 조회한다. */
    @Transactional(readOnly = true)
    public List<CommunityPostResult.Board> listPostCrops(UUID authorMemberId) {
        Objects.requireNonNull(authorMemberId, "authorMemberId");
        var boards = new ArrayList<CommunityPostResult.Board>();
        for (var crop : communityPostQueryRepository.findDistinctCropsByAuthor(authorMemberId)) {
            boards.add(new CommunityPostResult.Board(required(crop.getId(), "Persisted crop id is required"), crop.getName()));
        }
        return boards;
    }

    @Transactional(readOnly = true)
    public CommunityPostResult.Page search(CommunityPostSearchCondition condition) throws JsonProcessingException {
        Objects.requireNonNull(condition, "condition");
        if (condition.getMemberId() == null && (condition.getLikedOnly() || condition.getMineOnly())) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        validatePageSize(condition.getSize());
        var cursor = decodeCursor(condition.getSort(), condition.getCursor());
        var result = communityPostQueryRepository.search(new CommunityPostQueryRepository.SearchCondition(
            condition.getMemberId(), condition.getAuthorMemberId(), condition.getCropIds(), condition.getPostType(), condition.getKeyword(),
            condition.getLikedOnly(), condition.getMineOnly(), condition.getSort(), cursor, condition.getSize() + 1));
        var visible = new ArrayList<>(result.rows().subList(0, Math.min(condition.getSize(), result.rows().size())));
        var nextCursor = result.rows().size() > condition.getSize() && !visible.isEmpty() ? encodeCursor(condition.getSort(), visible.getLast()) : null;
        var items = new ArrayList<CommunityPostResult.PostSummary>();
        for (var row : visible) items.add(toSummary(row));
        return new CommunityPostResult.Page(items, nextCursor);
    }

    @Transactional(readOnly = true)
    public long count(CommunityPostSearchCondition condition) {
        Objects.requireNonNull(condition, "condition");
        return communityPostQueryRepository.count(new CommunityPostQueryRepository.SearchCondition(
            condition.getMemberId(), condition.getAuthorMemberId(), condition.getCropIds(), condition.getPostType(), condition.getKeyword(),
            condition.getLikedOnly(), condition.getMineOnly(), condition.getSort(), null, COUNT_QUERY_SIZE));
    }

    private void validatePageSize(int size) {
        if (size <= 0 || size == Integer.MAX_VALUE) throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    @Transactional(readOnly = true)
    public CommunityPostResult.PostDetail getDetail(@Nullable UUID memberId, UUID postId) {
        Objects.requireNonNull(postId, "postId");
        var post = findPost(postId);
        var imageUrls = new ArrayList<String>();
        for (var media : communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(postId)) imageUrls.add(media.getUploadedMedia().getFileUrl());
        return new CommunityPostResult.PostDetail(required(post.getId(), "Persisted post id is required"),
            required(post.getCrop().getId(), "Persisted crop id is required"), post.getCrop().getName(), post.getPostType(), post.getTitle(), post.getBody(),
            imageUrls, post.getFarmingRecord() == null ? null : post.getFarmingRecord().getId(), authorOf(post.getAuthor()),
            communityCommentRepository.countByPostIdAndIsDeletedFalse(postId), communityPostLikeRepository.countByPostId(postId),
            memberId != null && communityPostLikeRepository.existsByPostIdAndMemberId(postId, memberId), post.getCreatedAt());
    }

    public CommunityPostResult.PostId create(CommunityPostCommand.Create command) {
        Objects.requireNonNull(command, "command");
        var member = findMember(command.getMemberId());
        var crop = findCrop(command.getCropId());
        var record = resolveFarmingRecord(command.getMemberId(), command.getCropId(), command.getFarmingRecordId());
        var media = validateNewMedia(command.getMemberId(), command.getMediaIds());
        var post = communityPostRepository.save(new CommunityPost(null, member, crop, record, command.getPostType(), command.getTitle(), command.getBody(), false));
        attachMedia(post, media);
        return CommunityPostResult.idOf(post);
    }

    public CommunityPostResult.PostId update(CommunityPostCommand.Update command) {
        Objects.requireNonNull(command, "command");
        var post = findPost(command.getPostId());
        assertAuthor(post, command.getMemberId());
        var crop = findCrop(command.getCropId());
        var record = resolveFarmingRecord(command.getMemberId(), command.getCropId(), command.getFarmingRecordId());
        var existingMedia = communityPostMediaRepository.findByPostIdOrderByDisplayOrderAsc(command.getPostId());
        var media = validateUpdatedMedia(post, command.getMemberId(), command.getMediaIds());
        post.update(crop, record, command.getPostType(), command.getTitle(), command.getBody());
        syncMedia(post, existingMedia, media);
        return CommunityPostResult.idOf(post);
    }

    public void delete(CommunityPostCommand.Delete command) {
        Objects.requireNonNull(command, "command");
        var post = findPost(command.getPostId());
        assertAuthor(post, command.getMemberId());
        post.softDelete();
    }

    public CommunityPostResult.LikeToggle toggleLike(CommunityPostCommand.ToggleLike command) {
        Objects.requireNonNull(command, "command");
        var post = findPost(command.getPostId());
        var existing = communityPostLikeRepository.findByPostIdAndMemberId(command.getPostId(), command.getMemberId());
        boolean liked;
        if (existing == null) {
            var member = findMember(command.getMemberId());
            communityPostLikeRepository.save(new CommunityPostLike(post, member));
            liked = true;
        } else {
            communityPostLikeRepository.delete(existing);
            liked = false;
        }
        return new CommunityPostResult.LikeToggle(liked, communityPostLikeRepository.countByPostId(command.getPostId()));
    }

    private List<UploadedMedia> validateNewMedia(UUID memberId, List<UUID> mediaIds) {
        validateDistinctMediaIds(mediaIds);
        if (mediaIds.isEmpty()) return List.of();
        var byId = findMediaById(mediaIds);
        var result = new ArrayList<UploadedMedia>();
        for (var id : mediaIds) {
            var media = byId.get(id);
            if (media == null) throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
            validateMediaOwnerAndUsage(memberId, media);
            if (!media.isAttachable()) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            result.add(media);
        }
        return result;
    }

    private List<UploadedMedia> validateUpdatedMedia(CommunityPost post, UUID memberId, List<UUID> mediaIds) {
        validateDistinctMediaIds(mediaIds);
        if (mediaIds.isEmpty()) return List.of();
        var byId = findMediaById(mediaIds);
        var attachedById = new LinkedHashMap<UUID, CommunityPostMedia>();
        for (var row : communityPostMediaRepository.findByUploadedMediaIdIn(mediaIds)) attachedById.put(required(row.getUploadedMedia().getId(),
            "Persisted media id is required"), row);
        var postId = required(post.getId(), "Persisted post id is required");
        var result = new ArrayList<UploadedMedia>();
        for (var id : mediaIds) {
            var media = byId.get(id);
            if (media == null) throw new BusinessException(ErrorCode.MEDIA_NOT_FOUND);
            validateMediaOwnerAndUsage(memberId, media);
            var attached = attachedById.get(id);
            if (attached != null && !Objects.equals(attached.getPost().getId(), postId)) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            if (!media.isAttachable() && media.getStatus() != UploadedMediaStatus.ATTACHED) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            if (media.getStatus() == UploadedMediaStatus.ATTACHED && !Objects.equals(attached == null ? null : attached.getPost().getId(),
                postId)) throw new BusinessException(ErrorCode.MEDIA_NOT_ATTACHABLE);
            result.add(media);
        }
        return result;
    }

    private void validateDistinctMediaIds(List<UUID> ids) {
        if (ids.size() != new LinkedHashSet<>(ids).size()) throw new BusinessException(ErrorCode.INVALID_INPUT);
    }

    private Map<UUID, UploadedMedia> findMediaById(List<UUID> ids) {
        var byId = new LinkedHashMap<UUID, UploadedMedia>();
        for (var media : uploadedMediaRepository.findAllById(ids)) byId.put(required(media.getId(), "Persisted media id is required"), media);
        return byId;
    }

    private void validateMediaOwnerAndUsage(UUID memberId, UploadedMedia media) {
        if (!Objects.equals(media.getOwner().getId(), memberId)) throw new BusinessException(ErrorCode.MEDIA_NOT_OWNED);
        if (media.getUsageType() != UploadedMediaUsageType.COMMUNITY_POST) throw new BusinessException(ErrorCode.MEDIA_USAGE_MISMATCH);
    }

    private FarmingRecord resolveFarmingRecord(UUID memberId, UUID cropId, UUID recordId) {
        if (recordId == null) return null;
        var record = farmingRecordRepository.findByIdAndMember_Id(recordId, memberId);
        if (record == null) throw new BusinessException(ErrorCode.FARMING_RECORD_NOT_FOUND);
        if (!Objects.equals(record.getCrop().getId(), cropId)) throw new BusinessException(ErrorCode.COMMUNITY_FARMING_RECORD_CROP_MISMATCH);
        return record;
    }

    private void assertAuthor(CommunityPost post, UUID memberId) {
        if (!Objects.equals(post.getAuthor().getId(), memberId)) throw new BusinessException(ErrorCode.COMMUNITY_FORBIDDEN);
    }

    private void attachMedia(CommunityPost post, List<UploadedMedia> media) {
        if (media.isEmpty()) return;
        media.forEach(UploadedMedia::markAttached);
        var rows = new ArrayList<CommunityPostMedia>();
        for (int i = 0; i < media.size(); i++) rows.add(new CommunityPostMedia(post, media.get(i), i));
        communityPostMediaRepository.saveAll(rows);
    }

    private void syncMedia(CommunityPost post, List<CommunityPostMedia> existing, List<UploadedMedia> media) {
        var finalIds = new LinkedHashSet<UUID>();
        for (var item : media) finalIds.add(required(item.getId(), "Persisted media id is required"));
        var removed = new ArrayList<CommunityPostMedia>();
        for (var row : existing) {
            if (!finalIds.contains(required(row.getUploadedMedia().getId(), "Persisted media id is required"))) removed.add(row);
        }
        for (var row : removed) row.getUploadedMedia().markDeleted();
        communityPostMediaRepository.deleteByPost(post);
        communityPostMediaRepository.flush();
        attachMedia(post, media);
    }

    private CommunityPost findPost(UUID id) {
        var post = communityPostRepository.findByIdAndIsDeletedFalse(id);
        if (post == null) throw new BusinessException(ErrorCode.COMMUNITY_POST_NOT_FOUND);
        return post;
    }
    private Member findMember(UUID id) { return memberRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND)); }
    private Crop findCrop(UUID id) { return cropRepository.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.CROP_NOT_FOUND)); }

    private CommunityPostQueryRepository.Cursor decodeCursor(CommunityPostSort sort, String cursor) {
        if (cursor == null || cursor.chars().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c))) return null;
        var payload = cursorCodec.decode(cursor, CommunityPostCursorPayload.class);
        if (payload.getSort() != sort || (sort != CommunityPostSort.LATEST && payload.getScore() == null)) throw new BusinessException(ErrorCode.INVALID_CURSOR);
        return new CommunityPostQueryRepository.Cursor(payload.getSort(), payload.getScore(), payload.getCreatedAt(), payload.getId());
    }

    private String encodeCursor(CommunityPostSort sort, CommunityPostQueryRepository.Row row) throws JsonProcessingException {
        var post = row.post();
        var score = sort == CommunityPostSort.LATEST ? null : row.score();
        if (sort != CommunityPostSort.LATEST && score == null) throw new IllegalStateException("Cursor score is required for " + sort + " sort");
        return cursorCodec.encode(new CommunityPostCursorPayload(sort, score, post.getCreatedAt(), required(post.getId(), "Persisted post id is required")));
    }

    private CommunityPostResult.PostSummary toSummary(CommunityPostQueryRepository.Row row) {
        var post = row.post();
        return new CommunityPostResult.PostSummary(required(post.getId(), "Persisted post id is required"), required(post.getCrop().getId(),
            "Persisted crop id is required"),
            post.getCrop().getName(), post.getPostType(), post.getTitle(), post.getBody().substring(0, Math.min(BODY_PREVIEW_LENGTH, post.getBody().length())),
            row.thumbnailUrl(), authorOf(post.getAuthor()), row.commentCount(), row.likeCount(), row.likedByMe(), post.getCreatedAt());
    }

    private CommunityPostResult.AuthorSummary authorOf(Member member) {
        return new CommunityPostResult.AuthorSummary(required(member.getId(), "Persisted member id is required"), member.getNickname(),
            member.getProfileMedia() == null ? null : member.getProfileMedia().getFileUrl());
    }
    private static <T> T required(T value, String message) {
        if (value == null) throw new IllegalArgumentException(message);
        return value;
    }
}
