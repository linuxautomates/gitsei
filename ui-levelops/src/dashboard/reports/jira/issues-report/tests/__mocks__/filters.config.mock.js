export const filtersConfigMock = [
  {
    id: "projects",
    beKey: "projects",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Projects",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "none",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "project",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jiraprojects_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "project",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "statuses",
    beKey: "statuses",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Status",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "status",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "status",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "priorities",
    beKey: "priorities",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Priority",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "priority",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "priority",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "issue_types",
    beKey: "issue_types",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Issue Type",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "issue_type",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "issue_type",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "assignees",
    beKey: "assignees",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Assignee",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "assignee",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "assignee",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "components",
    beKey: "components",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Component",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "components",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "component",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "labels",
    beKey: "labels",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Label",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "labels",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "label",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "reporters",
    beKey: "reporters",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Reporter",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "reporter",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "reporter",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "fix_versions",
    beKey: "fix_versions",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Fix Version",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "fix_versions",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "fix_version",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "versions",
    beKey: "versions",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Affects Version",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "versions",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "version",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "resolutions",
    beKey: "resolutions",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Resolution",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "resolution",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "resolution",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "status_categories",
    beKey: "status_categories",
    renderComponent: undefined,
    apiContainer: () => {},
    label: "Status Category",
    tab: "filters",
    type: "API_DROPDOWN",
    labelCase: "title_case",
    deleteSupport: true,
    partialSupport: true,
    excludeSupport: true,
    partialKey: "status_category",
    supportPaginatedSelect: true,
    apiFilterProps: () => {},
    filterMetaData: {
      selectMode: "multiple",
      uri: "jira_filter_values",
      method: "list",
      payload: () => {},
      options: () => {},
      specialKey: "status_category",
      sortOptions: true,
      createOption: true
    }
  },
  {
    id: "visualization",
    renderComponent: undefined,
    label: "Visualization",
    beKey: "visualization",
    labelCase: "title_case",
    filterMetaData: {
      selectMode: "default",
      options: () => {},
      sortOptions: false
    },
    tab: "settings"
  },
  {
    id: "show_value_on_bar",
    renderComponent: () => {},
    label: "Bar Chart Options",
    beKey: "show_value_on_bar",
    labelCase: "title_case",
    updateInWidgetMetadata: true,
    isFEBased: true,
    defaultValue: true,
    filterMetaData: { checkboxLabel: "Show value above bar" },
    hideFilter: () => {},
    tab: "settings"
  },
  {
    id: "issue_due_at",
    renderComponent: {
      $$typeof: Symbol("react.memo"),
      type: () => {},
      compare: null
    },
    label: "Jira Due Date",
    beKey: "issue_due_at",
    labelCase: "title_case",
    deleteSupport: true,
    filterMetaData: { slicing_value_support: true, selectMode: "default", options: [] },
    tab: "filters"
  },
  {
    id: "filter_across_values",
    renderComponent: () => {},
    label: "X-Axis Labels",
    beKey: "filter_across_values",
    labelCase: "title_case",
    deleteSupport: true,
    defaultValue: true,
    disabled: () => {},
    filterMetaData: { checkboxLabel: "Display only filtered values" },
    tab: "settings"
  },
  {
    id: "issue_resolved_at",
    renderComponent: {
      $$typeof: Symbol("react.memo"),
      type: () => {},
      compare: null
    },
    label: "Issue Resolved In",
    beKey: "issue_resolved_at",
    labelCase: "title_case",
    deleteSupport: true,
    filterMetaData: { slicing_value_support: true, selectMode: "default", options: [] },
    tab: "filters"
  },
  {
    id: "issue_created_at",
    renderComponent: {
      $$typeof: Symbol("react.memo"),
      type: () => {},
      compare: null
    },
    label: "Issue Created In",
    beKey: "issue_created_at",
    labelCase: "title_case",
    deleteSupport: true,
    filterMetaData: { slicing_value_support: true, selectMode: "default", options: [] },
    tab: "filters"
  },
  {
    id: "issue_updated_at",
    renderComponent: {
      $$typeof: Symbol("react.memo"),
      type: () => {},
      compare: null
    },
    label: "Issue Updated In",
    beKey: "issue_updated_at",
    labelCase: "title_case",
    deleteSupport: true,
    filterMetaData: { slicing_value_support: true, selectMode: "default", options: [] },
    tab: "filters"
  },
  {
    id: "metric",
    renderComponent: undefined,
    label: "Metric",
    beKey: "metric",
    labelCase: "upper_case",
    filterMetaData: {
      selectMode: "default",
      options: [
        {
          label: "Number of tickets",
          value: "ticket"
        },
        {
          label: "Sum of story points",
          value: "story_point"
        }
      ],
      sortOptions: true
    },
    tab: "metrics"
  },
  {
    id: "stacks",
    renderComponent: undefined,
    label: "Stacks",
    beKey: "stacks",
    labelCase: "title_case",
    disabled: () => {},
    filterMetaData: {
      clearSupport: true,
      options: () => {},
      sortOptions: true
    },
    tab: "aggregations"
  },
  {
    id: "across",
    renderComponent: undefined,
    label: "X-Axis",
    beKey: "across",
    labelCase: "title_case",
    required: true,
    getMappedValue: () => {},
    filterMetaData: {
      selectMode: "default",
      options: () => {},
      sortOptions: true
    },
    tab: "aggregations"
  },
  {
    id: "effort_investment_profile",
    renderComponent: () => {},
    label: "Effort Investment Profile",
    beKey: "ticket_categorization_scheme",
    labelCase: "none",
    filterMetaData: {
      categorySelectionMode: "multiple",
      showDefaultScheme: false,
      withProfileCategory: true,
      isCategoryRequired: false,
      allowClearEffortInvestmentProfile: true
    },
    tab: "settings"
  },
  {
    id: "story_points",
    renderComponent: () => {},
    label: "Story Points",
    beKey: "story_points",
    labelCase: "none",
    filterMetaData: {},
    deleteSupport: true,
    tab: "filters",
    apiFilterProps: () => {}
  },
  {
    id: "parent_story_points",
    renderComponent: () => {},
    label: "Parent Story Points",
    beKey: "parent_story_points",
    labelCase: "none",
    filterMetaData: {},
    deleteSupport: true,
    tab: "filters",
    apiFilterProps: () => {}
  },
  {
    id: "links",
    renderComponent: undefined,
    label: "Dependency Analysis",
    beKey: "links",
    labelCase: "none",
    deleteSupport: true,
    getMappedValue: () => {},
    filterMetaData: {
      options: [
        {
          label: "Select issues blocked by filtered issues",
          value: "blocks"
        },
        {
          label: "Select issues blocking filtered issues",
          value: "is blocked by"
        },
        {
          label: "Select issues related to filtered issues",
          value: "relates to"
        }
      ],
      selectMode: "default",
      sortOptions: false,
      clearSupport: true,
      mapFilterValueForBE: () => {}
    },
    apiFilterProps: () => {},
    tab: "filters"
  },
  {
    id: "hygiene_types",
    renderComponent: undefined,
    label: "Hygiene",
    beKey: "hygiene_types",
    labelCase: "title_case",
    deleteSupport: true,
    filterMetaData: {
      options: [
        {
          label: "IDLE",
          value: "IDLE"
        },
        {
          label: "POOR_DESCRIPTION",
          value: "POOR_DESCRIPTION"
        },
        {
          label: "NO_DUE_DATE",
          value: "NO_DUE_DATE"
        },
        {
          label: "NO_ASSIGNEE",
          value: "NO_ASSIGNEE"
        },
        {
          label: "NO_COMPONENTS",
          value: "NO_COMPONENTS"
        },
        {
          label: "MISSED_RESPONSE_TIME",
          value: "MISSED_RESPONSE_TIME"
        },
        {
          label: "MISSED_RESOLUTION_TIME",
          value: "MISSED_RESOLUTION_TIME"
        },
        {
          label: "INACTIVE_ASSIGNEES",
          value: "INACTIVE_ASSIGNEES"
        }
      ],
      sortOptions: true,
      selectMode: "multiple"
    },
    apiFilterProps: () => {},
    tab: "filters"
  },
  {
    id: "issue_management_system",
    renderComponent: () => {},
    label: "Issue Management System",
    beKey: "issue_management_system",
    labelCase: "title_case",
    disabled: () => {},
    getMappedValue: () => {},
    defaultValue: "jira",
    filterMetaData: {
      selectMode: "default",
      options: [
        {
          label: "Azure",
          value: "azure_devops"
        },
        {
          label: "Jira",
          value: "jira"
        }
      ],
      sortOptions: true
    },
    tab: "settings"
  },
  {
    id: "sort_xaxis",
    renderComponent: undefined,
    label: "Sort X-Axis",
    beKey: "sort_xaxis",
    labelCase: "title_case",
    filterMetaData: { selectMode: "default", options: () => {} },
    tab: "settings"
  },
  {
    id: "week_date_format",
    renderComponent: () => {},
    label: "Week Date Format",
    beKey: "weekdate_format",
    defaultValue: "date-format",
    labelCase: "title_case",
    hideFilter: () => {},
    updateInWidgetMetadata: true,
    filterMetaData: { selectMode: "default", options: () => {} },
    tab: "settings"
  },
  {
    id: "epics",
    renderComponent: undefined,
    label: "Epics",
    beKey: "epics",
    labelCase: "none",
    deleteSupport: true,
    filterMetaData: {
      options: [],
      selectMode: "multiple",
      sortOptions: false,
      createOption: true
    },
    apiFilterProps: () => {},
    tab: "filters"
  },
  {
    id: "max_records",
    renderComponent: undefined,
    label: "Max X-Axis Entries",
    beKey: "max_records",
    labelCase: "none",
    updateInWidgetMetadata: true,
    filterMetaData: {
      selectMode: "default",
      options: [
        {
          label: "10",
          value: 10
        },
        {
          label: "20",
          value: 20
        },
        {
          label: "50",
          value: 50
        },
        {
          label: "100",
          value: 100
        }
      ],
      sortOptions: false
    },
    tab: "settings"
  },
  {
    id: "ou_filter",
    renderComponent: {
      $$typeof: Symbol("react.memo"),
      type: () => {},
      compare: null
    },
    filterInfo: "Propelo uses the default field for Collection base aggregations. Override Collection fields here.",
    label: "",
    beKey: "ou_user_filter_designation",
    labelCase: "none",
    filterMetaData: {
      filtersByApplications: {
        jira: {
          options: [
            {
              label: "Reporter",
              value: "reporter"
            },
            {
              label: "Assignee",
              value: "assignee"
            }
          ]
        }
      }
    },
    tab: "settings",
    defaultValue: undefined
  }
];
