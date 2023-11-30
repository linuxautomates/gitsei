import React from "react";
import { timeFilterKey } from "../helper";
import { getMaxRangeFromReportType } from "dashboard/graph-filters/components/utils/getMaxRangeFromReportType";
import { LEAD_TIME_REPORTS } from "dashboard/constants/applications/names";
import { TimeRangeAbsoluteRelativeWrapperComponent } from "../time-range-abs-rel-wrapper.component";

interface LeadTimeFiltersProps {
  filters: any;
  application: string;
  onFilterValueChange: (value: any, type?: any, rangeType?: string) => void;
  onRangeTypeChange: (key: string, value: any) => void;
  metaData?: any;
  reportType: string;
  onMetadataChange?: (value: any, type: any) => void;
  dashboardMetaData?: any;
}

const LeadTimeFilters: React.FC<LeadTimeFiltersProps> = (props: LeadTimeFiltersProps) => {
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

  const maxRange = getMaxRangeFromReportType(reportType);

  return (
    <>
      {[
        LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
        LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
        LEAD_TIME_REPORTS.JIRA_LEAD_TIME_TREND_REPORT,
        LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT
      ].includes(reportType as any) && (
        <TimeRangeAbsoluteRelativeWrapperComponent
          key="ticket_resolved_in"
          label="Ticket Resolved In"
          filterKey={timeFilterKey(application, "issue_resolved_at", reportType)}
          metaData={metaData}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          onTypeChange={onRangeTypeChange}
          onMetadataChange={onMetadataChange}
          maxRange={maxRange}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      {[
        LEAD_TIME_REPORTS.LEAD_TIME_SINGLE_STAT_REPORT,
        LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT,
        LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_TREND_REPORT
      ].includes(reportType as any) && (
        <TimeRangeAbsoluteRelativeWrapperComponent
          key="pr_merged_in"
          label="PR Merged In"
          filterKey={timeFilterKey(application, "pr_merged_at", reportType)}
          metaData={metaData}
          filters={filters}
          onFilterValueChange={onFilterValueChange}
          onTypeChange={onRangeTypeChange}
          onMetadataChange={onMetadataChange}
          maxRange={maxRange}
          dashboardMetaData={dashboardMetaData}
        />
      )}
      <TimeRangeAbsoluteRelativeWrapperComponent
        key="cicd_job_end_date"
        label="CICD Job End Date"
        filterKey={timeFilterKey(application, "cicd_job_run_end_time", reportType)}
        metaData={metaData}
        filters={filters}
        onFilterValueChange={onFilterValueChange}
        onTypeChange={onRangeTypeChange}
        onMetadataChange={onMetadataChange}
        maxRange={maxRange}
        dashboardMetaData={dashboardMetaData}
      />
    </>
  );
};

export default LeadTimeFilters;
