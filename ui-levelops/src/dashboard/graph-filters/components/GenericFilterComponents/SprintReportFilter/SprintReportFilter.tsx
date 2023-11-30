import { useSprintReportFilters } from "custom-hooks/useSprintReportFilters";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AddWidgetFilterContainerProps } from "../../AddWidgetFilter/AddWidgetFilter.container";
import { updateIssueCreatedAndUpdatedFilters } from "../../updateFilter.helper";
import UniversalSelectFilterWrapper from "../UniversalSelectFilterWrapper";

interface SprintReportFilterProps extends AddWidgetFilterContainerProps {
  filterProps: LevelOpsFilter;
  handleRemoveFilter: (key: string) => void;
  activePopKey?: string;
  handleActivePopkey: (key: string | undefined) => void;
}

const SprintReportFilterWrapper: React.FC<SprintReportFilterProps> = (props: SprintReportFilterProps) => {
  const { filterProps, metadata } = props;
  const { filterMetaData, allFilters } = filterProps;
  const { reportType } = filterMetaData as DropDownData;

  const application = getWidgetConstant(reportType, "application");

  const completedAt = useMemo(() => allFilters.completed_at, [allFilters]);

  const sprintReportFilters = useMemo(() => {
    const updatedFilters = updateIssueCreatedAndUpdatedFilters({ filter: allFilters }, metadata, reportType);
    const completedAt = get(updatedFilters, ["filter", "completed_at"], undefined);
    if (completedAt) {
      return {
        integration_ids: allFilters.integration_ids,
        completed_at: completedAt
      };
    }

    return { integration_ids: allFilters.integration_ids };
  }, [reportType, allFilters, metadata]);

  const { loading, error, sprintApiData } = useSprintReportFilters(application, sprintReportFilters, [
    reportType,
    completedAt
  ]);

  const renderFilter = useMemo(() => {
    return React.createElement(UniversalSelectFilterWrapper, {
      ...props,
      filterProps: { ...props.filterProps, filterMetaData: { ...props.filterProps.filterMetaData, sprintApiData } }
    });
  }, [props, sprintApiData]);

  return renderFilter;
};

export default SprintReportFilterWrapper;
