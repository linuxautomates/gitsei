import React from "react";
import { timeFilterKey } from "./helper";
import { getMaxRangeFromReportType } from "dashboard/graph-filters/components/utils/getMaxRangeFromReportType";
import { JIRA_SPRINT_REPORTS } from "../../constants/applications/names";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "./time-range-abs-rel-wrapper.component";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface JiraIssueTimeFilterComponentProps {
  filters: any;
  application: string;
  onFilterValueChange: (value: any, type?: any, rangeType?: string) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  metaData?: any;
  reportType: string;
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
}

const JiraIssueTimeFilterComponent: React.FC<JiraIssueTimeFilterComponentProps> = (
  props: JiraIssueTimeFilterComponentProps
) => {
  const {
    filters,
    onFilterValueChange,
    application,
    onRangeTypeChange,
    metaData,
    reportType,
    onMetadataChange,
    dashboardMetaData
  } = props;
  const jirasalesforcereport =
    application === IntegrationTypes.JIRA_SALES_FORCE && reportType === "jira_salesforce_report";

  const maxRange = getMaxRangeFromReportType(reportType);

  return (
    <>
      <TimeRangeAbsoluteRelativeWrapperComponent
        key={"issue_created_at"}
        label={!jirasalesforcereport ? "Issue Created in" : "SALESFORCE ISSUE CREATED DATE"}
        filterKey={timeFilterKey(application, !jirasalesforcereport ? "issue_created_at" : "created_at", reportType)}
        metaData={metaData}
        filters={filters}
        onFilterValueChange={onFilterValueChange}
        onMetadataChange={onMetadataChange}
        onTypeChange={onRangeTypeChange}
        maxRange={maxRange}
        dashboardMetaData={dashboardMetaData}
      />
      {reportType !== "jira_zendesk_report" && (
        <TimeRangeAbsoluteRelativeWrapperComponent
          key={"issue_updated_at"}
          label={!jirasalesforcereport ? "Issue Updated in" : "SALESFORCE ISSUE UPDATED DATE"}
          filterKey={timeFilterKey(application, !jirasalesforcereport ? "issue_updated_at" : "updated_at", reportType)}
          metaData={metaData}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          onMetadataChange={onMetadataChange}
          onTypeChange={onRangeTypeChange}
          maxRange={maxRange}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {["jira_backlog_trend_report"].includes(reportType) && (
        <TimeRangeAbsoluteRelativeWrapperComponent
          key={"snapshot_time_range"}
          label={"Snapshot Time Range"}
          filterKey={timeFilterKey(application, "snapshot_range", reportType)}
          metaData={metaData}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          onMetadataChange={onMetadataChange}
          onTypeChange={onRangeTypeChange}
          maxRange={maxRange}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {Object.values(JIRA_SPRINT_REPORTS).includes(reportType as any) && (
        <TimeRangeAbsoluteRelativeWrapperComponent
          key="sprint_end_date"
          label="Sprint End Date"
          filterKey={timeFilterKey(application, "completed_at", reportType)}
          metaData={metaData}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          onMetadataChange={onMetadataChange}
          onTypeChange={onRangeTypeChange}
          maxRange={maxRange}
          dashboardMetaData={dashboardMetaData}
          required
        />
      )}
      {["resolution_time_report", "jira_time_across_stages"].includes(reportType) && (
        <TimeRangeAbsoluteRelativeWrapperComponent
          key="last_closed_date"
          label="Last Closed Date"
          filterKey={timeFilterKey(application, "issue_resolved_at", reportType)}
          metaData={metaData}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          onMetadataChange={onMetadataChange}
          onTypeChange={onRangeTypeChange}
          maxRange={maxRange}
          required={true}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {["tickets_report"].includes(reportType) && (
        <TimeRangeAbsoluteRelativeWrapperComponent
          key="last_closed_date"
          label="Jira Due Date"
          filterKey={timeFilterKey(application, "issue_due_at", reportType)}
          metaData={metaData}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          onMetadataChange={onMetadataChange}
          onTypeChange={onRangeTypeChange}
          maxRange={maxRange}
          required={true}
          dashboardMetaData={dashboardMetaData}
        />
      )}
    </>
  );
};

export default JiraIssueTimeFilterComponent;
