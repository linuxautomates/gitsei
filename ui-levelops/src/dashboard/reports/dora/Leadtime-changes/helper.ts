import { ColumnProps } from "antd/lib/table";
import { convertToDay, dynamicColumnPrefix } from "custom-hooks/helpers/leadTime.helper";
import { IM_ADO } from "dashboard/pages/dashboard-drill-down-preview/drilldownColumnsHelper";
import { doraLeadTimeByChangeTimeColumn } from "dashboard/pages/dashboard-tickets/configs/doraMatrixTableConfig";
import { DoraAdvanceSettingsButtonConfig } from "dashboard/report-filters/dora/dora-advanced-setting-button";
import { get, round, uniq, unset } from "lodash";
import { Integration } from "model/entities/Integration";
import { getCICDFilterConfig, IMAzureFiltersConfig, IMJiraFiltersConfig, SCMPRFiltersConfig } from "../filters.config";
import { AcceptanceTimeUnit, WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { LeadTimeByStageFooter } from "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer";
import DrilldownViewMissingCheckbox from "dashboard/reports/jira/lead-time-by-stage-report/DrilldownViewMissingCheckbox";
import { tableCell } from "utils/tableUtils";
import { extractFilterKeys, getCICDSuportedFilters, getOUFilterKeys } from "../helper";
import { OrgUnitDataViewFilterConfig } from "dashboard/report-filters/dora/DoraOrgUnitFilterViewConfig.ts";
import { ProfileFilterViewConfig } from "dashboard/report-filters/dora/DoraProfileFilter.config";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const getFilterKeysToHide = (params: any) => {
  const selectedOU = get(params, "selectedOU", {});
  const OUFilterKeys = getOUFilterKeys(selectedOU);
  const leadTimeFilters = get(params.workspaceProfile, [
    "lead_time_for_changes",
    "filters",
    "lead_time_for_changes",
    "filter"
  ]);

  const filtersToRemove = extractFilterKeys(leadTimeFilters);

  return uniq([...filtersToRemove, ...OUFilterKeys]);
};
const removeCalculationFilter = (filters: any) => {
  return filters.filter((filter: any) => filter.id !== "time_calculation");
};
//This will return filter config based on integration and workflow profile
// this function might be change according to the requirement
export const getFilterConfig = (params: any) => {
  const integrationId = get(params.workspaceProfile, ["lead_time_for_changes", "integration_id"]);
  const integrationState: Integration[] = get(params, ["integrationState"], undefined);

  const applicationObj = integrationState?.find((item: Integration) => {
    return +item?.id === integrationId;
  });

  const lead_time_change_data = get(params.workspaceProfile, [
    "lead_time_for_changes",
    "filters",
    "lead_time_for_changes"
  ]);
  const integrationType = lead_time_change_data?.integration_type;

  switch (integrationType) {
    case "SCM":
      return removeCalculationFilter(SCMPRFiltersConfig);
    case "CICD":
      const supportedFilters = getCICDSuportedFilters(applicationObj?.application || "");
      return getCICDFilterConfig(supportedFilters);
    case "IM":
      if (applicationObj?.application === IntegrationTypes.JIRA) {
        return removeCalculationFilter(IMJiraFiltersConfig);
      }
      if (applicationObj?.application === IntegrationTypes.AZURE) {
        return removeCalculationFilter(IMAzureFiltersConfig);
      }
    default:
      return [OrgUnitDataViewFilterConfig, ProfileFilterViewConfig, DoraAdvanceSettingsButtonConfig];
  }
};

export const getDoraProfileIntegrationType = (params: any) => {
  const { integrations, workspaceOuProfilestate } = params;
  let integrationId = get(workspaceOuProfilestate, ["lead_time_for_changes", "integration_id"], undefined)?.toString();
  let integrationTypeData = (integrations || [])?.find((data: any) => data?.id === integrationId);
  if (integrationTypeData?.application === IntegrationTypes.AZURE) {
    return IM_ADO;
  } else {
    return WorkflowIntegrationType.SCM;
  }
};

export const mapFiltersBeforeCall = (filter: any) => {
  const finalFilters = filter;
  unset(finalFilters, ["filter", "across"]);
  unset(finalFilters, ["across"]);
  unset(finalFilters, ["filter", "activeColumn"]);
  unset(finalFilters, ["filter", "integration_ids"]);
  return finalFilters;
};

export const transformDrilldownRecords = (data: { records: any[] }) => {
  if (!("records" in data)) return [];

  return data.records.map((record: any) => {
    if (record.data && record.data.length) {
      record.data.map(({ key, mean }: any) => {
        record[`${dynamicColumnPrefix}${key}`] = mean;
      });
    }
    return record;
  });
};

export const dynamicColumnsConfigs = (key: string, activeKey?: string) => {
  if (!key.includes(dynamicColumnPrefix)) {
    return;
  }
  const title = key.replace(dynamicColumnPrefix, "");
  return {
    ...doraLeadTimeByChangeTimeColumn(title, key, "seconds"),
    className: activeKey && activeKey.toLowerCase() === title.toLowerCase() ? "active-stage" : "",
    sortDirections: ["descend", "ascend"]
  } as ColumnProps<any>;
};

export const getExtraDrilldownProps = (params: any) => {
  const { widgetId, drillDownProps } = params;
  const activeKey = getActiveColumnKey(drillDownProps);
  return {
    hasDynamicColumns: true,
    renderDynamicColumns: dynamicColumnsConfigs,
    transformRecordsData: transformDrilldownRecords,
    scroll: { x: "fit-content" },
    widgetId: widgetId as string,
    activeColumn: activeKey as string,
    customisedScroll: true
  };
};

export const getSprintMetricSingleStatDrilldownProps = (params: any) => {
  return { scroll: { x: "max-content" } };
};

const getActiveColumnKey = (drillDownProps: any) => {
  const application = get(drillDownProps, "application", "");
  const activeKey = get(drillDownProps, [application, "activeColumn"], "");
  return activeKey;
};
export const getDrilldownTitle = (params: any) => {
  const { drillDownProps } = params;
  const activeKey = getActiveColumnKey(drillDownProps);
  return activeKey === "total" ? "" : activeKey;
};

export const getDrillDownType = () => {
  return "Stage";
};

export const getGraphFilters = (params: any) => {
  const { filters, contextfilters, id } = params;
  const finalFilters = {
    ...filters,
    filter: {
      ...filters?.filter,
      ...contextfilters?.[id]
    }
  };
  unset(finalFilters, ["filter", "across"]);
  return finalFilters;
};

export const getCheckboxValue = (params: any) => {
  const { filters } = params;
  const ratings = get(filters, ["filter", "ratings"], undefined);
  return ratings && ratings.indexOf("missing") > -1;
};

export const handleRatingChange = (params: any) => {
  const { filters, checked } = params;
  const ratings = get(filters, ["filter", "ratings"], undefined);
  if (checked) {
    return {
      ...filters,
      filter: {
        ...filters?.filter,
        ratings: ["missing", "good", "slow", "needs_attention"]
      }
    };
  }
  if (!checked && ratings) {
    const _ratings = ratings.filter((item: string) => item !== "missing");
    return {
      ...filters,
      filter: {
        ...filters?.filter,
        ratings: _ratings.length ? _ratings : ["good", "slow", "needs_attention"]
      }
    };
  }
  return filters;
};

export const getDrilldownFooter = (params: any) => {
  const { filters } = params;
  const activeColumn = get(filters, ["filter", "activeColumn"], "");
  return activeColumn === "total" ? false : LeadTimeByStageFooter;
};

export const getShowTitle = (params: any) => {
  const { drillDownProps } = params;
  const activeColumn = getActiveColumnKey(drillDownProps);
  return activeColumn === "total" ? false : true;
};

export const getDrilldownCheckBox = (params: any) => {
  const { filters } = params;
  const activeColumn = get(filters, ["filter", "activeColumn"], "");
  return activeColumn === "total" ? false : DrilldownViewMissingCheckbox;
};

export const csvTransformer = (data: any) => {
  const { apiData, columns, jsxHeaders } = data;

  let headers = jsxHeaders ?? columns;
  return (apiData || []).map((record: any) => {
    return [...(headers || [])]
      .map((col: any) => {
        let result = record[col.key];
        if (Array.isArray(result)) {
          if (!result.length) return "";
          return `"${result.join(",")}"`;
        }
        if (typeof result === "string") {
          if (result.includes(",")) {
            return `"${result}"`;
          }
          return result;
        }

        if (col.key === "total") {
          const leadTime = round(convertToDay(result, AcceptanceTimeUnit.SECONDS), 1);
          const unit = leadTime > 1 ? "Days" : "Day";
          return `${leadTime} ${unit}`;
        }

        if (col.key === "created_at") {
          return tableCell("updated_on", result);
        }

        if (col.key?.includes(dynamicColumnPrefix)) {
          const dynamicColumn: any = get(record, ["data"], []).find((val: any) => val.key === col.title);
          const stageTime = round(convertToDay(get(dynamicColumn, ["mean"], 0), AcceptanceTimeUnit.SECONDS), 1);
          const unit = stageTime > 1 ? "Days" : "Day";

          return `${stageTime} ${unit}`;
        }

        return result;
      })
      .join(",");
  });
};

export const getDoraProfileIntegrationId = (params: any) => {
  const { workspaceOuProfilestate } = params;
  let integrationId = get(workspaceOuProfilestate, ["lead_time_for_changes", "integration_id"], undefined)?.toString();
  return integrationId;
};
