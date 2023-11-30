import { cloneWidgets, mapFiltersToIds, mapNonApiFilters, mapUserIdsToAssigneeOrReporter } from "./helper";

describe("dashboard helper file tests", () => {
  const userData = [
    {
      email: "kushagra@devslane.com",
      id: "21",
      name: "Kushagra Saxena"
    }
  ];

  const filterData = {
    assignees: ["21"],
    reporter: "maxime@levelops.io",
    stacks: ["assignee"],
    status: "OPEN"
  };

  test("mapUserIdsToAssigneeOrReporter() test", () => {
    const expectedOutput = {
      assignee_user_ids: [{ key: "21", label: "kushagra@devslane.com" }]
    };

    const data = mapUserIdsToAssigneeOrReporter(userData, filterData);
    expect(data).toStrictEqual(expectedOutput);

    const data1 = mapUserIdsToAssigneeOrReporter([], filterData);
    expect(data1).toStrictEqual({ assignee_user_ids: [] });

    const data2 = mapUserIdsToAssigneeOrReporter(userData, {});
    expect(data2).toStrictEqual({});
  });

  test("mapNonApiFilters() test", () => {
    const expectedOutput = {
      reporter: "maxime@levelops.io",
      stacks: ["assignee"],
      status: "OPEN"
    };

    const filter = mapNonApiFilters(filterData);
    expect(filter).toStrictEqual(expectedOutput);

    const filter1 = mapNonApiFilters({});
    expect(filter1).toStrictEqual({});
  });

  test("mapFiltersToIds() test", () => {
    const expectedOutput = {
      user_ids: ["21"]
    };

    const filter = mapFiltersToIds(filterData);
    expect(filter).toStrictEqual(expectedOutput);

    const filter1 = mapFiltersToIds({});
    expect(filter1).toStrictEqual({});

    const filter2 = mapFiltersToIds(undefined);
    expect(filter2).toStrictEqual({});
  });

  test("cloneWidgets() test", () => {
    const widgets = [
      {
        dashboard_id: "568",
        id: "f6bd9130-6497-11eb-8f84-630707561c5e",
        name: "widget 1",
        type: "testrails_tests_report",
        query: {
          across: "milestone",
          stacks: []
        },
        metadata: {
          children: [],
          custom_hygienes: [],
          hidden: false,
          max_records: 20,
          order: 1,
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
      },
      {
        dashboard_id: "568",
        id: "01df7380-6498-11eb-8f84-630707561c5e",
        name: "widget 2",
        type: "sonarqube_issues_report",
        query: {
          across: "type",
          stacks: []
        },
        metadata: {
          children: [],
          custom_hygienes: [],
          hidden: false,
          max_records: 20,
          order: 2,
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
      }
    ];

    const clonedWidgets = cloneWidgets(widgets);
    expect(clonedWidgets[0].id !== widgets[0].id && clonedWidgets[0].id !== widgets[1].id).toBeTruthy();
    expect(clonedWidgets[0].id !== widgets[1].id && clonedWidgets[1].id !== widgets[1].id).toBeTruthy();

    const compositeWidgets = [
      {
        dashboard_id: "568",
        id: "8dd54e80-649a-11eb-97ad-5d446feafe07",
        name: "widget 1",
        type: "",
        query: {},
        metadata: {
          children: ["a3e50ad0-649a-11eb-97ad-5d446feafe07", "9369ae90-649a-11eb-97ad-5d446feafe07"],
          custom_hygienes: [],
          hidden: false,
          max_records: 20,
          order: 1,
          weights: {
            IDLE: 20,
            LATE_ATTACHMENTS: 2,
            NO_ASSIGNEE: 20,
            NO_COMPONENTS: 20,
            NO_DUE_DATE: 30,
            POOR_DESCRIPTION: 8
          },
          widget_type: "compositegraph",
          width: "half"
        }
      },
      {
        dashboard_id: "568",
        id: "9369ae90-649a-11eb-97ad-5d446feafe07",
        name: "series 1",
        type: "cicd_pipeline_jobs_duration_trend_report",
        query: { across: "trend" },
        metadata: {
          children: [],
          custom_hygienes: [],
          hidden: false,
          max_records: 20,
          order: 1,
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
      },
      {
        dashboard_id: "568",
        id: "a3e50ad0-649a-11eb-97ad-5d446feafe07",
        name: "series 2",
        type: "jenkins_job_config_change_counts_trend",
        query: { across: "trend" },
        metadata: {
          children: [],
          custom_hygienes: [],
          hidden: false,
          max_records: 20,
          order: 1,
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
      }
    ];

    const clonedWidgets1 = cloneWidgets(compositeWidgets);
    const graphWidgets = clonedWidgets1.filter((widget: any) => widget.metadata.widget_type === "graph");
    const clonedCompositeWidget = clonedWidgets1.filter(
      (widget: any) => widget.metadata.widget_type === "compositegraph"
    );

    expect(graphWidgets[0].id !== compositeWidgets[1].id && graphWidgets[0].id !== compositeWidgets[2].id).toBeTruthy();
    expect(graphWidgets[1].id !== compositeWidgets[1].id && graphWidgets[1].id !== compositeWidgets[2].id).toBeTruthy();
    expect(clonedCompositeWidget[0].metadata.children).toStrictEqual(graphWidgets.map((widget: any) => widget.id));

    expect(clonedCompositeWidget[0].id !== compositeWidgets[0].id).toBeTruthy();
  });
});
