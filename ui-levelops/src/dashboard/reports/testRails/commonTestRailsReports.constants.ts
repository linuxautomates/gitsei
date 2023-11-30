import { IntegrationTypes } from "constants/IntegrationTypes";
import { DrillDownType } from "dashboard/constants/drilldown.constants";
import { testrailsSupportedFilters } from "dashboard/constants/supported-filters.constant";
import { IssueVisualizationTypes, SCMVisualizationTypes } from "dashboard/constants/typeConstants";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { genericDrilldownTransformer } from "dashboard/helpers/drilldown-transformers";
import { TestrailsTableConfig } from "dashboard/pages/dashboard-tickets/configs";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { makeObjectKeyAsValue } from "utils/commonUtils";

export const testRailsDrilldown: DrillDownType = {
  title: "Testrail Tests",
  uri: "testrails_tests_list",
  application: IntegrationTypes.TESTRAILS,
  columns: TestrailsTableConfig,
  supported_filters: testrailsSupportedFilters,
  drilldownTransformFunction: (data: any) => genericDrilldownTransformer(data)
};

export const TESTRAILS_COMMON_FILTER_KEY_MAPPING: Record<string, string> = {
  type: "types",
  project: "projects",
  status: "statuses",
  milestone: "milestones",
  test_plan: "test_plans",
  test_run: "test_runs",
  priority: "priorities"
};

export const REVERSE_TESTRAILS_COMMON_FILTER_KEY_MAPPING: Record<string, string> = makeObjectKeyAsValue(
  TESTRAILS_COMMON_FILTER_KEY_MAPPING
);

export const testrailsStackFilters = [
  "milestone",
  "project",
  "test_plan",
  "test_run",
  "priority",
  "status",
  "assignee",
  "type"
];

export const TESTRAILS_STACK_FILTERS_OPTIONS = [
  { label: "Milestone", value: "milestone" },
  { label: "Project", value: "project" },
  { label: "Test Plan", value: "test_plan" },
  { label: "Test Run", value: "test_run" },
  { label: "Priority", value: "priority" },
  { label: "Current Status", value: "status" },
  { label: "Assignee", value: "assignee" },
  { label: "Type", value: "type" }
];

export const TESTRAILS_ACROSS_OPTIONS = [
  { label: "Milestone", value: "milestone" },
  { label: "Project", value: "project" },
  { label: "Test Plan", value: "test_plan" },
  { label: "Test Run", value: "test_run" },
  { label: "Priority", value: "priority" },
  { label: "Current Status", value: "status" },
  { label: "Type", value: "type" }
];

export const TESTRAILS_FILTER_LABEL_MAPPING: basicMappingType<string> = {
  status: "Current Status",
  project: "Project",
  test_plan: "Test Plan",
  test_run: "Test Run",
  priority: "Priority",
  type: "Type",
  milestone: "Milestone"
};

export const convertChartType = (params: any) => {
  const { type } = params;

  switch (type) {
    case SCMVisualizationTypes.BAR_CHART:
      return ChartType.BAR;
    case IssueVisualizationTypes.PIE_CHART:
      return ChartType.CIRCLE;
    default:
      return ChartType.BAR;
  }
};
