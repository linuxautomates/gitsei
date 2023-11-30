import React, { useCallback, useMemo } from "react";
import { Form } from "antd";
import { AntSelect } from "../../../shared-resources/components";
import { v1 as uuid } from "uuid";
import {
  AZURE_SPRINT_REPORTS,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  JIRA_SPRINT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT
} from "dashboard/constants/applications/names";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { jiraBAStatReports } from "dashboard/constants/bussiness-alignment-applications/constants";
import {
  azureTicketsSingleStatAcrossOptions,
  defaultTimePeriodOptions,
  ITEM_TEST_ID,
  jiraEffortInvestmentTimeRangeFilterOption,
  jiraTicketsSingleStatAcrossOptions,
  timePeriodTointervalMapping
} from "./Constants";
import { get } from "lodash";

interface StatFiltersProps {
  onFilterValueChange: (value: any, type?: any) => void;
  filters: any;
  chartType: string;
  application: string;
  reportType: string;
}

const StatFiltersComponent: React.FC<StatFiltersProps> = (props: StatFiltersProps) => {
  const { onFilterValueChange, filters, reportType, chartType } = props;

  const getInterval = useCallback(
    (timePeriod: number, interval: any) => {
      return get(timePeriodTointervalMapping, [timePeriod], interval);
    },
    [reportType, filters]
  );

  const getAggregationValue = useCallback(() => {
    if (
      [
        ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
        JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT
      ].includes(reportType as any)
    ) {
      const timePeriod = filters?.time_period;
      const interval = getInterval(timePeriod, filters?.interval) || "day";
      const across = filters?.across || "issue_created";
      if (
        reportType === JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT &&
        !["issue_created", "issue_resolved"].includes(across)
      ) {
        return "issue_created_day";
      } else if (
        reportType === ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT &&
        !["workitem_created_at", "workitem_resolved_at"].includes(across)
      ) {
        return "workitem_created_at_day";
      } else {
        return `${across}_${interval}`;
      }
    } else {
      return filters?.agg_type;
    }
  }, [reportType, filters]);

  const timePeriodTitle = useMemo(() => {
    if (
      [
        JENKINS_REPORTS.JENKINS_COUNT_SINGLE_STAT_REPORT,
        JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT,
        JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_BY_FILE_SINGLE_STAT_REPORT,
        JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT
      ].includes(reportType as any)
    )
      return "Job Start Date";
    return "Time Period";
  }, [reportType]);

  const aggregationOptions = useMemo(() => {
    if (reportType === JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT) {
      return [
        {
          label: "Average Initial Commit to Deploy Time",
          value: "average"
        },
        {
          label: "Median Initial Commit to Deploy Time",
          value: "median"
        }
      ];
    }

    if (reportType === JENKINS_REPORTS.JOBS_DURATION_SINGLE_STAT_REPORT) {
      return [
        {
          label: "Average Job Duration",
          value: "average"
        },
        {
          label: "Median Job Duration",
          value: "median"
        }
      ];
    }

    if (reportType === JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_SINGLE_STAT_REPORT) {
      return [
        {
          label: "Total number of lines changed",
          value: "lines_changed"
        },
        {
          label: "Total number of files changed",
          value: "files_changed"
        }
      ];
    }

    if (reportType === JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT) {
      return jiraTicketsSingleStatAcrossOptions;
    }

    if (reportType === ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT) {
      return azureTicketsSingleStatAcrossOptions;
    }

    return [
      {
        label: "Average",
        value: "average"
      },
      {
        label: "Total",
        value: "total"
      }
    ];
  }, [reportType]);

  const timePeriodOptions = useMemo(() => {
    if (
      [
        jiraBAReportTypes.EFFORT_INVESTMENT_SINGLE_STAT,
        ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT
      ].includes(reportType as any)
    ) {
      return jiraEffortInvestmentTimeRangeFilterOption;
    }

    return defaultTimePeriodOptions;
  }, [reportType]);

  return (
    <>
      {![
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
        jiraBAReportTypes.JIRA_PROGRESS_SINGLE_STAT,
        ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
        JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT,
        JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_SINGLE_STAT_REPORT,
        ISSUE_MANAGEMENT_REPORTS.RESOLUTION_TIME_SINGLE_STAT_REPORT,
        JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT
      ].includes(reportType as any) && (
        <Form.Item
          key={`${ITEM_TEST_ID}-time-period`}
          data-filterselectornamekey={`${ITEM_TEST_ID}-time-period`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-time-period`}
          label={timePeriodTitle}
          required={true}>
          <AntSelect
            dropdownTestingKey={`${ITEM_TEST_ID}-time-period_dropdown`}
            value={filters.time_period}
            mode={"single"}
            options={timePeriodOptions}
            onChange={(value: any, options: any) => onFilterValueChange(value, "time_period")}
          />
        </Form.Item>
      )}
      {![
        ...jiraBAStatReports,
        ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_SINGLE_STAT_REPORT,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_SINGLE_STAT,
        JENKINS_REPORTS.JOBS_CHANGE_VOLUMES_BY_FILE_SINGLE_STAT_REPORT,
        JENKINS_REPORTS.JENKINS_COUNT_SINGLE_STAT_REPORT,
        ISSUE_MANAGEMENT_REPORTS.TICKET_ISSUE_SINGLE_STAT,
        JIRA_MANAGEMENT_TICKET_REPORT.TICKET_ISSUE_SINGLE_STAT
      ].includes(reportType as any) && (
        <Form.Item
          key={`${ITEM_TEST_ID}-aggregation-type`}
          data-filterselectornamekey={`${ITEM_TEST_ID}-aggregation-type`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-aggregation-type`}
          label={"Aggregation Type"}
          required={true}>
          <AntSelect
            dropdownTestingKey={`${ITEM_TEST_ID}-aggregation-type_dropdown`}
            value={getAggregationValue()}
            mode={"single"}
            options={aggregationOptions}
            onChange={(value: any, options: any) => onFilterValueChange(value, "agg_type")}
          />
        </Form.Item>
      )}
    </>
  );
};

export default StatFiltersComponent;
