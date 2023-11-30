import { RestWorkItem } from "classes/RestWorkItem";
import Color from "color";
import { colors } from "constants/colors.constants";
import { get } from "lodash";
import queryString from "query-string";
import { createSelector } from "reselect";
import { isValidHex } from "shared-resources/lib/colors";
import { TENANT_STATE } from "../actions/actionTypes";

export const restapiState = state => state.restapiReducer;

export const emptyObj = { create: {}, update: {}, delete: {}, get: {}, list: {} };

export const getKBSelector = createSelector(restapiState, apis => {
  return {
    get: get(apis, ["bestpractices", "get"], {}),
    list: get(apis, ["bestpractices", "list"], {}),
    create: get(apis, ["bestpractices", "create"], {}),
    update: get(apis, ["bestpractices", "update"], {}),
    delete: get(apis, ["bestpractices", "delete"], {})
  };
});
export const getTagsSelector = createSelector(restapiState, apis => {
  return {
    get: get(apis, ["tags", "get"], {}),
    list: get(apis, ["tags", "list"], {}),
    create: get(apis, ["tags", "create"], {}),
    update: get(apis, ["tags", "update"], {}),
    delete: get(apis, ["tags", "delete"], {}),
    bulk: get(apis, ["tags", "bulk"], {})
  };
});

export const getIntegrationsSelector = createSelector(restapiState, apis => {
  return apis.integrations || emptyObj;
});

export const getCustomFieldsListSelector = createSelector(restapiState, restApi => {
  const defaultResponse = {
    visible: [],
    hidden: []
  };

  const customFieldListData = get(restApi, ["custom_fields", "list", "0", "data", "records"], []);

  if (customFieldListData.length === 0) {
    return defaultResponse;
  }

  return restApi.custom_fields.list["0"].data.records.reduce((acc, item) => {
    if (item.hidden) {
      acc.hidden.push(item);
      return acc;
    }
    acc.visible.push(item);
    return acc;
  }, defaultResponse);
});

const workItemsState = state => get(state.restapiReducer, ["workitem", "list", "0", "data", "records"], []);
export const getWorkItemsListSelector = createSelector(workItemsState, workItems => {
  if (!workItems || !workItems.length) {
    return [];
  }

  return workItems.map((record, i) => {
    let color = "grey";
    if (isValidHex(colors[i % 20]) && isValidHex(colors[i % 9])) {
      color = Color(colors[i % 20]).mix(Color(colors[i % 9]));
    }

    return {
      ...record,
      assignee: record.assignee || "user@gmail.com",
      assignees: record.assignees.map(a => ({
        ...a,
        color: Color(colors[a.user_email.charCodeAt(0) % 20]).mix(Color(colors[a.user_email.charCodeAt(0) % 9]))
      })),
      name: record.title || ``,
      color: record.color || color
    };
  });
});

export const getProductsBulkListSelector = createSelector(restapiState, apis => {
  return get(apis, ["products", "bulk", "0", "data", "records"], []);
});

export const getTenantStateSelector = createSelector(restapiState, state => {
  return get(state, [TENANT_STATE?.toLowerCase(), "get", "0", "data"], []);
});

export const getActivityLogsSelector = createSelector(restapiState, apis => {
  return get(apis, ["activitylogs", "list", "0", "data", "records"], []);
});

const workItemState = (state, ownProps) => {
  if (
    ownProps.workId &&
    state.restapiReducer.workitem.get &&
    state.restapiReducer.workitem.get[ownProps.workId.toString()]
  ) {
    return state.restapiReducer.workitem.get[ownProps.workId.toString()];
  }

  return null;
};

const workItemCreateState = state => {
  return get(state.restapiReducer, ["workitem", "create", "0"], { loading: true, error: false });
};

export const getWorkItemCreateSelector = createSelector(workItemCreateState, data => {
  return data;
});

export const getWorkItemSelector = createSelector(workItemState, workItem =>
  workItem && workItem.data ? new RestWorkItem(workItem.data) : new RestWorkItem(null)
);

export const getSmartTicketTemplatesSelector = createSelector(restapiState, apis => {
  return {
    ...apis.ticket_templates
  };
});

