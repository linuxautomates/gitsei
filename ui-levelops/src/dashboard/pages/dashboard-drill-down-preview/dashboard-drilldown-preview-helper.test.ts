import * as helperFunctions from "./helper";

describe("buildDrillDownFilters test suite", () => {
  test("it should return correct response if drillDownTransformFunction exists", () => {
    const mock_drillDownProps = {
      application: "levelops_issues",
      dashboardId: "50",
      levelops_issues: { name: "NEW", id: "27" },
      widgetId: "fd276620-5f67-11eb-b114-110d5027f4cd",
      x_axis: { name: "NEW", id: "27" },
      widgetMetaData: {
        children: [],
        custom_hygienes: [],
        hidden: false,
        max_records: 20,
        order: 4,
        weights: {
          IDLE: 20,
          LATE_ATTACHMENTS: 2,
          NO_ASSIGNEE: 20,
          NO_COMPONENTS: 20,
          NO_DUE_DATE: 30,
          POOR_DESCRIPTION: 8
        },
        widget_type: "graph",
        width: "half"
      }
    };
    const mock_widget = {
      type: "levelops_workitem_count_report",
      query: {
        across: "state",
        stacks: []
      }
    };
    const mock_dashboardQuery = {
      product_id: "80",
      integration_ids: ["227", "203", "164", "229", "250", "106", "30", "159", "146"]
    };
    const mock_metaData = {
      children: [],
      custom_hygienes: [],
      hidden: false,
      max_records: 20,
      order: 4,
      weights: {
        IDLE: 20,
        LATE_ATTACHMENTS: 2,
        NO_ASSIGNEE: 20,
        NO_COMPONENTS: 20,
        NO_DUE_DATE: 30,
        POOR_DESCRIPTION: 8
      },
      widget_type: "graph",
      width: "half"
    };

    const mock_response = {
      acrossValue: "state",
      filters: {
        across: "state",
        filter: { stacks: Array(0), status: "NEW" }
      }
    };

    expect(
      helperFunctions.buildDrillDownFilters(mock_drillDownProps, mock_widget, mock_dashboardQuery, mock_metaData)
    ).toStrictEqual(mock_response);
  });

  test("it should return empty object response if drillDownTransformFunction does not exists", () => {
    const mock_drillDownProps = {
      application: "",
      dashboardId: "",
      widgetId: "",
      x_axis: ""
    };
    const mock_widget = { type: "xyz" };
    const mock_dashboardQuery = "";
    const mock_metaData = "";

    const mock_response = {};

    expect(
      helperFunctions.buildDrillDownFilters(mock_drillDownProps, mock_widget, mock_dashboardQuery, mock_metaData)
    ).toStrictEqual(mock_response);
  });
});
