package io.levelops.aggregations.models;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import io.levelops.aggregations.models.jenkins.ConfigChangesByJob;
import io.levelops.aggregations.models.jenkins.ConfigChangesByUser;
import io.levelops.aggregations.models.jenkins.JobRunsByJob;
import io.levelops.aggregations.models.jenkins.JobRunsByUser;
import io.levelops.aggregations.models.jenkins.JobStats;
import lombok.Getter;
import lombok.Setter;
import lombok.Singular;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Setter
@Getter
@ToString
@SuperBuilder(toBuilder = true)
@JsonDeserialize(builder = JenkinsMonitoringAggData.JenkinsMonitoringAggDataBuilderImpl.class)
public class JenkinsMonitoringAggData extends AggData {
    @JsonProperty("config_changes_by_jobs")
    private List<ConfigChangesByJob> configChangesByJobs;
    @JsonProperty("config_changes_by_users")
    private List<ConfigChangesByUser> configChangesByUsers;
    @JsonProperty("config_changes_by_jobs_time_series")
    private Map<Long,List<ConfigChangesByJob>> configChangesByJobsTimeSeries;
    @JsonProperty("config_changes_by_users_time_series")
    private Map<Long,List<ConfigChangesByUser>> configChangesByUsersTimeSeries;

    @JsonProperty("job_runs_by_jobs")
    private List<JobRunsByJob> jobRunsByJobs;
    @JsonProperty("job_runs_by_users")
    private List<JobRunsByUser> jobRunsByUsers;
    @JsonProperty("job_runs_by_jobs_time_series")
    private Map<Long,List<JobRunsByJob>> jobRunsByJobsTimeSeries;
    @JsonProperty("job_runs_by_users_time_series")
    private Map<Long,List<JobRunsByUser>> jobRunsByUsersTimeSeries;

    @JsonProperty("job_stats")
    @Singular
    private List<JobStats> jobStats;
    @JsonProperty("job_stats_time_series")
    private Map<Long,List<JobStats>> jobStatsTimeSeries;

    @JsonPOJOBuilder(withPrefix = "")
    static final class JenkinsMonitoringAggDataBuilderImpl extends JenkinsMonitoringAggData.JenkinsMonitoringAggDataBuilder<JenkinsMonitoringAggData, JenkinsMonitoringAggData.JenkinsMonitoringAggDataBuilderImpl> {
    }

}