export const getSmartTicketFieldsSelector = createSelector(restapiState, restApi => {
  return get(restApi, ["workitem", "create", 0, "data"], {});
});

const genericPaginatorState = (state, ownProps) => {
  if (ownProps.uri) {
    const method = ownProps.method || "list";
    const id = ownProps.uuid || "0";
    return get(state.restapiReducer, [ownProps.uri, method, id], { loading: true });
  }
};

const _genericPaginatorSearchState = (state, ownProps) => {
  if (ownProps.searchURI) {
    const method = ownProps.method || "list";
    const id = ownProps.uuid || "0";
    return get(state.restapiReducer, [ownProps.searchURI, method, id], { loading: true });
  }
};

export const genericPaginationSeachData = createSelector(_genericPaginatorSearchState, data => {
  return data;
});

export const genericPaginationData = createSelector(genericPaginatorState, data => {
  return data;
});

const getAssessmentCheckQuiz = (state, ownProps) => {
  if (ownProps.value && ownProps.value.assessment_template_id) {
    const questionnaire = get(state.restapiReducer, ["questionnaires", "get", ownProps.value.assessment_template_id], {
      loading: true,
      error: false,
      data: {}
    });
    let sections = [];
    if (!questionnaire.loading && questionnaire.data.sections) {
      questionnaire.data.sections.forEach(section => {
        const sectionDict = get(state.restapiReducer, ["sections", "get", section], {
          loading: true,
          error: false,
          data: {}
        });
        sections.push({
          loading: sectionDict.loading !== undefined ? sectionDict.loading : true,
          error: sectionDict.error !== undefined ? sectionDict.error : true,
          data: sectionDict.data
        });
      });
    }
    return {
      questionnaire: questionnaire,
      sections: sections
    };
  }
  return { questionnaire: { loading: true, error: false }, sections: [] };
};

export const getAssessmentCheckData = createSelector(getAssessmentCheckQuiz, data => {
  return data;
});

export const configsState = state => {
  return get(state.restapiReducer, ["configs", "list", 0], { loading: true, error: true });
};

export const dashboardListState = (state, key = 0) => {
  return get(state.restapiReducer, ["dashboards", "list", key], { loading: true, error: true });
};

export const completeDashboardListState = state => {
  return get(state.restapiReducer, ["dashboards", "list"], {});
};

export const dashboardState = (state, ownProps) => {
  if (ownProps.location && ownProps.location.search) {
    const dashboardId = queryString.parse(ownProps.location.search).id || "0";
    return get(state.restapiReducer, ["dashboards", "get", dashboardId], { loading: true, error: true });
  }

  return { loading: true, error: true };
};

export const dashboardCreateState = (state, ownProps) => {
  return get(state.restapiReducer, ["dashboards", "create", 0], { loading: true, error: true });
};

export const dashboardUpdateState = (state, ownProps) => {
  if (ownProps.location && ownProps.location.search) {
    const dashboardId = queryString.parse(ownProps.location.search).id || "0";
    return get(state.restapiReducer, ["dashboards", "update", dashboardId], { loading: true, error: true });
  }

  return { loading: true, error: true };
};

export const dashboardViewState = (state, ownProps) => {
  if (ownProps.match.params && ownProps.match.params.id) {
    const dashboardId = ownProps.match.params.id || "0";
    return get(state.restapiReducer, ["dashboards", "get", dashboardId], { loading: true, error: true });
  }
  return { loading: true, error: true };
};

export const dashboardReportUploadState = (state, ownProps) => {
  return get(state.restapiReducer, ["dashboard_reports", "upload"], {});
};

export const getDashboardSelector = createSelector(dashboardState, dashboard => dashboard);

export const getMentionsSearchResults = (state, uri, method, id) => {
  return get(state.restapiReducer, [uri, method, id], { loading: true, error: true });
};

export const customFieldsState = (state, ownProps) => {
  const integrationId = get(ownProps, ["value", "integration_id"], undefined);
  let resultData = {};
  if (integrationId) {
    resultData.integration = get(state.restapiReducer, ["integrations", "get", integrationId], {
      loading: true,
      error: false
    });
  }
  resultData.jira_filter_values = get(state.restapiReducer, ["jira_filter_values", "list", "0"], {
    loading: true,
    error: false
  });
  return resultData;
};

