import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import JiraBAWidgetAcrossFilters from "dashboard/graph-filters/components/GenericFilterComponents/JiraBAWidgetAcrossFilters";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { IssueUpdatedAtFilterConfig } from "dashboard/report-filters/common/issue-updated-filter.config";
import { OUFiltersMapping } from "../../constants/ou-filters";
import { generateOUFilterConfig } from "../common/ou-filter.config";
import { DropDownData, EffortInvestmentProfileFilterData, LevelOpsFilter } from "model/filters/levelopsFilters";
import { jiraCommonFiltersConfig } from "./common-filters.config";
import { EpicsFilterConfig } from "./epic-filter.config";
import { IssueCreatedAtFilterConfig } from "../common/issue-created-filter.config";
import { ParentStoryPointsFilterConfig, StoryPointsFilterConfig } from "../common/story-points-filters.config";
import { generateMaxRecordsFilterConfig } from "../common/max-records-filter.config";
import { IssueManagementSystemFilterConfig } from "../common/issue-management-filter.config";
import { generateEffortInvestmentProfileFilter } from "./effort-investment-profile.filter";

const ticketCategorizationUnitFilterOptions = [
  {
    label: "Ticket Count",
    value: "tickets_report"
  },
  { label: "Story Point", value: "story_point_report" }
];

export const JiraIssueProgressReport: LevelOpsFilter[] = [
  ...jiraCommonFiltersConfig.filter((item: LevelOpsFilter) => !["statuses", "status_categories"].includes(item.beKey)),
  {
    id: "across",
    renderComponent: UniversalSelectFilterWrapper,
    label: "X-Axis",
    beKey: "across",
    labelCase: "title_case",
    required: true,
    filterMetaData: {
      selectMode: "default",
      options: [
        { label: "Effort Investment Category", value: "ticket_category" },
        { label: "Epics", value: "epic" }
      ],
      sortOptions: true
    } as DropDownData,
    tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
  },
  {
    id: "effort_investment_profile",
    renderComponent: JiraBAWidgetAcrossFilters,
    label: "Effort Investment Profile",
    beKey: "ticket_categorization_scheme",
    labelCase: "title_case",
    required: true,
    filterMetaData: {
      categorySelectionMode: "default",
      withProfileCategory: false,
      showDefaultScheme: true,
      isCategoryRequired: false
    } as EffortInvestmentProfileFilterData,
    tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
  },
  generateMaxRecordsFilterConfig(
    [
      { label: "5", value: 5 },
      { label: "10", value: 10 },
      { label: "20", value: 20 },
      { label: "50", value: 50 },
      { label: "100", value: 100 }
    ],
    "Max Records"
  ),
  {
    id: "uri_unit",
    renderComponent: UniversalSelectFilterWrapper,
    label: "Effort Unit",
    beKey: "uri_unit",
    labelCase: "title_case",
    defaultValue: "tickets_report",
    filterMetaData: {
      options: ticketCategorizationUnitFilterOptions,
      selectMode: "default",
      sortOptions: true
    } as DropDownData,
    tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
  },
  IssueCreatedAtFilterConfig,
  IssueUpdatedAtFilterConfig,
  IssueManagementSystemFilterConfig,
  StoryPointsFilterConfig,
  ParentStoryPointsFilterConfig,
  EpicsFilterConfig,
  generateOUFilterConfig({
    jira: {
      options: OUFiltersMapping.jira
    }
  })
];
