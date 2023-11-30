import { IntegrationTypes } from "constants/IntegrationTypes";

export const mockDrillDownProps = {
  application: IntegrationTypes.JIRA,
  dashboard: {
    id: "324",
    name: "salesforce config test",
    widgets: [
      {
        id: "4632ef10-3a06-11eb-8026-d5c43d887d47",
        name: "tickets report ",
        type: "tickets_report",
        query: { across: "status", stacks: [], partial_match: { project: { $begins: "test" } } },
        order: 1,
        width: "half",
        weights: {
          IDLE: 20,
          NO_ASSIGNEE: 20,
          NO_DUE_DATE: 30,
          NO_COMPONENTS: 20,
          LATE_ATTACHMENTS: 2,
          POOR_DESCRIPTION: 8
        },
        max_records: 20,
        widget_type: "graph",
        hidden: false,
        children: [],
        metadata: {
          order: 1,
          width: "half",
          hidden: false,
          weights: {
            IDLE: 20,
            NO_ASSIGNEE: 20,
            NO_DUE_DATE: 30,
            NO_COMPONENTS: 20,
            LATE_ATTACHMENTS: 2,
            POOR_DESCRIPTION: 8
          },
          children: [],
          max_records: 20,
          widget_type: "graph",
          custom_hygienes: []
        }
      }
    ],
    query: {
      product_id: "118",
      integration_ids: ["108", "148", "213", "216", "212", "203", "159", "228", "227", "30", "106"]
    },
    owner_id: "19",
    public: false,
    default: false,
    metadata: { global_filters: { jira_filter_values: { partial_match: { project: { $begins: "test" } } } } },
    type: "someType"
  },
  widgetId: "4632ef10-3a06-11eb-8026-d5c43d887d47",
  x_axis: "DONE",
  jira: "DONE",
  widgetMetaData: {
    order: 1,
    width: "half",
    hidden: false,
    weights: {
      IDLE: 20,
      NO_ASSIGNEE: 20,
      NO_DUE_DATE: 30,
      NO_COMPONENTS: 20,
      LATE_ATTACHMENTS: 2,
      POOR_DESCRIPTION: 8
    },
    children: [],
    max_records: 20,
    widget_type: "graph",
    custom_hygienes: []
  }
};
