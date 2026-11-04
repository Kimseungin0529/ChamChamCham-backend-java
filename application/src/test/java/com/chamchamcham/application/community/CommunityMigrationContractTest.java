package com.chamchamcham.application.community;

import com.chamchamcham.application.common.OpaqueCursorCodec;
import com.chamchamcham.domain.community.CommunityPostSort;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.kotlin.KotlinModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDateTime;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class CommunityMigrationContractTest {
    private final UUID id = UUID.fromString("00000000-0000-0000-0000-000000000101");
    private final LocalDateTime now = LocalDateTime.of(2026, 7, 17, 9, 0);
    private ObjectMapper mapper() { return new ObjectMapper().registerModule(new KotlinModule.Builder().build()).registerModule(new JavaTimeModule()); }
    @Test
    void cursorLiteralJsonAndNullableScore() throws Exception {
        var codec = new OpaqueCursorCodec();
        var post = new CommunityPostCursorPayload(CommunityPostSort.LATEST, null, now, id);
        var json = new String(Base64.getUrlDecoder().decode(codec.encode(post)), java.nio.charset.StandardCharsets.UTF_8);
        assertThat(json).isEqualTo("{\"sort\":\"LATEST\",\"score\":null,\"createdAt\":\"2026-07-17T09:00:00\",\"id\":\"00000000-0000-0000-0000-000000000101\"}");
        assertThat(codec.decode(codec.encode(post), CommunityPostCursorPayload.class)).isEqualTo(post);
        var comment = new CommunityCommentCursorPayload(now, id);
        assertThat(codec.decode(codec.encode(comment), CommunityCommentCursorPayload.class)).isEqualTo(comment);
    }
    @Test
    void defaultCollectionOnlyAppliesToMissingJsonProperty() throws Exception {
        var mapper = mapper();
        var condition = mapper.readValue("{\"likedOnly\":false,\"mineOnly\":false,\"sort\":\"LATEST\",\"size\":20}", CommunityPostSearchCondition.class);
        assertThat(condition.getCropIds()).isEmpty();
        assertThat(condition.getAuthorMemberId()).isNull();
        assertThatThrownBy(() -> mapper.readValue("{\"cropIds\":null,\"sort\":\"LATEST\",\"size\":20}",
            CommunityPostSearchCondition.class)).isInstanceOf(Exception.class);
        var commentJson = "{\"id\":\"" + id + "\",\"author\":{\"memberId\":\"" + id + "\"},\"body\":\"내용\",\"deleted\":false,\"createdAt\":\"2026-07-17T09:00:00\"";
        assertThat(mapper.readValue(commentJson + "}", CommunityCommentResult.Comment.class).getReplies()).isEmpty();
        assertThatThrownBy(() -> mapper.readValue(commentJson + ",\"replies\":null}", CommunityCommentResult.Comment.class)).isInstanceOf(Exception.class);
    }
    @Test
    void dtoCollectionsKeepInputAliasAndValueEquality() {
        var items = new ArrayList<CommunityPostResult.PostSummary>();
        var page = new CommunityPostResult.Page(items, null);
        assertThat(page.getItems()).isSameAs(items);
        assertThat(page).isEqualTo(new CommunityPostResult.Page(new ArrayList<>(), null));
        var board = new CommunityPostResult.Board(id, "황기");
        assertThat(board).isEqualTo(new CommunityPostResult.Board(id, "황기"));
        assertThatThrownBy(() -> new CommunityPostResult.Board(id, null)).isInstanceOf(NullPointerException.class);
    }
}
