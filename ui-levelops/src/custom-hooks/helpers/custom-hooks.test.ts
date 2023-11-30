import {
  cicdSCMJobCountTransformer,
  genericTicketsReportTransformer,
  getStacksData,
  jenkinsJobConfigChangeTransform,
  jenkinsPipelineJobsCountTransformer,
  jobsChangeVolumeTransform,
  levelopsAsssessmentCountReportTransformer,
  nccGroupIssuesReportTransform,
  praetorianIssuesReportTransform,
  scmFilesTransform,
  sonarqubeIssuesReportTransformer,
  timeDurationGenericTransform
} from "./helper";

describe("custom-hooks/helper.ts Tests", () => {
  const epoch = 1611655965;
  const epochInMins = 26860933; // Rounded

  test("getStacksData() test", () => {
    const stacks = [
      { key: "1602028800", count: 8 },
      { key: "1602115200", count: 1 }
    ];
    const filters = { across: "cicd_user_id", stacks: ["trend"] };
    const reportType = "jenkins_job_config_change_counts";

    const expectedOutput = { "10/07": 8, "10/08": 1 };

    const stackedData = getStacksData(stacks, filters, reportType, "count");
    expect(stackedData).toStrictEqual(expectedOutput);

    const stackedData1 = getStacksData([], filters, reportType, "count");
    expect(stackedData1).toStrictEqual({});
  });

  test("scmFilesTransform() test", () => {
    const inputData = {
      apiData: [
        {
          created_at: 1611783072,
          filename: "README.md",
          id: "e4c09f54-8515-4a7c-baba-857f97c4de14",
          integration_id: "371",
          num_commits: 2,
          repo_id: "chandra17/lo-bank",
          total_additions: 0,
          total_changes: 0,
          total_deletions: 0
        },
        {
          created_at: 1611770473,
          filename: "README.md",
          id: "82de5409-3fb9-4183-8f6d-4b134fdea01a",
          integration_id: "371",
          num_commits: 1,
          repo_id: "srinath.chandrashekhar/test",
          total_additions: 0,
          total_changes: 0,
          total_deletions: 0
        },
        {
          created_at: 1611770473,
          filename: "CONTRIBUTING.md",
          id: "b00a22ba-d77f-43b5-b73e-f288216d2956",
          integration_id: "371",
          num_commits: 1,
          repo_id: "srinath.chandrashekhar/test",
          total_additions: 0,
          total_changes: 0,
          total_deletions: 0
        }
      ],
      filters: { across: "" },
      records: 20,
      reportType: "scm_files_report",
      sortBy: undefined,
      statUri: "scm_files_report",
      uri: "scm_files_report",
      widgetFilters: { across: "repo_id", filter: { integration_ids: ["371", "373", "375"], product_id: "134" } }
    };

    const outputData = {
      data: [
        {
          color: "#abfbfc",
          created_at: 1611783072,
          filename: "README.md",
          id: "e4c09f54-8515-4a7c-baba-857f97c4de14",
          integration_id: "371",
          num_commits: 2,
          repo_id: "chandra17/lo-bank",
          subTitle: { label: "File Name", value: "README.md" },
          title: { label: "Repo Id", value: "chandra17/lo-bank" },
          total_additions: 0,
          total_changes: 0,
          total_deletions: 0
        },
        {
          color: "#abfbfc",
          created_at: 1611770473,
          filename: "README.md",
          id: "82de5409-3fb9-4183-8f6d-4b134fdea01a",
          integration_id: "371",
          num_commits: 1,
          repo_id: "srinath.chandrashekhar/test",
          subTitle: { label: "File Name", value: "README.md" },
          title: { label: "Repo Id", value: "srinath.chandrashekhar/test" },
          total_additions: 0,
          total_changes: 0,
          total_deletions: 0
        },
        {
          color: "#d3617c",
          created_at: 1611770473,
          filename: "CONTRIBUTING.md",
          id: "b00a22ba-d77f-43b5-b73e-f288216d2956",
          integration_id: "371",
          num_commits: 1,
          repo_id: "srinath.chandrashekhar/test",
          subTitle: { label: "File Name", value: "CONTRIBUTING.md" },
          title: { label: "Repo Id", value: "srinath.chandrashekhar/test" },
          total_additions: 0,
          total_changes: 0,
          total_deletions: 0
        }
      ],
      dataKey: "num_commits",
      total: 4
    };

    const transformedData = scmFilesTransform(inputData);
    // expect(transformedData).toEqual(outputData);

    const inputData1 = {
      apiData: [],
      filters: { across: "" },
      records: 20,
      reportType: "scm_jira_files_report",
      sortBy: undefined,
      statUri: "scm_jira_files_report",
      uri: "scm_jira_files_report",
      widgetFilters: { across: undefined, filter: { integration_ids: ["371", "373", "375"], product_id: "134" } }
    };

    const outputData1 = {
      dataKey: "num_issues",
      total: 0,
      data: []
    };

    const transformedData1 = scmFilesTransform(inputData1);
    expect(transformedData1).toStrictEqual(outputData1);
  });

  test("jenkinsJobConfigChangeTransform() test", () => {
    const inputData = {
      apiData: [
        { key: "admin", count: 208 },
        { key: "viraj", count: 41 },
        { key: "testread", count: 31 },
        { key: "meghana", count: 9 },
        { key: "ishan", count: 3 },
        { key: "gershon", count: 2 }
      ],
      filters: { across: "cicd_user_id", stacks: [] },
      records: 20,
      reportType: "jenkins_job_config_change_counts",
      sortBy: undefined,
      statUri: "jenkins_job_config_change_counts",
      uri: "jenkins_job_config_change_counts",
      widgetFilters: { across: "cicd_user_id", filter: { integration_ids: ["371", "373", "375"], product_id: "134" } }
    };

    const outputData = {
      data: [
        { count: 208, name: "admin" },
        { count: 41, name: "viraj" },
        { count: 31, name: "testread" },
        { count: 9, name: "meghana" },
        { count: 3, name: "ishan" },
        { count: 2, name: "gershon" }
      ]
    };

    const transformedData = jenkinsJobConfigChangeTransform(inputData);
    expect(transformedData).toStrictEqual(outputData);

    const inputData1 = {
      apiData: [],
      filters: { across: "cicd_user_id", stacks: [] },
      records: 20,
      reportType: "jenkins_job_config_change_counts",
      sortBy: undefined,
      statUri: "jenkins_job_config_change_counts",
      uri: "jenkins_job_config_change_counts",
      widgetFilters: { across: "cicd_user_id", filter: { integration_ids: ["371", "373", "375"], product_id: "134" } }
    };

    const outputData1 = {
      data: []
    };
    const transformedData1 = jenkinsJobConfigChangeTransform(inputData1);
    expect(transformedData1).toStrictEqual(outputData1);

    const inputData2 = {
      ...inputData,
      apiData: [
        {
          count: 21,
          key: "admin",
          stacks: [
            { key: "1599004800", count: 13 },
            { key: "1598745600", count: 7 },
            { key: "1606176000", count: 1 }
          ]
        },
        {
          count: 2,
          key: "viraj",
          stacks: [{ key: "1606176000", count: 2 }]
        }
      ],
      filters: { ...inputData.filters, stacks: ["trend"], job_names: ["Test Pipeline 2"] },
      widgetFilters: {
        across: "cicd_user_id",
        stacks: ["trend"],
        filter: { integration_ids: ["371", "373", "375"], product_id: "134" }
      }
    };
    const outputData2 = {
      data: [
        { count: 21, name: "admin", "09/02": 13, "08/30": 7, "11/24": 1 },
        { count: 2, name: "viraj", "11/24": 2 }
      ]
    };

    const transformedData2 = jenkinsJobConfigChangeTransform(inputData2);
    expect(transformedData2).toStrictEqual(outputData2);
  });

  test("genericTicketsReportTransformer() test", () => {
    const inputData = {
      apiData: [
        { key: "CODE_SMELL", total_issues: 8248 },
        { key: "BUG", total_issues: 506 },
        { key: "VULNERABILITY", total_issues: 42 }
      ],
      filters: { across: "type", stacks: [] },
      records: 20,
      reportType: "sonarqube_issues_report",
      sortBy: "total_issues",
      statUri: "sonarqube_issues_report",
      uri: "sonarqube_issues_report",
      widgetFilters: { across: "type", filter: { integration_ids: ["371", "373", "375"], product_id: "134" } }
    };

    const outputData = [
      {
        id: undefined,
        key: "CODE_SMELL",
        name: "CODE_SMELL",
        total_issues: 8248
      },
      {
        id: undefined,
        key: "BUG",
        name: "BUG",
        total_issues: 506
      },
      {
        id: undefined,
        key: "VULNERABILITY",
        name: "VULNERABILITY",
        total_issues: 42
      }
    ];

    const transformedData = genericTicketsReportTransformer(inputData, "total_issues");
    expect(transformedData).toStrictEqual(outputData);

    const inputData1 = {
      ...inputData,
      apiData: []
    };
    const transformedData1 = genericTicketsReportTransformer(inputData1, "total_issues");
    expect(transformedData1).toStrictEqual([]);
  });

  test("sonarqubeIssuesReportTransformer() test", () => {
    const inputData = {
      apiData: [
        { key: "CODE_SMELL", total_issues: 8248 },
        { key: "BUG", total_issues: 506 },
        { key: "VULNERABILITY", total_issues: 42 }
      ],
      filters: { across: "type", stacks: [] },
      records: 20,
      reportType: "sonarqube_issues_report",
      sortBy: "total_issues",
      statUri: "sonarqube_issues_report",
      uri: "sonarqube_issues_report",
      widgetFilters: { across: "type", filter: { integration_ids: ["371", "373", "375"], product_id: "134" } }
    };

    const outputData = {
      data: [
        {
          id: undefined,
          key: "CODE_SMELL",
          name: "CODE_SMELL",
          total_issues: 8248
        },
        {
          id: undefined,
          key: "BUG",
          name: "BUG",
          total_issues: 506
        },
        {
          id: undefined,
          key: "VULNERABILITY",
          name: "VULNERABILITY",
          total_issues: 42
        }
      ]
    };

    const transformedData = sonarqubeIssuesReportTransformer(inputData);
    expect(transformedData).toStrictEqual(outputData);

    const inputData1 = {
      ...inputData,
      apiData: []
    };

    const transformedData1 = sonarqubeIssuesReportTransformer(inputData1);
    expect(transformedData1).toStrictEqual({ data: [] });
  });

  test("levelopsAsssessmentCountReportTransformer() test", () => {
    const inputData = {
      apiData: [
        {
          id: "20fed146-f54a-4391-82eb-1e361b258fb4",
          key: "fgh",
          total: 5
        },
        {
          id: "6ceec26b-fbc5-45ec-b95a-1f41e79eca28",
          key: "AC-03-Q-4-1",
          total: 3
        },
        {
          id: "10efea0e-e675-42c3-8cd9-2ca342c36252",
          key: "testing edit",
          total: 2
        },
        {
          id: "57532e71-5b70-4748-b24b-ad0fe8ef0948",
          key: "template create -1",
          total: 2
        },
        {
          id: "4fcb3146-4062-4aba-8aa8-413982e6b3c6",
          key: "AC-03-Q-5-1",
          total: 1
        },
        {
          id: "80da25b7-f043-4215-b480-7b5833de37b1",
          key: "VA Test 33",
          total: 1
        },
        {
          id: "a3b9f733-bbbc-4e49-ab63-535704ba15cf",
          key: "template create",
          total: 1
        },
        {
          id: "ed26bff2-6b57-4206-adc6-32935750389d",
          key: "AC-03-Q-6-1",
          total: 1
        }
      ],
      filters: {
        across: "questionnaire_template_id",
        completed: true,
        created_at: { $gt: 1609525800, $lt: 1612290599 },
        stacks: []
      },
      records: 20,
      reportType: "levelops_assessment_count_report",
      sortBy: "total",
      statUri: "levelops_assessment_count_report",
      uri: "quiz_aggs_count_report",
      widgetFilters: {
        across: "questionnaire_template_id",
        filter: {
          completed: true,
          created_at: { $gt: 1609525800, $lt: 1612290599 }
        },
        stacks: []
      }
    };

    const outputData = { data: [...inputData.apiData.map((item: any) => ({ ...item, name: item.key }))] };

    const transformedData = levelopsAsssessmentCountReportTransformer(inputData);
    expect(transformedData).toStrictEqual(outputData);

    const inputData1 = {
      ...inputData,
      apiData: []
    };

    const transformedData1 = levelopsAsssessmentCountReportTransformer(inputData1);
    expect(transformedData1).toStrictEqual({ data: [] });
  });

  test("timeDurationGenericTransform() test", () => {
    const inputData = {
      apiData: [
        {
          count: 1368,
          key: "SYSTEM",
          max: 11821,
          median: 11,
          min: 0,
          sum: 448044
        },
        {
          count: 471,
          key: "admin",
          max: 11231,
          median: 11,
          min: 0,
          sum: 31305
        },
        {
          count: 746,
          key: "meghana",
          max: 867,
          median: 7,
          min: 0,
          sum: 129575
        },
        {
          count: 193,
          key: "thanh",
          max: 726,
          median: 8,
          min: 0,
          sum: 39458
        },
        {
          count: 24,
          key: "temoke",
          max: 724,
          median: 13,
          min: 5,
          sum: 7814
        },
        {
          count: 4256,
          key: "SCMTrigger",
          max: 641,
          median: 17,
          min: 0,
          sum: 238808
        },
        {
          count: 562,
          key: "viraj",
          max: 523,
          median: 22,
          min: 0,
          sum: 39506
        },
        {
          count: 103,
          key: "testread",
          max: 383,
          median: 17,
          min: 0,
          sum: 3345
        },
        {
          count: 2,
          key: "ishan",
          max: 47,
          median: 3,
          min: 3,
          sum: 50
        },
        {
          count: 1,
          key: "gershon",
          max: 35,
          median: 35,
          min: 35,
          sum: 35
        }
      ],
      filters: { across: "cicd_user_id" },
      records: 20,
      reportType: "cicd_scm_jobs_duration_report",
      sortBy: undefined,
      statUri: "jobs_duration_report",
      uri: "jobs_duration_report",
      across: "cicd_user_id",
      filter: { product_id: "134", integration_ids: [] }
    };

    const outputData = {
      data: [
        { median: 0, min: 0, max: 197, name: "SYSTEM" },
        { median: 0, min: 0, max: 187, name: "admin" },
        { median: 0, min: 0, max: 14, name: "meghana" },
        { median: 0, min: 0, max: 12, name: "thanh" },
        { median: 0, min: 0, max: 12, name: "temoke" },
        { median: 0, min: 0, max: 11, name: "SCMTrigger" },
        { median: 0, min: 0, max: 9, name: "viraj" },
        { median: 0, min: 0, max: 6, name: "testread" },
        { median: 0, min: 0, max: 1, name: "ishan" },
        { median: 1, min: 1, max: 1, name: "gershon" }
      ]
    };

    const transformedData = timeDurationGenericTransform(inputData);
    expect(transformedData).toStrictEqual(outputData);

    const inputData1 = {
      ...inputData,
      apiData: []
    };

    const transformedData1 = timeDurationGenericTransform(inputData1);
    expect(transformedData1).toStrictEqual({ data: [] });
  });

  test("jenkinsPipelineJobsCountTransformer() test", () => {
    const inputData = {
      apiData: [
        {
          additional_key: "Jenkins Instance",
          cicd_job_id: "3adbb0be-c5c0-443c-ad58-ca85249f0d0e",
          count: 460,
          key: "UIBuild"
        },
        {
          additional_key: "Jenkins Instance",
          cicd_job_id: "06023828-92b0-422a-9dfb-2949508be5e4",
          count: 199,
          key: "Deploy-UI-to-Kubernetes-Cluster"
        }
      ],
      filters: {
        across: "cicd_job_id",
        cicd_user_ids: ["meghana"],
        job_statuses: ["SUCCESS"]
      },
      records: 20,
      reportType: "cicd_pipeline_jobs_count_report",
      sortBy: undefined,
      statUri: "pipelines_jobs_count_report",
      uri: "pipelines_jobs_count_report",
      widgetFilters: {}
    };

    const outputData = {
      data: [
        {
          additional_key: "Jenkins Instance",
          cicd_job_id: "3adbb0be-c5c0-443c-ad58-ca85249f0d0e",
          color: "#6375ed",
          count: 460,
          key: "UIBuild",
          subTitle: { label: "Job", value: "UIBuild" }
        },
        {
          additional_key: "Jenkins Instance",
          cicd_job_id: "06023828-92b0-422a-9dfb-2949508be5e4",
          color: "#f4a6b7",
          count: 199,
          key: "Deploy-UI-to-Kubernetes-Cluster",
          subTitle: {
            label: "Job",
            value: "Deploy-UI-to-Kubernetes-Cluster"
          }
        }
      ],
      dataKey: "count",
      total: 659
    };

    const transformedData = jenkinsPipelineJobsCountTransformer(inputData);
    // expect(transformedData).toStrictEqual(outputData);

    const inputData1 = {
      ...inputData,
      apiData: []
    };

    const outputData1 = {
      data: [
        {
          color: "#84b8f4",
          noData: "No Jobs Found",
          subTitle: {
            label: "Job",
            value: "No Jobs Found"
          }
        }
      ],
      dataKey: "noData",
      total: 1
    };

    const transformedData1 = jenkinsPipelineJobsCountTransformer(inputData1);
    expect(transformedData1).toStrictEqual(outputData1);
  });

  test("cicdSCMJobCountTransformer() test ", () => {
    const inputData = {
      apiData: [
        { key: "SYSTEM", count: 8 },
        { key: "viraj", count: 8 }
      ],
      filters: {
        across: "cicd_user_id",
        job_statuses: ["UNSTABLE"],
        stacks: []
      },
      records: 20,
      reportType: "cicd_scm_jobs_count_report",
      sortBy: undefined,
      statUri: "jobs_count_report",
      uri: "jobs_count_report",
      widgetFilters: {}
    };

    const outputData = {
      data: [
        { count: 8, name: "SYSTEM" },
        { count: 8, name: "viraj" }
      ]
    };

    const transformedData = cicdSCMJobCountTransformer(inputData);
    expect(transformedData).toStrictEqual(outputData);

    const inputData1 = {
      ...inputData,
      apiData: []
    };

    const transformedData1 = cicdSCMJobCountTransformer(inputData1);
    expect(transformedData1).toStrictEqual({ data: [] });

    const inputData2 = {
      ...inputData,
      apiData: [
        {
          key: "SYSTEM",
          count: 8,
          stacks: [
            { key: "1604534400", count: 4 },
            { key: "1604102400", count: 3 },
            { key: "1604448000", count: 1 }
          ]
        },
        {
          key: "viraj",
          count: 8,
          stacks: [
            { key: "1604534400", count: 4 },
            { key: "1604102400", count: 3 },
            { key: "1604448000", count: 1 }
          ]
        }
      ],
      filters: {
        ...inputData.filters,
        stacks: ["trend"]
      }
    };

    const outputData2 = {
      data: [
        {
          "10/31": 3,
          "11/04": 1,
          "11/05": 4,
          count: 8,
          name: "SYSTEM"
        },
        {
          "10/31": 3,
          "11/04": 1,
          "11/05": 4,
          count: 8,
          name: "viraj"
        }
      ]
    };

    const transformedData2 = cicdSCMJobCountTransformer(inputData2);
    expect(transformedData2).toStrictEqual(outputData2);
  });

  test("jobsChangeVolumeTransform() test", () => {
    const inputData = {
      apiData: [
        {
          key: "1617580800",
          additional_key: "5-4-2021",
          lines_added_count: 134,
          lines_removed_count: 11,
          files_changed_count: 5
        },
        {
          key: "1617321600",
          additional_key: "2-4-2021",
          lines_added_count: 166,
          lines_removed_count: 136,
          files_changed_count: 11
        },
        {
          key: "1617235200",
          additional_key: "1-4-2021",
          lines_added_count: 192,
          lines_removed_count: 19,
          files_changed_count: 10
        },
        {
          key: "1617148800",
          additional_key: "31-3-2021",
          lines_added_count: 753,
          lines_removed_count: 81,
          files_changed_count: 24
        }
      ]
    };

    const outputData = {
      data: [
        {
          name: "5-4-2021",
          key: "1617580800",
          lines_added_count: 134,
          files_changed_count: 5,
          lines_removed_count: -11
        },
        {
          name: "2-4-2021",
          key: "1617321600",
          lines_added_count: 166,
          files_changed_count: 11,
          lines_removed_count: -136
        },
        {
          name: "1-4-2021",
          key: "1617235200",
          lines_added_count: 192,
          files_changed_count: 10,
          lines_removed_count: -19
        },
        {
          name: "31-3-2021",
          key: "1617148800",
          lines_added_count: 753,
          files_changed_count: 24,
          lines_removed_count: -81
        }
      ]
    };

    const transformedData = jobsChangeVolumeTransform({ apiData: [] });

    expect(transformedData).toStrictEqual({ data: [] });

    const transformedData1 = jobsChangeVolumeTransform(inputData);

    expect(transformedData1).toStrictEqual(outputData);
  });

  test("praetorianIssuesReportTransform() test", () => {
    const inputData = {
      apiData: [
        { key: "key1", count: 8248 },
        { key: "key2", count: 506 },
        { key: "key3", count: 42 }
      ]
    };

    const outputData = {
      data: [
        { key: "key1", name: "key1", id: undefined, count: 8248 },
        { key: "key2", name: "key2", id: undefined, count: 506 },
        { key: "key3", name: "key3", id: undefined, count: 42 }
      ]
    };
    const transformedData = praetorianIssuesReportTransform(inputData);
    expect(transformedData).toStrictEqual(outputData);

    const transformedData1 = praetorianIssuesReportTransform({ apiData: [] });
    expect(transformedData1).toStrictEqual({ data: [] });
  });

  test("nccGroupIssuesReportTransform() test", () => {
    const inputData = {
      apiData: [
        { key: "key1", count: 8248 },
        { key: "key2", count: 506 },
        { key: "key3", count: 42 }
      ]
    };

    const outputData = {
      data: [
        { key: "key1", name: "key1", id: undefined, count: 8248 },
        { key: "key2", name: "key2", id: undefined, count: 506 },
        { key: "key3", name: "key3", id: undefined, count: 42 }
      ]
    };
    const transformedData = nccGroupIssuesReportTransform(inputData);
    expect(transformedData).toStrictEqual(outputData);

    const transformedData1 = nccGroupIssuesReportTransform({ apiData: [] });
    expect(transformedData1).toStrictEqual({ data: [] });
  });
});
