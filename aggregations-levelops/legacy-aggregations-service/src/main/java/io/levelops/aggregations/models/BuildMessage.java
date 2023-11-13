package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

public class BuildMessage implements Comparable<BuildMessage> {
    @JsonProperty("start_time")
    private final long startTime;
    @JsonProperty("result")
    private final String result;
    @JsonProperty("duration")
    private final long duration;
    @JsonProperty("build_number")
    private final long buildNumber;
    @JsonProperty("user_id")
    private final String userId;

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