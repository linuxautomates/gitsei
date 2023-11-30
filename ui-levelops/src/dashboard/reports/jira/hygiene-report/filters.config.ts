import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import UniversalInputFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalInputFilter";
import { baseFilterConfig, LevelOpsFilter } from "model/filters/levelopsFilters";
import { withDeleteProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";
import { jiraCommonFiltersConfig } from "dashboard/report-filters/jira/common-filters.config";
import { EpicsFilterConfig } from "dashboard/report-filters/jira/epic-filter.config";
import { HygieneWeightsFiltersConfig } from "dashboard/report-filters/jira/hygiene-weights-filters-config";
import HygieneWeightFilterComponent from "../../../graph-filters/components/GenericFilterComponents/HygieneFilter/HygieneWeightFilter";
import { IssueCreatedAtFilterConfig } from "dashboard/report-filters/common/issue-created-filter.config";
import { IssueManagementSystemFilterConfig } from "dashboard/report-filters/common/issue-management-filter.config";
import { IssueResolvedAtFilterConfig } from "dashboard/report-filters/common/issue-resolved-filter.config";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import {
  ParentStoryPointsFilterConfig,
  StoryPointsFilterConfig
} from "dashboard/report-filters/common/story-points-filters.config";
import UniversalTextSwitchWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalTextSwitchWrapper";
import { generateOUFilterConfig } from "dashboard/report-filters/common/ou-filter.config";
import { OUFiltersMapping } from "../../../constants/ou-filters";

export const generateHygieneConfigs = (hygiene: any[]) => {
  return hygiene.map(hygiene => {
    return baseFilterConfig(hygiene.id ?? hygiene.value ?? hygiene, {
      renderComponent: HygieneWeightFilterComponent,
      label: hygiene.name ?? hygiene.label ?? hygiene,
      labelCase: "title_case",
      filterMetaData: {},
      tab: WIDGET_CONFIGURATION_KEYS.WEIGHTS
    });
  });
};

export const JiraIssueHygieneReportFiltersConfig: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig,
  HygieneWeightsFiltersConfig,
  IssueResolvedAtFilterConfig,
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  {
    id: "poor_description",
    renderComponent: UniversalInputFilterWrapper,
    label: "Poor Description Length (Number Of Characters)",
    beKey: "poor_description",
    labelCase: "title_case",
    filterMetaData: { type: "number" },
    defaultValue: 30,
    deleteSupport: true,
    apiFilterProps: (args: any) => {
      const withDelete: withDeleteProps = {
        showDelete: args?.deleteSupport,
        key: args?.beKey,
        onDelete: args.handleRemoveFilter
      };
      return { withDelete };
    },
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS
  },
  {
    id: "idle",
    renderComponent: UniversalInputFilterWrapper,
    label: "Idle Length (Days)",
    beKey: "idle",
    labelCase: "title_case",
    filterMetaData: {
      type: "number"
    },
    defaultValue: 10,
    deleteSupport: true,
    apiFilterProps: (args: any) => {
      const withDelete: withDeleteProps = {
        showDelete: args?.deleteSupport,
        key: args?.beKey,
        onDelete: args.handleRemoveFilter
      };
      return { withDelete };
    },
    tab: WIDGET_CONFIGURATION_KEYS.FILTERS
  },
  {
    id: "hideScore",
    renderComponent: UniversalTextSwitchWrapper,
    label: "Hide Score",
    beKey: "hideScore",
    labelCase: "title_case",
    filterMetaData: {},
    tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
  },
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  IssueManagementSystemFilterConfig,
  EpicsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
