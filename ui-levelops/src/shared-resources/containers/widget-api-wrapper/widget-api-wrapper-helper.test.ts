import * as helperFunctions from "./helper";

// const special_cases = [
//   "jenkins_job_config_change_counts",
//   "cicd_scm_jobs_count_report",
//   "cicd_pipeline_jobs_count_report",
//   "pagerduty_hotspot_report",
//   "pagerduty_ack_trend",
//   "pagerduty_after_hours",
//   "testrails_tests_report",
//   "testrails_tests_estimate_report",
//   "testrails_tests_estimate_forecast_report",
//   "pagerduty_release_incidents",
//   "tickets_report",
//   "levelops_assessment_count_report",
//   "levelops_workitem_count_report",
// ]

describe("getProps test suite", () => {
  test("it should return existingProps if 'type' does not exists in 'special_cases' list or 'filters.stacks.length === 0'", () => {
    const mock_type = "assignee_time_report";
    const mock_existingProps = {
      barProps: [
        {
          dataKey: "count",
          name: "count",
          unit: "count"
        }
      ],
      chartProps: {
        barGap: 0,
        margin: {
          bottom: 50,
          left: 5,
          right: 5,
          top: 20
        }
      },
      stacked: false,
      unit: "Counts"
    };
    const mock_reportData = {
      data: []
    };
    const mock_filters = {
      across: "repo_id",
      filter: {
        integration_ids: ["329", "368"],
        product_id: "127",
        repo_ids: ["JamCode", "SecondDepot"]
      }
    };
    const mock_response = {
      ...mock_existingProps
    };

    expect(helperFunctions.getProps(mock_type, mock_existingProps, mock_reportData, mock_filters, {})).toStrictEqual(
      mock_response
    );
  });

  test("it should return correct response if 'type' exists in 'special_cases' list and 'filters.stacks.length > 0'", () => {
    const mock_type = "pagerduty_release_incidents";
    const mock_existingProps = {
      barProps: [
        {
          dataKey: "count",
          name: "count",
          unit: "count"
        }
      ],
      chartProps: {
        barGap: 0,
        margin: {
          bottom: 50,
          left: 5,
          right: 5,
          top: 20
        }
      },
      stacked: false,
      unit: "Counts"
    };
    const mock_reportData = {
      data: []
    };
    const mock_filters = {
      across: "repo_id",
      filter: {
        integration_ids: ["329", "368"],
        product_id: "127",
        repo_ids: ["JamCode", "SecondDepot"]
      },
      stacks: [1, 2, 3]
    };
    const mock_response = {
      barProps: [],
      chartProps: {
        barGap: 0,
        margin: {
          bottom: 50,
          left: 5,
          right: 5,
          top: 20
        }
      },
      stacked: true,
      unit: "Counts"
    };

    expect(helperFunctions.getProps(mock_type, mock_existingProps, mock_reportData, mock_filters, {})).toStrictEqual(
      mock_response
    );
  });
});

describe("convertChildKeysToSiblingKeys test suite", () => {
  test("it should return correct response when none of sibling keys is not present in parentKey object of inputObject", () => {
    const mock_inputObject = {
      filter: {
        integration_ids: ["106", "30", "159"],
        jira_statuses: ["DONE"],
        product_id: "80"
      }
    };
    const mock_parentKey = "filter";
    const mock_siblingKeys = ["across", "stacks", "sort"];

    const mock_response = {
      filter: {
        integration_ids: ["106", "30", "159"],
        jira_statuses: ["DONE"],
        product_id: "80"
      }
    };

    expect(
      helperFunctions.convertChildKeysToSiblingKeys(mock_inputObject, mock_parentKey, mock_siblingKeys)
    ).toStrictEqual(mock_response);
  });

  test("it should return correct response when one/more of sibling keys is present in parentKey object of inputObject", () => {
    const mock_inputObject = {
      filter: {
        integration_ids: ["106", "30", "159"],
        jira_statuses: ["DONE"],
        product_id: "80"
      }
    };
    const mock_parentKey = "filter";
    const mock_siblingKeys = ["across", "stacks", "product_id"];

    const mock_response = {
      filter: {
        integration_ids: ["106", "30", "159"],
        jira_statuses: ["DONE"]
      },
      product_id: "80"
    };

    expect(
      helperFunctions.convertChildKeysToSiblingKeys(mock_inputObject, mock_parentKey, mock_siblingKeys)
    ).toStrictEqual(mock_response);
  });
});

describe("mergeFilters test suite", () => {
  test("it should return correct response", () => {
    const mock_constFilters = {};
    const mock_widgetFilters = {
      jira_statuses: ["DONE"]
    };
    const mock_hiddenFilters = {};

    const mock_response = {
      jira_statuses: ["DONE"]
    };

    expect(helperFunctions.mergeFilters(mock_constFilters, mock_widgetFilters, mock_hiddenFilters)).toStrictEqual(
      mock_response
    );
  });
});

describe("combineAllFilters test suite", () => {
  test("it should return correct response", () => {
    const mock_constFilters = {};
    const mock_widgetFilters = {
      jira_statuses: ["DONE"]
    };
    const mock_hiddenFilters = {};

    const mock_response = {
      jira_statuses: ["DONE"]
    };

    expect(helperFunctions.combineAllFilters(mock_constFilters, mock_widgetFilters, mock_hiddenFilters)).toStrictEqual(
      mock_response
    );
  });
});
