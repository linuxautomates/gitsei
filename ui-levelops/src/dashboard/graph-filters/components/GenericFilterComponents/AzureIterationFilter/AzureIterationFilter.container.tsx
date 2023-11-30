import { useSprintReportFilters } from "custom-hooks/useSprintReportFilters";
import {
  LEAD_MTTR_DORA_REPORTS,
  allSprintMetricsReport,
  azureIterationSupportableReports
} from "dashboard/constants/applications/names";
import { AZURE_CUSTOM_FIELD_PREFIX, CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getFilterValue } from "helper/widgetFilter.helper";
import { get } from "lodash";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useCallback, useMemo, useState } from "react";
import { baseColumnConfig } from "utils/base-table-config";
import { updateIssueCreatedAndUpdatedFilters } from "../../updateFilter.helper";
import { stringSortingComparator } from "../../sort.helper";
import AzureIterationFilter from "./AzureIterartionFilter";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { workflowProfileDetailSelector } from "reduxConfigs/selectors/workflowProfileByOuSelector";
import queryString from "query-string";
import { IntegrationTypes } from "constants/IntegrationTypes";
interface AzureIterationConatinerProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  filterProps: LevelOpsFilter;
  handleRemoveFilter: any;
  metadata: any;
  handlePartialValueChange: (key: string, value: any) => void;
  onExcludeFilterChange: (key: string, value: boolean) => void;
}

const AzureIterationFilterContainer: React.FC<AzureIterationConatinerProps> = ({
  onFilterValueChange,
  filters,
  handleRemoveFilter,
  filterProps,
  metadata,
  handlePartialValueChange,
  onExcludeFilterChange
}) => {
  const { label, beKey, filterMetaData, allFilters, apiFilterProps, getMappedValue, partialFilterKey, partialKey } =
    filterProps;
  const { reportType, activeIterationKey } = filterMetaData as any;
  const queryParamOU = queryString.parse(location.search).OU as string;
  const workspaceOuProfilestate = useParamSelector(workflowProfileDetailSelector, { queryParamOU });
  const doraProfileIntegrationApplication = useMemo(() => {
    const getdoraProfileApplication = get(
      widgetConstants,
      [reportType as string, "getDoraProfileIntegrationApplication"],
      undefined
    );
    if (getdoraProfileApplication) {
      return getdoraProfileApplication({ workspaceOuProfilestate, reportType: reportType });
    }
  }, [workspaceOuProfilestate, reportType]);
  let application = get(widgetConstants, [reportType as string, "application"], undefined);
  if (application === IntegrationTypes.ANY) {
    application = doraProfileIntegrationApplication;
  }
  const completedAt = useMemo(() => filters.completed_at, [filters]);
  const [activePopupKey, setActivePopupKey] = useState<boolean>(false);
  const partialValue = useMemo(() => {
    return get(allFilters, [partialFilterKey ?? "partial_match", partialKey ?? beKey], {});
  }, [allFilters]);
  const sprintReportFilters = useMemo(() => {
    const updatedFilters = updateIssueCreatedAndUpdatedFilters({ filter: filters }, metadata, reportType);
    const completedAt = get(updatedFilters, ["filter", "completed_at"], undefined);
    if (completedAt) {
      return {
        ...filters,
        completed_at: completedAt
      };
    }

    return filters;
  }, [reportType, filters, metadata]);

  const {
    loading: sprintFiltersLoading,
    error: sprintError,
    sprintApiData
  } = useSprintReportFilters(application, sprintReportFilters, [reportType, completedAt]);

  const getSprintData = useCallback(() => {
    if (azureIterationSupportableReports.includes(reportType as any)) {
      if (
        (reportType as string).includes("azure_") ||
        [...Object.values(LEAD_MTTR_DORA_REPORTS)].includes(reportType)
      ) {
        const values = sprintApiData.map((item: any) => ({
          key: item.name,
          parent_key: item.parent_sprint
        }));
        values.sort(stringSortingComparator());
        return values || [];
      } else if (allSprintMetricsReport.includes(reportType as any)) {
        const values = sprintApiData.map((item: any) => ({
          key: item.name
        }));
        values.sort(stringSortingComparator());
        return [
          {
            "sprint_report@Sprint Report": values || []
          }
        ];
      }
    }
    return [];
  }, [sprintApiData]);

  const columnConfig = useCallback(
    (key: string) => [
      {
        ...baseColumnConfig("Value", key),
        sorter: stringSortingComparator(),
        sortDirections: ["descend", "ascend"]
      }
    ],
    []
  );

  const handleFilterValueChange = (value: any, filterKey: string, exclude?: boolean) => {
    let azureIterationValue = undefined;
    if (filterKey === activeIterationKey) {
      azureIterationValue = value && ["current"];
    } else {
      azureIterationValue = value;
    }
    onFilterValueChange(azureIterationValue, filterKey, exclude);
  };

  const getSelectedValueForCustomTree = (filterKey: string) => {
    const filterValue: string[] = get(filters, [filterProps.parentKey as string, filterKey], []);
    return filterValue.map((val: any) => {
      if (typeof val === "string") {
        return val;
      }
      return val.child;
    });
  };

  const setActiveKey = (value: boolean) => {
    setActivePopupKey(value);
  };
  const onRemoveFilter = () => {
    onFilterValueChange(false, activeIterationKey);
    return handleRemoveFilter?.(beKey);
  };

  const apiFilters: any = useMemo(() => {
    if (apiFilterProps) {
      return apiFilterProps({
        ...filterProps,
        handleRemoveFilter: onRemoveFilter,
        options: getSprintData(),
        handleSwitchValueChange: onExcludeFilterChange
      });
    }
    return {};
  }, [apiFilterProps, filterProps, sprintApiData]);
  const APIFiltersProps = () => {
    return {
      filters: allFilters,
      handleActivePopkey: setActiveKey,
      handleFilterValueChange: handleFilterValueChange,
      handlePartialValueChange: handlePartialValueChange,
      activePopkey: activePopupKey,
      handleRemoveFilter: onRemoveFilter,
      isCustom: beKey.includes(CUSTOM_FIELD_PREFIX) || beKey.includes(AZURE_CUSTOM_FIELD_PREFIX)
    };
  };

  const value = getMappedValue?.({ allFilters, filterProps }) ?? getFilterValue(allFilters, beKey, true)?.value;

  const mappedApiFilterProps = () => {
    return {
      dataKey: beKey,
      selectName: label,
      value: value,
      options: apiFilters.options,
      withSwitchConfig: apiFilters.withSwitch,
      partialValue,
      switchValue: !!apiFilters.withSwitch?.switchValue
    };
  };
  return (
    <AzureIterationFilter
      activePopupKey={activePopupKey}
      apiFilters={apiFilters}
      mappedApiFilterProps={mappedApiFilterProps}
      beKey={filterProps.beKey}
      columnConfig={columnConfig}
      handleFilterValueChange={handleFilterValueChange}
      label={label}
      selectedValue={getSelectedValueForCustomTree(filterProps.beKey)}
      setActivePopupKey={setActiveKey}
      filterProps={filterProps}
      tableHeader="Azure Iteration"
      APIFiltersProps={APIFiltersProps}
      reportType={reportType as string}
    />
  );
};

export default AzureIterationFilterContainer;