export const customFieldsSelector = createSelector(customFieldsState, data => data);

export const quizListState = (state, ownProps) => {
  const id = ownProps.workItemId;
  if (!id) {
    return { loading: true, error: false };
  }
  return get(state.restapiReducer, ["quiz", "list", id], { loading: true, error: false });
};

export const tagsListState = (state, id) => {
  if (!id) {
    return { loading: true, error: false };
  }
  return get(state.restapiReducer, ["tags", "list", id], { loading: true, error: false });
};

export const subTicketsListState = (state, ownProps) => {
  const id = ownProps.workItemId;
  if (!id) {
    return { loading: true, error: false };
  }
  return get(state.restapiReducer, ["workitem", "list", id], { loading: true, error: false });
};

export const notesListState = (state, ownProps) => {
  return get(state.restapiReducer, ["notes", "list", "0"], { loading: true, error: false });
};

export const notesCreateState = state => {
  return get(state.restapiReducer, ["notes", "create"], { loading: true, error: false });
};

export const activityLogsListState = (state, ownProps) => {
  const id = ownProps.workItemId;
  if (!id) {
    return { loading: true, error: false };
  }
  return get(state.restapiReducer, ["activity_logs", "list", id], { loading: true, error: false });
};

export const workItemSelectorState = (state, ownProps) => {
  // if (!ownProps.workItem) {
  //   return 0;
  //
  // }
  return {
    workItemRestApi: {
      upload: get(state.restapiReducer, ["workitem", "upload"]),
      patch: get(state.restapiReducer, ["workitem", "patch"]),
      delete: get(state.restapiReducer, ["workitem", "delete"])
    },
    tags: get(state.restapiReducer, "tags"),
    users: get(state.restapiReducer, "users")
  };
};

export const workItemSelector = createSelector(workItemSelectorState, data => data);

export const configTableState = (state, listId, getId) => {
  const listState = get(state.restapiReducer, ["config_tables", "list", listId], { loading: true, error: false });
  const getState = get(state.restapiReducer, ["config_tables", "get", `${getId}?expand=schema`], {
    loading: true,
    error: false
  });
  return {
    list: listState,
    get: getState
  };
};

export const triageRulesDeleteState = state => {
  const deleteState = get(state.restapiReducer, ["triage_rules", "delete"], {});
  return deleteState;
};

export const triageRulesBulkDeleteState = state => {
  return get(state.restapiReducer, ["triage_rules", "bulkDelete"], {});
};

export const triageRuleIdState = (state, location) => {
  const create = get(state.restapiReducer, ["triage_rules", "create", "0"], { loading: true, error: false });
  if (location && location.search) {
    const ruleId = queryString.parse(location.search).rule || "0";
    const rule = get(state.restapiReducer, ["triage_rules", "get", ruleId], { loading: true, error: false });
    const update = get(state.restapiReducer, ["triage_rules", "update", ruleId], { loading: true, error: false });
    return {
      triage_rule: rule,
      triage_rule_update: update,
      stages: []
    };
  }
  return { triage_rule: { loading: true, error: false }, stages: [], triage_rule_create: create };
};

export const configTableViewState = (state, ownProps) => {
  if (ownProps.location && ownProps.location.search) {
    const tableId = queryString.parse(ownProps.location.search).id || "0";
    const version = queryString.parse(ownProps.location.search).version;
    const uri =
      version !== undefined
        ? `${tableId}/revisions/${version}?expand=schema,rows,history`
        : `${tableId}?expand=schema,rows,history`;
    return get(state.restapiReducer, ["config_tables", "get", uri], {
      loading: true,
      error: true
    });
  }
  return { loading: true, error: true };
};

export const configTableCreateState = state => {
  return get(state.restapiReducer, ["config_tables", "create", 0], { loading: true, error: true });
};

export const configTablesBulkDeleteState = state => {
  return get(state.restapiReducer, ["config_tables", "bulkDelete", "0"], { loading: true, error: true });
};

export const configTableUpdateState = (state, ownProps) => {
  if (ownProps.location && ownProps.location.search) {
    const tableId = queryString.parse(ownProps.location.search).id || "0";
    return get(state.restapiReducer, ["config_tables", "update", tableId], { loading: true, error: true });
  }

  return { loading: true, error: true };
};

