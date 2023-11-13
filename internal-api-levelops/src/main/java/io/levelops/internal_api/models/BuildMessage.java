package io.levelops.internal_api.models;

import com.fasterxml.jackson.annotation.*;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class BuildMessage implements Comparable<BuildMessage> {
    @JsonProperty("start_time")
    private long startTime;
    @JsonProperty("result")
    private String result;
    @JsonProperty("duration")
    private long duration;
    @JsonProperty("build_number")
    private long buildNumber;
    @JsonProperty("user_id")
    private String userId;

    public BuildMessage(long buildNumber, long startTime, long duration, String result, String userId) {
        this.startTime = startTime;
        this.result = result;
        this.duration = duration;
        this.buildNumber = buildNumber;
        this.userId = userId;
    }

    public long getBuildNumber()  { return buildNumber; }

    public long getStartTime() {
        return startTime;
    }

    public String getResult() {
        return result;
    }

    public long getDuration() { return duration; }

    public String getUserId() {
        return userId;
    }

    public int compare(BuildMessage message1, BuildMessage message2) {
        return (int) (message1.getBuildNumber() - message2.getBuildNumber());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        BuildMessage that = (BuildMessage) o;

        return new EqualsBuilder()
                .append(startTime, that.startTime)
                .append(duration, that.duration)
                .append(buildNumber, that.buildNumber)
                .append(result, that.result)
                .append(userId, that.userId)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(startTime)
                .append(result)
                .append(duration)
                .append(buildNumber)
                .append(userId)
                .toHashCode();
    }

    @Override
    public int compareTo(BuildMessage message) {
        return compare(this, message);
    }

    @Override
    public String toString() {
        return "BuildMessage{" +
                "startTime=" + startTime +
                ", result='" + result + '\'' +
                ", duration=" + duration +
                ", buildNumber=" + buildNumber +
                ", userId='" + userId + '\'' +
                '}';
    }
}