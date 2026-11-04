package com.chamchamcham.application.coaching.recordfeedback.lifecycle;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.transaction.event.TransactionalEventListener;
import java.util.*;
import org.junit.jupiter.api.DisplayName;
import com.chamchamcham.application.coaching.reportfeedback.generation.ReportFeedbackContextKt;

class RecordFeedbackPreparationListenerTest {

    @Test
    @DisplayName("record feedback preparation listener is the only async entry point")
    void record_feedback_preparation_listener_is_the_only_async_entry_point() throws Exception {
        var preparationOn = RecordFeedbackPreparationListener.class.getDeclaredMethod("on", RecordFeedbackPreparationRequested.class);
        var generationOn = RecordFeedbackGenerationListener.class.getDeclaredMethod("on", RecordFeedbackGenerationRequested.class);
        assertThat(preparationOn.getAnnotation(Async.class).value()).isEqualTo("coachingTaskExecutor");
        assertThat(generationOn.isAnnotationPresent(Async.class)).isFalse();
        assertThat(generationOn.isAnnotationPresent(EventListener.class)).isTrue();
        assertThat(generationOn.isAnnotationPresent(TransactionalEventListener.class)).isFalse();
    }
}