export const configTablesListState = state => {
  return get(state.restapiReducer, ["config_tables", "list"], {});
};

export const configTablesGetState = state => {
  return get(state.restapiReducer, ["config_tables", "get"], {});
};

export const configTablesDeleteState = state => {
  return get(state.restapiReducer, ["config_tables", "delete"], {});
};

export const propelsSelectorState = state => {
  const listState = get(state.restapiReducer, ["propels", "list"], {});
  const createState = get(state.restapiReducer, ["propels", "create"], {});
  const updateState = get(state.restapiReducer, ["propels", "update"], {});
  const deleteState = get(state.restapiReducer, ["propels", "detele"], {});
  return {
    list: listState,
    create: createState,
    update: updateState,
    delete: deleteState
  };
};

export const propelsSelector = createSelector(propelsSelectorState, data => data);

export const pipelineJobRunsLogsIdState = (state, location) => {
  if (location && location.search) {
    const id = queryString.parse(location.search).job_id || "0";
    return get(state.restapiReducer, ["pipeline_job_runs_logs", "get", id], { loading: true, error: true });
  }
  return {};
};

export const pipelineJobRunsStageIdState = (state, location) => {
  if (location && location.search) {
    const id = queryString.parse(location.search).stage_id || "0";
    return get(state.restapiReducer, ["pipeline_job_runs_stages_logs", "get", id], { loading: true, error: true });
  }
  return {};
};

export const triageRuleResultsCountState = (state, location) => {
  if (location && location.search) {
    const query = { ...queryString.parse(location.search) };
    let id = -1;
    if (query.hasOwnProperty("job_id")) {
      id = query.job_id;
    } else if (query.hasOwnProperty("stage_id")) {
      id = query.stage_id;
    }

    if (id !== -1) {
      return get(state.restapiReducer, ["triage_rule_results", "list", id], { loading: true, error: true });
    }
  }
  return { loading: true, error: true };
};

export const propelRunsLogsState = (state, id = "0") => {
  return get(state.restapiReducer, ["propel_runs_logs", "list", id], { loading: true, error: true });
};

export const propelGetState = (state, id) => {
  return get(state.restapiReducer, ["propels", "get", id], { loading: true, error: true });
};

export const propelDeleteState = (state, id) => {
  return get(state.restapiReducer, ["propels", "delete", id], { loading: true });
};

export const propelBulkDeleteState = state => {
  return get(state.restapiReducer, ["propels", "bulkDelete", "0"], { loading: true, error: true });
};

export const propelCreateState = (state, id = "0") => {
  return get(state.restapiReducer, ["propels", "create", id], { loading: true, error: true });
};

export const workItemDeleteState = (state, id) => {
  return get(state.restapiReducer, ["workitem", "delete", id], { loading: true });
};

export const workItemListState = (state, id) => {
  return get(state.restapiReducer, ["workitem", "list", id], { loading: true });
};

// Automation Rules
export const automationRuleCreateState = (state, id = "0") => {
  return get(state.restapiReducer, ["automation_rules", "create", id], { loading: true, error: true });
};

export const automationRuleGetState = (state, id) => {
  return get(state.restapiReducer, ["automation_rules", "get", id], { loading: true, error: true });
};

export const automationRuleDeleteState = (state, id) => {
  return get(state.restapiReducer, ["automation_rules", "delete", id], { loading: true });
};

export const KbsSelectorState = (state, id = "0") => {
  const getState = state.restapiReducer?.bestpractices?.get?.[id] || {};
  const listState = state.restapiReducer?.bestpractices?.list?.[0] || {};
  const updateState = state.restapiReducer?.bestpractices?.update;
  const createState = state.restapiReducer?.bestpractices?.create?.[0] || {};
  const uploadState = state.restapiReducer?.bestpractices?.upload || {};
  const deleteState = state.restapiReducer?.bestpractices?.delete || {};
  const bulkDeletingState = state.restapiReducer?.bestpractices?.bulkDelete || {};

  return {
    get: getState,
    list: listState,
    create: createState,
    update: updateState,
    upload: uploadState,
    delete: deleteState,
    bulkDelete: bulkDeletingState
  };
};

