import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { WidgetFilterType } from "dashboard/constants/enums/WidgetFilterType.enum";
import { RANGE_FILTER_CHOICE } from "dashboard/constants/filter-key.mapping";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import moment from "moment";

export const jiraChartProps: basicMappingType<any> = {
  barGap: 0,
  margin: { top: 20, right: 5, left: 5, bottom: 50 }
};

export const issue_resolved_at = {
  type: WidgetFilterType.TIME_BASED_FILTERS,
  label: "Issue Resolved In",
  BE_key: "issue_resolved_at",
  slicing_value_support: true,
  configTab: WIDGET_CONFIGURATION_KEYS.FILTERS
};

export const issueSingleStatDefaultMeta = {
  [RANGE_FILTER_CHOICE]: {
    jira_issue_created_at: {
      type: "relative",
      relative: {
        last: {
          num: 30,
          unit: "days"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

export const issueSingleDefualtCreatedAt = {
  $lt: moment.utc().unix().toString(),
  $gt: moment.utc().unix().toString()
};
export const jiraLeadTimeDefaultQuery = {
  limit_to_only_applicable_data: false
};

export const sprintDefaultMeta = {
  [RANGE_FILTER_CHOICE]: {
    completed_at: {
      type: "relative",
      relative: {
        last: {
          num: 4,
          unit: "weeks"
        },
        next: {
          unit: "today"
        }
      }
    }
  }
};

export const idFilters = ["assignee", "reporter", "first_assignee"];

export const jiraApiBasedFilterKeyMapping = {
  assignees: "assignee",
  reporters: "reporter",
  first_assignees: "first_assignee",
  jira_assignees: "jira_assignee",
  jira_reporters: "jira_reporter",
  authors: "author",
  committers: "committer"
};

export const baApiFilters = ["reporters", "assignees", "authors", "committers"];
