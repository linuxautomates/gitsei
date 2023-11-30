import { jenkinsDrilldownTransformer } from "../jenkinsDrilldownTransformer";

describe("jenkinsDrilldownTransformer.ts tests for the following reports:", () => {
  const test_cases = [
    {
      it: "jenkins_job_config_change_counts (Jenkins Job Config Change Count Report)",
      input: {
        drillDownProps: {
          application: "jenkins_job_config",
          dashboardId: "710",
          widgetId: "b5f8c720-8614-11eb-bdb7-c57a14a4b9f6",
          x_axis: "viraj",
          jenkins_job_config: "viraj",
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
          },
          additionFilter: {}
        },
        widget: {
          id: "b5f8c720-8614-11eb-bdb7-c57a14a4b9f6",
          name: "Jenkins Job Config Change Count Report",
          type: "jenkins_job_config_change_counts",
          query: { across: "cicd_user_id" },
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
        },
        dashboardQuery: { product_id: "186", integration_ids: ["651"] },
        metaData: {
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
      },
      output: {
        acrossValue: "cicd_user_id",
        filters: { filter: { product_id: "186", cicd_user_ids: ["viraj"] }, across: "cicd_user_id" }
      }
    },
    {
      it: "jenkins_job_config_change_counts_trend (Jenkins Job Config Change Count Trend Report)",
      input: {
        drillDownProps: {
          application: "jenkins_job_config",
          dashboardId: "710",
          widgetId: "6866e5d0-92d7-11eb-b3b0-7b5e985952e0",
          x_axis: { name: "02/10", value: "1612915200" },
          jenkins_job_config: { name: "02/10", value: "1612915200" },
          widgetMetaData: {
            order: 3,
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
          },
          additionFilter: {}
        },
        widget: {
          id: "6866e5d0-92d7-11eb-b3b0-7b5e985952e0",
          name: "Jenkins Job Config Change Count Trend Report",
          type: "jenkins_job_config_change_counts_trend",
          query: { across: "trend" },
          metadata: {
            order: 3,
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
        },
        dashboardQuery: { product_id: "186", integration_ids: ["651"] },
        metaData: {
          order: 3,
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
      },
      output: {
        acrossValue: "trend",
        filters: {
          filter: {
            across: "trend",
            product_id: "186",
            job_config_changed_at: { $gt: "1612915200", $lt: "1613001599" }
          },
          across: "trend"
        }
      }
    },
    {
      it: "cicd_pipeline_jobs_duration_report Jenkins Pipeline Jobs Duration Report",
      input: {
        drillDownProps: {
          application: "jenkins_pipeline_jobs",
          dashboardId: "710",
          widgetId: "784a6ee0-92d7-11eb-b3b0-7b5e985952e0",
          x_axis: { name: "repo-test-1 (1)", value: "repo-test-1 (1)" },
          jenkins_pipeline_jobs: { name: "repo-test-1 (1)", value: "repo-test-1 (1)" },
          widgetMetaData: {
            order: 4,
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
          },
          additionFilter: {}
        },
        widget: {
          id: "784a6ee0-92d7-11eb-b3b0-7b5e985952e0",
          name: "Jenkins Pipeline Jobs Duration Report",
          type: "cicd_pipeline_jobs_duration_report",
          query: { across: "cicd_job_id" },
          metadata: {
            order: 4,
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
        },
        dashboardQuery: { product_id: "186", integration_ids: ["651"] },
        metaData: {
          order: 4,
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
      },
      output: {
        acrossValue: "cicd_job_id",
        filters: {
          filter: { product_id: "186", cicd_integration_ids: ["651"], job_names: ["repo-test-1 (1)"] },
          across: "cicd_job_id"
        }
      }
    },
    {
      it: "cicd_pipeline_jobs_duration_trend_report Jenkins Pipeline Jobs Duration Trend Report",
      input: {
        drillDownProps: {
          application: "jenkins_pipeline_jobs",
          dashboardId: "710",
          widgetId: "82952b10-92d7-11eb-b3b0-7b5e985952e0",
          x_axis: { name: "04/01", value: "1617235200" },
          jenkins_pipeline_jobs: { name: "04/01", value: "1617235200" },
          widgetMetaData: {
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
          },
          additionFilter: {}
        },
        widget: {
          id: "82952b10-92d7-11eb-b3b0-7b5e985952e0",
          name: "Jenkins Pipeline Jobs Duration Trend Report",
          type: "cicd_pipeline_jobs_duration_trend_report",
          query: { across: "trend", end_time: { $gt: "1617062400", $lt: "1617663252" } },
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
        },
        dashboardQuery: { product_id: "186", integration_ids: ["651"] },
        metaData: {
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
      },
      output: {
        acrossValue: "trend",
        filters: {
          filter: {
            end_time: { $gt: "1617062400", $lt: "1617663252" },
            across: "trend",
            product_id: "186",
            cicd_integration_ids: ["651"],
            start_time: { $gt: "1617235200", $lt: "1617321599" }
          },
          across: "trend"
        }
      }
    },
    {
      it: "cicd_pipeline_jobs_count_trend_report Jenkins Pipeline Jobs Count Trend Report",
      input: {
        drillDownProps: {
          application: "jenkins_pipeline_jobs",
          dashboardId: "710",
          widgetId: "9a69a1d0-92d7-11eb-b3b0-7b5e985952e0",
          x_axis: { name: "04/02", value: "1617321600" },
          jenkins_pipeline_jobs: { name: "04/02", value: "1617321600" },
          widgetMetaData: {
            order: 7,
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
          },
          additionFilter: {}
        },
        widget: {
          id: "9a69a1d0-92d7-11eb-b3b0-7b5e985952e0",
          name: "Jenkins Pipeline Jobs Count Trend Report",
          type: "cicd_pipeline_jobs_count_trend_report",
          query: { across: "trend", end_time: { $gt: "1617062400", $lt: "1617663334" } },
          metadata: {
            order: 7,
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
        },
        dashboardQuery: { product_id: "186", integration_ids: ["651"] },
        metaData: {
          order: 7,
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
      },
      output: {
        acrossValue: "trend",
        filters: {
          filter: {
            end_time: { $gt: "1617062400", $lt: "1617663334" },
            across: "trend",
            product_id: "186",
            start_time: { $gt: "1617321600", $lt: "1617407999" }
          },
          across: "trend"
        }
      }
    },
    {
      it: "jobs_count_trends_report SCM to CICD Jobs Count Trend Report",
      input: {
        drillDownProps: {
          application: "jenkins_github_job_runs",
          dashboardId: "803",
          widgetId: "c34166a0-9652-11eb-ae29-fd3492a8a00f",
          x_axis: { name: "04/01", value: "1617235200" },
          jenkins_github_job_runs: { name: "04/01", value: "1617235200" },
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
          },
          additionFilter: {}
        },
        widget: {
          id: "c34166a0-9652-11eb-ae29-fd3492a8a00f",
          name: "scm to cicd jobs count trend report",
          type: "jobs_count_trends_report",
          query: { across: "trend", end_time: { $gt: "1617062400", $lt: "1617662875" } },
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
        },
        dashboardQuery: { product_id: "186" },
        metaData: {
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
      },
      output: {
        acrossValue: "trend",
        filters: {
          filter: {
            end_time: { $gt: "1617062400", $lt: "1617662875" },
            across: "trend",
            product_id: "186",
            job_started_at: { $gt: "1617235200", $lt: "1617321599" }
          },
          across: "trend"
        }
      }
    }
  ];

  test_cases.forEach(test_case => {
    test(test_case.it || "should work", () => {
      //@ts-ignore
      const result = jenkinsDrilldownTransformer(test_case.input);
      expect(result).toStrictEqual(test_case.output);
    });
  });
});
