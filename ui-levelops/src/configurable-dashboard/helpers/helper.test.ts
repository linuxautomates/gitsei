import * as helperFunctions from "./helper";
import { RestWidget } from "../../classes/RestDashboards";

describe("configurable-dashboard helper test suite", () => {
  test("getFilterValue should return correct value", () => {
    expect(helperFunctions.getFilterValue({}, "issue_type")).toStrictEqual([]);
    expect(
      helperFunctions.getFilterValue(
        {
          exclude: {
            statuses: [],
            priorities: []
          }
        },
        "priorities"
      )
    ).toStrictEqual([]);
  });

  test("isExcludeVal should return correct value", () => {
    expect(
      helperFunctions.isExcludeVal(
        {
          exclude: {
            issue_types: [],
            statuses: [],
            custom_fields: {
              customfield_10004: []
            },
            hygiene_types: []
          }
        },
        "issue_types"
      )
    ).toEqual(true);

    expect(
      helperFunctions.isExcludeVal(
        {
          exclude: {
            issue_types: [],
            statuses: [],
            custom_fields: {
              customfield_10004: []
            },
            hygiene_types: []
          }
        },
        "wrong_key"
      )
    ).toEqual(false);
  });

  test("validateConfigTableWidget should return correct value", () => {
    expect(
      helperFunctions.validateConfigTableWidget(
        new RestWidget({
          id: "b03c27f0-663e-11eb-b05a-09aca4a67d9e",
          name: "New ncc report",
          type: "ncc_group_vulnerability_report",
          query: {
            across: "category",
            stacks: []
          },
          order: 5,
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
            order: 5,
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
        })
      )
    ).toStrictEqual(false);
  });
});
