import { ColumnProps } from "antd/lib/table";
import { filtersTransformationUtilityType } from "dashboard/dashboard-types/Dashboard.types";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { timeBoundFilterKeys } from "dashboard/graph-filters/components/helper";
import { genericDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { JiraReleaseTableKeyValue } from "dashboard/pages/dashboard-tickets/configs/jiraReleaseTableConfig";
import { set } from "lodash";
import { tableCell } from "utils/tableUtils";


export const mapFiltersBeforeCall = (filter: any, filterValue: any) => {
  set(filter, ["filter", "excludeVelocityStages"], filterValue);
  return filter;
}

export const dataTransformFunction = (data: any) => {
  const { apiData } = data;
  return { data: apiData };
};

export const JiraReleaseReportCSVFiltersTransformer = (filters: basicMappingType<any>, utilityConfig: filtersTransformationUtilityType) => {

  delete filters.filter?.page
  delete filters.filter?.page_size

  return {
    ...filters,
    "ou_ids": [utilityConfig.queryParam?.OU],
    "widget_id": utilityConfig.widgetId
  }
}

export const JiraReleaseReportCSVColumn = (data: Array<basicMappingType<any>> | basicMappingType<any>) => {
  return JiraReleaseTableKeyValue;
}

export const JiraReleaseReportCSVDataTransformer = (data: Array<basicMappingType<any>> | basicMappingType<any>, columns: ColumnProps<any>[]) => {
  return (data || []).map((record: any) => {
    return [...(columns || [])]
      .map((col: any) => {

        let result = record[col.key];
        if (result === undefined) {
          return result;
        }

        if (col.key === "average_lead_time") {
          return `${(result / 86400).toFixed(1)} days`;
        }

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

        if (
          timeBoundFilterKeys.includes(col.key) ||
          col.key?.includes("created") ||
          col.key?.includes("updated") ||
          col.key?.includes("modify")
        ) {
          return tableCell("created_at", result);
        }

        return result;
      })
      .join(",");
  });

}

export const onChartClickHandler = (records: any) => {
  return { "fix_versions": [records?.name] };
}

export const JiraReleaseDrillDownTransformer = (drillDownData: any) => {
  const { drillDownProps } = drillDownData;
  const { filters } = genericDrilldownTransformer(drillDownData);
  let finalFilters = {
    filter: {
      ...filters.filter,
      ...drillDownProps[drillDownProps.application],
    }
  };
  return { filters: finalFilters };
};

export const getJiraReleaseDrilldownTitle = (params: any) => {
  const { drillDownProps } = params;
  return drillDownProps[drillDownProps.application]?.fix_versions[0];  
}

export const getJiraReleaseDrilldownType = (params: any) => {
  return 'Release Version';  
}