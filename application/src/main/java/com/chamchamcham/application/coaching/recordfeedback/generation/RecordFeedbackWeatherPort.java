package com.chamchamcham.application.coaching.recordfeedback.generation;

public interface RecordFeedbackWeatherPort {
    @org.springframework.lang.NonNull RecordFeedbackLiveWeather fetch(double latitude, double longitude, int limitDays);
}
