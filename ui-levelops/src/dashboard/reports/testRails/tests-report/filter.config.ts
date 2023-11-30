import { MaxRecordsFilterConfig } from "dashboard/report-filters/common/max-records-filter.config";
import { ShowValueOnBarConfig } from "dashboard/report-filters/common/show-value-on-bar.config";
import { TestrailsCommonFiltersConfig } from "dashboard/report-filters/testrails/common-filters.config";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { TESTRAILS_ACROSS_OPTIONS } from "../commonTestRailsReports.constants";
import { AcrossFilterConfig, MetricFilterConfig, StackFilterConfig } from "./specific-filter.config.constants";
import { capitalize, get, set, unset } from "lodash";
import { generateVisualizationFilterConfig } from "dashboard/report-filters/common/visualization-filter.config";
import { CUSTOM_CHECKBOX_FIELD_TYPE, REMOVE_COLUMN_KEY, VISUALIZATION_OPTIONS } from "./constants";
import { IssueVisualizationTypes } from "dashboard/constants/typeConstants";
import { ReportDrilldownColTransFuncType } from "dashboard/dashboard-types/common-types";

export const TestrailsTestReportFiltersConfig: LevelOpsFilter[] = [
  ...TestrailsCommonFiltersConfig,
  ShowValueOnBarConfig,
  AcrossFilterConfig,
  StackFilterConfig,
  MaxRecordsFilterConfig,
  generateVisualizationFilterConfig(VISUALIZATION_OPTIONS, IssueVisualizationTypes.BAR_CHART),
  MetricFilterConfig
];

export const getDrillDownType = (params: any) => {
  const across = get(params, "across", undefined);
  let lableName = TESTRAILS_ACROSS_OPTIONS.find(data => data.value === across);
  return lableName?.label || capitalize(across.replace(/_/g, " "));
};

export const getTestrailsExtraDrilldownProps = (params: any) => {
  const { widgetId } = params;
  return {
    hasDynamicColumns: true,
    shouldDerive: ["custom_case_fields"],
    derive: true,
    widgetId: widgetId as string,
    scroll: { x: true }
  };
};

export const getCustomColumn = (testrailsCustomField: any) => {
  let customColumn: any[] = [];
  if (testrailsCustomField && testrailsCustomField.length > 0) {
    customColumn = testrailsCustomField
      .filter((data: { field_type: string }) => !["SCENARIO"].includes(data.field_type))
      .map((data: any) => {
        return {
          key: data.field_key,
          dataIndex: data.field_key,
          width: "250px",
          title: data.name,
          hasAcrossBasedTitle: true,
          filedType: data.field_type,
          render: (value: any, record: any) => {
            const customVal = get(record, ["custom_case_fields", data.field_key], undefined);
            if (customVal) {
              if (data.field_type === CUSTOM_CHECKBOX_FIELD_TYPE && typeof customVal === "boolean")
                return customVal ? "True" : "False";
              if (typeof customVal === "string") return customVal;
              if (typeof customVal === "boolean") return customVal.toString();
              if (Array.isArray(customVal)) return customVal.toString();
              return customVal;
            } else if (data.field_type === CUSTOM_CHECKBOX_FIELD_TYPE) {
              return customVal ? "True" : "False";
            }
          }
        };
      });
  }
  return customColumn;
};

export const testRailDrilldownColumnTransformer: ReportDrilldownColTransFuncType = (utilities: any) => {
  let matricFilter = get(utilities, ["filters", "query", "metric"], "");
  let columns = get(utilities, ["columns"], []);

  if (matricFilter === "test_case_count") {
    return columns.filter((data: { key: string }) => !REMOVE_COLUMN_KEY.includes(data.key));
  }
  return columns;
};

export const transformTestRailsReportPrevQuery = (widget: any) => {
  const { query, metadata } = widget;
  const { stacks } = query;
  const { visualization } = metadata;
  let visualizationVal = visualization;

  if (!visualizationVal) {
    // If value is not set, assign it to default value to make it back-ward compatible.
    visualizationVal = IssueVisualizationTypes.PIE_CHART;
    set(widget, ["metadata", "visualization"], visualizationVal);
  }

  if (visualizationVal === IssueVisualizationTypes.PIE_CHART && stacks?.length > 0) {
    unset(widget, ["query", "stacks"]);
  }
  return widget;
};
