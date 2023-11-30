export const genericDrilldownTransformMock = {
  parameters: {
    drillDownProps: {
      application: "github_prs",
      dashboardId: "303",
      widgetId: "f658e800-2b28-11eb-a467-15afedf1c69e",
      x_axis: "levelops/commons-levelops",
      github_prs: "levelops/commons-levelops",
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
    },
    widget: {
      id: "f658e800-2b28-11eb-a467-15afedf1c69e",
      name: "Github/Bitbucket PRS Report Testing",
      type: "github_prs_report",
      query: { across: "repo_id" },
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
    },
    dashboardQuery: { product_id: "80", integration_ids: ["203", "203", "164"] }
  },
  response: {
    acrossValue: "repo_id",
    filters: {
      filter: { product_id: "80", integration_ids: ["203", "203", "164"], repo_ids: ["levelops/commons-levelops"] },
      across: "repo_id"
    }
  }
};

export const genericDrilldownTransformMock2 = {
  parameters: {
    drillDownProps: {
      application: "github_prs",
      dashboardId: "303",
      widgetId: "2c41dee0-2b29-11eb-a467-15afedf1c69e",
      x_axis: "01/13",
      github_prs: "01/13",
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
      }
    },
    widget: {
      id: "2c41dee0-2b29-11eb-a467-15afedf1c69e",
      name: "Github/Bitbucket PRS Report Trends Testing",
      type: "github_prs_report_trends",
      query: { across: "pr_created" },
      order: 3,
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
    dashboardQuery: { product_id: "80", integration_ids: ["203", "203", "164"] }
  },
  response: {
    acrossValue: "pr_created",
    filters: {
      filter: { product_id: "80", integration_ids: ["203", "203", "164"], pr_created: ["01/13"] },
      across: "pr_created"
    }
  }
};

export const githubFilesDrilldownTransformerMock = {
  parameters: {
    drillDownProps: {
      application: "github_files",
      dashboardId: "303",
      widgetId: "cb74b910-2b29-11eb-a467-15afedf1c69e",
      x_axis: "levelops/commons-levelops",
      github_files: "levelops/commons-levelops",
      widgetMetaData: {
        order: 6,
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
    widget: {
      id: "cb74b910-2b29-11eb-a467-15afedf1c69e",
      name: "Github/Bitbucket Files  Report Testing",
      type: "scm_files_report",
      query: { across: "repo_id" },
      order: 6,
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
        order: 6,
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
    dashboardQuery: { product_id: "80", integration_ids: ["203", "203", "164"] }
  },
  response: {
    acrossValue: "repo_id",
    filters: {
      filter: {
        across: "repo_id",
        product_id: "80",
        integration_ids: ["203", "203", "164"],
        repo_ids: ["levelops/commons-levelops"]
      },
      across: "repo_id"
    }
  }
};