export const integrationsSelectorState = (state, id = "0") => ({
  list: state.restapiReducer?.integrations?.list?.[id] || {}
});

export const questionnariesSelectorState = state => ({
  get: state.restapiReducer?.questionnaires?.get || {},
  list: state.restapiReducer?.questionnaires?.list || {},
  create: state.restapiReducer?.questionnaires?.create || {},
  update: state.restapiReducer?.questionnaires?.update,
  upload: state.restapiReducer?.questionnaires?.upload || {},
  delete: state.restapiReducer?.questionnaires?.delete || {}
});

export const SmartTicketSelectorState = (state, id = "0") => {
  const getState = state.restapiReducer?.ticket_templates?.get?.[id] || {};
  const listState = state.restapiReducer?.ticket_templates?.list || {};
  const updateState = state.restapiReducer?.ticket_templates?.update?.[id];
  const createState = state.restapiReducer?.ticket_templates?.create?.[id] || {};
  const deleteState = state.restapiReducer?.ticket_templates?.delete || {};
  const bulkDeleteState = state.restapiReducer?.ticket_templates?.bulkDelete || {};

  return {
    get: getState,
    list: listState,
    create: createState,
    update: updateState,
    delete: deleteState,
    bulkDelete: bulkDeleteState
  };
};

export const questionnairesStateSelector = state => {
  const listState = get(state.restapiReducer, ["questionnaires", "list"], {});
  const createState = get(state.restapiReducer, ["questionnaires", "create"], {});
  const updateState = get(state.restapiReducer, ["questionnaires", "update"], {});
  const deleteState = get(state.restapiReducer, ["questionnaires", "delete"], {});
  const exportState = get(state.restapiReducer, ["questionnaires", "export"], {});
  const importState = get(state.restapiReducer, ["questionnaires", "import"], {});
  const getState = get(state.restapiReducer, ["questionnaires", "get"], {});
  const bulkDeleteState = get(state.restapiReducer, ["questionnaires", "bulkDelete"], {});
  return {
    list: listState,
    create: createState,
    update: updateState,
    delete: deleteState,
    export: exportState,
    import: importState,
    get: getState,
    bulkDelete: bulkDeleteState
  };
};

export const questionnairesSectionStateSelector = state => {
  const listState = get(state.restapiReducer, ["sections", "list"], {});
  const getState = get(state.restapiReducer, ["sections", "get"], {});
  const createState = get(state.restapiReducer, ["sections", "create"], {});
  const updateState = get(state.restapiReducer, ["sections", "update"], {});
  const deleteState = get(state.restapiReducer, ["sections", "delete"], {});
  return {
    list: listState,
    get: getState,
    create: createState,
    update: updateState,
    delete: deleteState
  };
};

export const ProductsSelectorState = (state, id = "0") => ({
  list: state?.restapiReducer?.products?.list?.[id] || {},
  create: state?.restapiReducer?.products?.create || {},
  update: state?.restapiReducer?.products?.update || {},
  delete: state?.restapiReducer?.products?.delete || {},
  get: state?.restapiReducer?.products?.get || {},
  bulkDelete: state?.restapiReducer?.products?.bulkDelete || {}
});

export const MappingsSelectorState = (state, id = "0") => ({
  list: state?.restapiReducer?.mappings?.list?.[id] || {},
  delete: state?.restapiReducer?.mappings?.delete || {},
  get: state?.restapiReducer?.mappings?.get || {}
});

export const IntegrationsSelectorState = (state, id = "0") => ({
  list: state?.restapiReducer?.integrations?.list?.[id] || {},
  delete: state?.restapiReducer?.integrations?.delete || {},
  get: state?.restapiReducer?.integrations?.get || {}
});

export const UserSelectorState = (state, id = "0") => ({
  list: state?.restapiReducer?.users?.list?.[id] || {},
  delete: state?.restapiReducer?.users?.delete || {},
  get: state?.restapiReducer?.users?.get || {}
});

export const propelReportsSelector = createSelector(restapiState, data => get(data, ["propel_reports"], {}));

export const propelBulkDeleteSelector = createSelector(propelReportsSelector, propels =>
  get(propels, ["bulkDelete"], {})
);
