package com.chamchamcham.application.coaching.recordfeedback.generation;

public final class CommonFeedbackDetail implements RecordFeedbackWorkDetail {
    public static final CommonFeedbackDetail INSTANCE = new CommonFeedbackDetail();
    private CommonFeedbackDetail() {}
    @Override public boolean equals(Object other) { return other instanceof CommonFeedbackDetail; }
    @Override public int hashCode() { return "com.chamchamcham.application.coaching.recordfeedback.generation.CommonFeedbackDetail".hashCode(); }
    @Override public String toString() { return "CommonFeedbackDetail"; }
}
