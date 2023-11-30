import Loader from "components/Loader/Loader";
import { useIntegrationList } from "custom-hooks/useIntegrationList";
import { LEVELOPS_MULTITIME_SERIES_SUPPORTED_REPORTS } from "dashboard/constants/applications/multiTimeSeries.application";
import { getIssueManagementReportType } from "dashboard/graph-filters/components/helper";
import { stringSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { get, isArray, map, uniq } from "lodash";
import React, { useCallback, useMemo } from "react";
import { AntSelect } from "../../../../../shared-resources/components";
import { IntegrationTypes } from "constants/IntegrationTypes";
import widgetConstants from "dashboard/constants/widgetConstants";

interface ReportsDropdownProps {
  mode?: string;
  dashboardId: string;
  widgetType: string;
  className: string;
  value: any;
  onChange: any;
  placeHolder?: string;
}

const MultiTimeSeriesReportsDropdown: React.FC<ReportsDropdownProps> = ({
  mode,
  className,
  value,
  onChange,
  widgetType,
  dashboardId,
  placeHolder = "Select A Report"
}) => {
  const [integrationsLoading, integrationList] = useIntegrationList({ dashboardId }, [dashboardId]);

  const handleReportFilter = useCallback((value: string, option: { label: string; value: string }) => {
    return (option?.label as string)?.toLowerCase().includes(value?.toLowerCase());
  }, []);

  const applications = useMemo(
    () => (isArray(integrationList) ? uniq(map(integrationList || [], data => data.application)) : []),
    [integrationList]
  );

  const reportOptions = useMemo(() => {
    if (applications.includes(IntegrationTypes.JIRA) || applications.includes(IntegrationTypes.AZURE)) {
      return LEVELOPS_MULTITIME_SERIES_SUPPORTED_REPORTS.map((report: string) => {
        const reportConstant = get(widgetConstants, report, undefined);
        if (reportConstant) {
          return { label: reportConstant?.name, value: report };
        }
      })
        .filter((item: any) => !!item)
        .sort(stringSortingComparator("label"));
    }
    return [];
  }, [applications]);

  const handleChange = useCallback(
    (value: string) => {
      if (applications.includes(IntegrationTypes.JIRA)) {
        onChange(value);
        return;
      }
      if (applications.includes(IntegrationTypes.AZURE)) {
        onChange(getIssueManagementReportType(value, IntegrationTypes.AZURE));
        return;
      }
    },
    [applications, onChange]
  );

  const mappedValue = useMemo(() => {
    if (!value) return undefined;
    if (applications.includes(IntegrationTypes.JIRA)) {
      return value;
    }
    if (applications.includes(IntegrationTypes.AZURE)) {
      return getIssueManagementReportType(value, IntegrationTypes.JIRA);
    }
  }, [value, applications]);

  if (integrationsLoading) {
    return <Loader />;
  }

  return (
    <AntSelect
      autoFocus
      mode={mode ? mode : "default"}
      className={className}
      value={mappedValue}
      placeholder={placeHolder}
      options={reportOptions}
      showSearch
      showArrow
      onOptionFilter={handleReportFilter}
      onChange={handleChange}
    />
  );
};

export default MultiTimeSeriesReportsDropdown;
