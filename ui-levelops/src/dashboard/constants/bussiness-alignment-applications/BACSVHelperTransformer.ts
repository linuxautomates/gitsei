/**
 *  BA report CSV transformers
 */
import { cloneDeep, forEach, get } from "lodash";
import { getJiraOrFiltersHelper } from "dashboard/components/dashboard-header/helper";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { dynamicURIFuncType, filtersTransformationUtilityType } from "dashboard/dashboard-types/Dashboard.types";
import {
  getSupportedApplications,
  removeFiltersWithEmptyValues
} from "shared-resources/containers/widget-api-wrapper/helper";
import { sanitizeObject } from "utils/commonUtils";
import { genericCSVFiltersTransformer } from "../genericReportCSVTransformer/genericReportFiltersCSVTransformer";
import { getWidgetConstant } from "../widgetConstants";
import {
  ACTIVE_WORK_UNIT_FILTER_KEY,
  alignmentStatusMapping,
  engineerTableEffortTypeToURIMapping,
  TICKET_CATEGORIZATION_UNIT_FILTER_KEY,
  URI_MAPPING,
  uriUnitMapping
} from "./constants";
import { alignmentTableConfig } from "shared-resources/charts/alignment-table/alignmentTableConfig";
import { ColumnProps } from "antd/lib/table";
import { allTimeFilterKeys } from "../../graph-filters/components/helper";
import { valuesToFilters } from "../constants";
import widgetConstants from "../widgetConstants";
import { EffortType } from "../enums/jira-ba-reports.enum";
import { activeWorkBasedReports } from "reduxConfigs/constants/effort-investment.constants";
import { azureIterationSupportableReports } from "../applications/names";
import {
  DEFAULT_CATEGORY_NAME,
  FILTER_FIELD_UNCATEGORIZED_NAME
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { getCommaSeparatedTotalAndPercentage } from "./helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

/** BA CSV Filters transformers */
export const EIAlignmentReportCSVFiltersTransformer = (
  filters: basicMappingType<any>,
  utilityConfig: filtersTransformationUtilityType
) => {
  let tranformedFilters = genericCSVFiltersTransformer(filters, utilityConfig);
  /** Handling jiraOrFilters */
  if (utilityConfig.application === IntegrationTypes.JIRA) {
    const jiraOrFiltersUpdated = getJiraOrFiltersHelper(
      get(utilityConfig.dashboardMetadata, ["jira_or_query"], {}),
      "jira_filter_values"
    );

    if (Object.keys(jiraOrFiltersUpdated || {}).length > 0) {
      tranformedFilters = {
        ...tranformedFilters,
        filter: {
          ...(tranformedFilters.filter || {}),
          or: removeFiltersWithEmptyValues(jiraOrFiltersUpdated)
        }
      };
    }
  }

  /**
   * handling OU filters
   */

  const ou_ids = utilityConfig?.queryParam?.OU
    ? [utilityConfig?.queryParam?.OU]
    : get(utilityConfig.dashboardMetadata ?? {}, "ou_ids", []);
  if (ou_ids.length) {
    let combinedOUFilters = {
      ...get(utilityConfig.dashboardMetadata ?? {}, "ou_user_filter_designation", {}),
      ...sanitizeObject(get(utilityConfig.widgetMetadata ?? {}, "ou_user_filter_designation", {}))
    };

    tranformedFilters = {
      ...tranformedFilters,
      ou_ids
    };

    const supportedApplications = getSupportedApplications(utilityConfig.report);

    forEach(Object.keys(combinedOUFilters), (key: string) => {
      if (!supportedApplications.includes(key)) {
        delete combinedOUFilters?.[key];
      }
    });

    let sprint: string | undefined = "";

    if (azureIterationSupportableReports.includes(utilityConfig.report as any)) {
      sprint = "sprint_report";
    } else {
      const sprintCustomField = (utilityConfig.supportedCustomFields ?? []).find((item: any) =>
        (item.name || "").toLowerCase().includes("sprint")
      );
      if (sprintCustomField) {
        sprint = sprintCustomField.field_key;
      }
    }

    combinedOUFilters = {
      ...combinedOUFilters,
      sprint: [sprint]
    };

    if (
      combinedOUFilters?.hasOwnProperty("sprint") &&
      (!combinedOUFilters?.sprint || !combinedOUFilters?.sprint?.[0])
    ) {
      delete combinedOUFilters?.sprint;
    }

    if (Object.keys(combinedOUFilters).length) {
      tranformedFilters["ou_user_filter_designation"] = combinedOUFilters;
    }

    const across = tranformedFilters?.across;

    const widgetConstantFilterValue = get(
      widgetConstants,
      [utilityConfig.report || "", "valuesToFilters", across],
      undefined
    );
    const filterValue = widgetConstantFilterValue || get(valuesToFilters, [across], across);

    if (filterValue && !allTimeFilterKeys.includes(across)) {
      tranformedFilters["ou_exclusions"] = [filterValue];
    }
  }

  return sanitizeObject(tranformedFilters);
};

/** BA URI Transformer */
export const EIDynamicURITransformer: dynamicURIFuncType = (
  filters: basicMappingType<any>,
  metadata: basicMappingType<any>,
  reportType: string
) => {
  let newUri = getWidgetConstant(reportType, "uri");
  const unitFilterValue = get(filters, ["filter", TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
  const effortType = get(metadata, ["effort_type"], undefined);

  if (effortType && unitFilterValue) {
    if (effortType === EffortType.ACTIVE_EFFORT) {
      return get(filters, ["filter", ACTIVE_WORK_UNIT_FILTER_KEY]);
    } else {
      return get(engineerTableEffortTypeToURIMapping, [effortType, unitFilterValue]);
    }
  }

  if (unitFilterValue) {
    const URIMapping = getWidgetConstant(reportType, URI_MAPPING);
    if (URIMapping) {
      newUri = get(URIMapping, unitFilterValue, unitFilterValue);
    } else {
      newUri = get(uriUnitMapping, unitFilterValue, unitFilterValue);
    }
  }

  if (activeWorkBasedReports.includes(reportType)) {
    newUri = get(filters, ["filter", ACTIVE_WORK_UNIT_FILTER_KEY]);
  }

  return newUri;
};

/** BA reports column get functions */
export const EIAlignmentReportCSVColumns = (data: Array<basicMappingType<any>> | basicMappingType<any>) => {
  return [...alignmentTableConfig].map(column => {
    const clonnedColumn = cloneDeep(column);
    if (!clonnedColumn.title) {
      if (clonnedColumn.key === "slider-score") {
        clonnedColumn.title = "Score";
      } else if (clonnedColumn.key === "alignment_status") {
        clonnedColumn.title = "Status";
      }
    }
    return clonnedColumn;
  });
};

export const EIEngineerReportCSVColumns = (data: Array<basicMappingType<any>> | basicMappingType<any>) => {
  const categories = ((data as basicMappingType<any>).categories || []).map((category: string) => ({
    key: category,
    title: category
  }));
  return [{ key: "engineer", title: "Engineer" }, ...(categories || [])];
};

/** used for getting columns for CSV of trend report. */
export const EITrendReportCSVColumns = (data: Array<basicMappingType<any>> | basicMappingType<any>) => {
  const categories = ((data as basicMappingType<any>).categories || []).map((category: { name: string }) => {
    let colTitle: string = category?.name;
    if (colTitle === FILTER_FIELD_UNCATEGORIZED_NAME) {
      colTitle = DEFAULT_CATEGORY_NAME;
    }
    return {
      key: colTitle,
      title: colTitle
    };
  });
  return [{ key: "time_ranges", title: "" }, ...(categories || [])];
};

/** BA reports CSV data transformers */

export const EIEngineerCSVDataTransformer = (
  data: Array<basicMappingType<any>> | basicMappingType<any>,
  columns: ColumnProps<any>[]
) => {
  const apiData: Array<basicMappingType<any>> = (data as basicMappingType<any>)?.apidata || [];
  return apiData.map(record => {
    return columns
      .map(column => {
        switch (column?.key) {
          case "engineer":
            return record?.engineer;
          default:
            const allocationConfig: basicMappingType<number> = record?.allocation_summary || {};
            return ["Remaining Allocation"].includes(column?.key as string)
              ? `${allocationConfig?.[column?.key || ""] ?? "0.00"}%`
              : getCommaSeparatedTotalAndPercentage(allocationConfig?.[column?.key || ""]);
        }
      })
      .join(",");
  });
};

export const EITrendReportCSVDataTransformer = (
  data: Array<basicMappingType<any>> | basicMappingType<any>,
  columns: ColumnProps<any>[]
) => {
  const apiData: Array<basicMappingType<any>> = (data as basicMappingType<any>)?.records || [];
  const transformedData = apiData
    .filter(record => !!record?.name)
    .map(record => {
      return columns
        .map(column => {
          switch (column?.key) {
            case "time_ranges":
              return record?.name;
            default:
              return `${(record?.[column?.key ?? ""] ?? 0).toFixed(2)}%`;
          }
        })
        .join(",");
    });
  return transformedData;
};

export const EIAlignmentReportCSVDataTransformer = (
  data: Array<basicMappingType<any>> | basicMappingType<any>,
  columns: ColumnProps<any>[]
) => {
  const categories: Array<basicMappingType<any>> = (data as basicMappingType<any>).categories ?? [];
  const transformedData = categories.map(category => {
    const config = get(category, ["config"], {});
    return columns
      .map(column => {
        switch (column?.key) {
          case "name":
            return category?.name;
          case "ideal_range":
            return `${config?.ideal_range?.min}% - ${config?.ideal_range?.max}%`;
          case "alignment_status":
            return (alignmentStatusMapping as any)[config?.alignment_score ?? 0];
          case "slider-score":
            const score = (config?.allocation ?? 0) * 100;
            return `${Math.round(score)}%`;
        }
      })
      .join(",");
  });
  return transformedData;
};
