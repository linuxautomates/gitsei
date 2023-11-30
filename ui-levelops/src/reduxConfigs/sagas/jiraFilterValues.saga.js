import { all, put, select, take, takeEvery } from "redux-saga/effects";
import {
  JIRA_FILTER_VALUES,
  JIRA_FILTER_VALUES_PRE_FETCHED,
  JIRA_SALESFORCE_FILTER_VALUES,
  JIRA_ZENDESK_FILTER_VALUES,
  LEAD_TIME_FILTER_VALUES
} from "../actions/actionTypes";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { getData, getError, getLoading } from "utils/loadingUtils";
import { get, find } from "lodash";
import { restapiData, restapiError, restapiLoading } from "../actions/restapi";
import { toTitleCase } from "../../utils/stringUtils";
import { IntegrationConfigComplete } from "../../configurations/containers/integration-steps/integrations-details-new/integration-details-new.container";

const restapiState = state => state.restapiReducer;

export function* jiraFiltersEffectSaga(action) {
  const actionId = action.id;
  const complete = `COMPLETE_INTEG_CONFIG_LIST_${actionId}`;
  const integrationIds = get(action, ["data", "integration_ids"], []);

  yield put(actionTypes.genericList(action.uri, action.method, action.data, complete, actionId, false));

  yield take(complete);

  const fieldState = yield select(restapiState);
  if (getError(fieldState, action.uri, action.method, actionId)) {
    return;
  }

  let data = getData(fieldState, action.uri, action.method, actionId);

  let fieldRecords = data.records || [];

  const integConfig = "jira_integration_config";
  const customFilter = "jira_custom_filter_values";
  const projectFilter = "jiraprojects_values";

  const configComplete = `COMPLETE_INTEG_CONFIG_FOR_JIRA_FIELDS_${actionId}`;
  if (integrationIds.length <= 0) {
    data.custom_fields = [];
    data.custom_hygienes = [];
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));

    if (action.complete !== null) {
      yield put({ type: action.complete });
    }

    return;
  }
  yield put(actionTypes.genericList(integConfig, "list", { filter: action.data }, configComplete, actionId, false));

  yield take(configComplete);

  const apiState = yield select(restapiState);
  if (getError(apiState, integConfig, "list", actionId)) {
    data.custom_fields = [];
    data.custom_hygienes = [];
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));

    return;
  }
  const integData = getData(apiState, integConfig, "list", actionId);
  const aggFields = (integData.records || [])
    .reduce((agg, obj) => {
      const fields = get(obj, ["config", "agg_custom_fields"], []);
      agg.push(...fields);
      return agg;
    }, [])
    .filter(field => field.key.includes("customfield_"));
  data.custom_hygienes = (integData.records || []).reduce((agg, obj) => {
    const cHygienes = get(obj, ["custom_hygienes"], []);
    agg.push(...cHygienes);
    return agg;
  }, []);
  const integFields = aggFields.map(record => record.key);
  if (integFields.length === 0) {
    data.custom_fields = [];
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    if (action.complete !== null) {
      yield put({ type: action.complete });
    }
    return;
  }
  const filter = {
    fields: integFields.filter((field, index) => integFields.indexOf(field) === index),
    integration_ids: integrationIds,
    filter: {
      integration_ids: integrationIds
    }
  };

  yield put(actionTypes.genericList(customFilter, "list", filter, complete, actionId, false));

  yield take(complete);

  const project_filter = {
    fields: ["project_name"],
    integration_ids: integrationIds,
    filter: {
      integration_ids: integrationIds
    }
  };

  yield put(actionTypes.genericList(projectFilter, "list", project_filter, complete, actionId, false));

  yield take(complete);

  const projectApiState = yield select(restapiState);
  if (getError(projectApiState, projectFilter, "list", actionId)) {
    yield put(restapiError(true, action.uri, action.method, actionId));
    yield put({ type: action.complete });
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    return;
  }

  const project = find(fieldRecords, "project");

  if (project) {
    const projectData = getData(projectApiState, projectFilter, "list", actionId);
    const modifiedData = get(projectData, ["records", "0", "project_name"], []);
    const updatedProjects = modifiedData.map(item => ({
      key: item.key,
      value: `${toTitleCase(item.additional_key)} (${item.key})`
    }));

    const updatedRecords = fieldRecords.filter(item => item !== project);
    updatedRecords.push({
      project: updatedProjects
    });

    fieldRecords = updatedRecords;
  }

  const customApiState = yield select(restapiState);
  if (getError(customApiState, customFilter, "list", actionId)) {
    data.custom_fields = [];
    yield put(restapiError(true, action.uri, action.method, actionId));
    yield put({ type: action.complete });
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    return;
  }
  const customData = getData(customApiState, customFilter, "list", actionId);
  fieldRecords.push(...(customData.records || []));
  data.records = fieldRecords;
  data.custom_fields = aggFields.filter((field, index) => aggFields.map(f => f.key).indexOf(field.key) === index);

  data._metadata = {
    ...(data._metadata || {}),
    has_next: !!(data?._metadata?.has_next || customData?._metadata?.has_next)
  };

  yield put(restapiData(data, action.uri, action.method, actionId));
  yield put(restapiLoading(false, action.uri, action.method, actionId));
  if (action.complete !== null) {
    yield put({ type: action.complete });
  }
}

// Added this Saga to avoid integration config list API call in case parent component has already called it,
// and clubbing API calls which can be called in parallel
export function* jiraFiltersPreFetchedEffectSaga(action) {
  const actionId = action.id;
  const integrationIds = get(action, ["data", "integration_ids"], []);

  const apiState = yield select(restapiState);

  const integConfig = "jira_integration_config";
  const customFilter = "jira_custom_filter_values";
  const projectFilter = "jiraprojects_values";

  const loading = getLoading(apiState, integConfig, "list", "0");

  let integData = {};
  // Checking If parent component has already called integration config list API
  if (loading) {
    yield take(IntegrationConfigComplete);
    integData = getData(apiState, integConfig, "list", "0");
  } else {
    integData = getData(apiState, integConfig, "list", "0");
    if (!Object.keys(integData).length) {
      const configComplete = `COMPLETE_INTEG_CONFIG_FOR_JIRA_FIELDS_${actionId}`;

      yield put(actionTypes.genericList(integConfig, "list", { filter: action.data }, configComplete, actionId, false));

      yield take(configComplete);

      const apiState = yield select(restapiState);
      if (getError(apiState, integConfig, "list", actionId)) {
        let data = getData(fieldState, action.uri, action.method, actionId);
        data.custom_fields = [];
        yield put(restapiData(data, action.uri, action.method, actionId));
        yield put(restapiLoading(false, action.uri, action.method, actionId));

        return;
      }
      integData = getData(apiState, integConfig, "list", actionId);
    }
  }

  const aggFields = (integData.records || [])
    .reduce((agg, obj) => {
      const fields = get(obj, ["config", "agg_custom_fields"], []);
      agg.push(...fields);
      return agg;
    }, [])
    .filter(field => field.key.includes("customfield_"));

  const integFields = aggFields.map(record => record.key);

  const filter = {
    fields: integFields.filter((field, index) => integFields.indexOf(field) === index),
    integration_ids: integrationIds,
    filter: {
      integration_ids: integrationIds
    }
  };

  const project_filter = {
    fields: ["project_name"],
    integration_ids: integrationIds,
    filter: {
      integration_ids: integrationIds
    }
  };

  let calls = [
    {
      uri: action.uri,
      filter: action.data,
      method: action.method
    },
    {
      uri: projectFilter,
      filter: project_filter,
      method: "list"
    },
    {
      uri: customFilter,
      filter: filter,
      method: "list"
    }
  ];

  if (integFields.length === 0) {
    calls = calls.slice(0, 1);
  }

  yield all(
    calls.map(call =>
      put(actionTypes.genericList(call.uri, action.method, call.filter, `COMPLETE_${call.uri}`, actionId))
    )
  );

  yield all(calls.map(call => take(`COMPLETE_${call.uri}`)));

  const fieldState = yield select(restapiState);

  let data = getData(fieldState, action.uri, action.method, actionId);

  data.custom_hygienes = (integData.records || []).reduce((agg, obj) => {
    const cHygienes = get(obj, ["custom_hygienes"], []);
    agg.push(...cHygienes);
    return agg;
  }, []);

  if (integFields.length === 0) {
    data.custom_fields = [];
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    if (action.complete !== null) {
      yield put({ type: action.complete });
    }
    return;
  }

  let fieldRecords = data.records || [];

  const projectApiState = yield select(restapiState);
  if (getError(projectApiState, projectFilter, "list", actionId)) {
    yield put(restapiError(true, action.uri, action.method, actionId));
    yield put({ type: action.complete });
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    return;
  }

  const project = find(fieldRecords, "project");

  if (project) {
    const projectData = getData(projectApiState, projectFilter, "list", actionId);
    const modifiedData = get(projectData, ["records", "0", "project_name"], []);
    const updatedProjects = modifiedData.map(item => ({
      key: item.key,
      value: `${item.additional_key} (${item.key})`
    }));

    const updatedRecords = fieldRecords.filter(item => item !== project);
    updatedRecords.push({
      project: updatedProjects
    });

    fieldRecords = updatedRecords;
  }

  const customApiState = yield select(restapiState);
  if (getError(customApiState, customFilter, "list", actionId)) {
    data.custom_fields = [];
    yield put(restapiError(true, action.uri, action.method, actionId));
    yield put({ type: action.complete });
    yield put(restapiData(data, action.uri, action.method, actionId));
    yield put(restapiLoading(false, action.uri, action.method, actionId));
    return;
  }
  const customData = getData(customApiState, customFilter, "list", actionId);
  fieldRecords.push(...(customData.records || []));
  data.records = fieldRecords;
  data.custom_fields = aggFields.filter((field, index) => aggFields.map(f => f.key).indexOf(field.key) === index);

  data._metadata = {
    ...(data._metadata || {}),
    has_next: !!(data?._metadata?.has_next || customData?._metadata?.has_next)
  };
  yield put(restapiData(data, action.uri, action.method, actionId));
  yield put(restapiLoading(false, action.uri, action.method, actionId));
  if (action.complete !== null) {
    yield put({ type: action.complete });
  }
}

export function* jiraZendeskFiltersEffectSaga(action) {
  // what is going to be the action.data here?
  // make sure to sort out the fields correctly
  const actionId = action.id;
  yield put(restapiLoading(true, action.uri, action.method, actionId));
  const jiraFields = (action.data.fields || [])
    .filter(field => field.includes("jira"))
    .map(field => field.replace("jira_", ""));
  const zendeskFields = (action.data.fields || [])
    .filter(field => field.includes("zendesk_"))
    .map(field => field.replace("zendesk_", ""));
  yield put(
    actionTypes.widgetFilterValuesGet(
      "jira_filter_values",
      { ...action.data, fields: jiraFields },
      `COMPLETE_JIRA_FILTERS`,
      actionId
    )
  );
  yield take(`COMPLETE_JIRA_FILTERS`);

  // now put the action for zendesk filters with complete
  yield put(
    actionTypes.widgetFilterValuesGet(
      "zendesk_filter_values",
      { ...action.data, fields: zendeskFields },
      `COMPLETE_ZENDESK_FILTERS`,
      actionId
    )
  );
  yield take(`COMPLETE_ZENDESK_FILTERS`);

  const apiState = yield select(restapiState);
  const error = getError(apiState, "jira_filter_values", "list", actionId);
  if (error) {
    yield put(restapiError(true, JIRA_ZENDESK_FILTER_VALUES.toLowerCase(), "list", actionId));
  }
  // eslint-disable-next-line array-callback-return
  const mappedJiraFields = get(apiState, ["jira_filter_values", "list", actionId, "data", "records"], []).map(
    record => {
      const labelKey = Object.keys(record || [null])[0];
      if (labelKey) {
        return {
          [`jira_${labelKey}`]: record[labelKey]
        };
      }
    }
  );
  const mappedZendeskFields = get(apiState, ["zendesk_filter_values", "list", actionId, "data", "records"], []).map(
    record => {
      const labelKey = Object.keys(record || [null])[0];
      if (labelKey) {
        return {
          [`zendesk_${labelKey}`]: record[labelKey]
        };
      }
      return true;
    }
  );

  const jiraCustomFields = get(apiState, ["jira_filter_values", "list", actionId, "data", "custom_fields"], []).map(
    field => {
      return {
        ...(field || {}),
        key: `jira_${field?.key}`
      };
    }
  );

  const zendeskCustomFields = get(
    apiState,
    ["zendesk_filter_values", "list", actionId, "data", "custom_fields"],
    []
  ).map(field => {
    return {
      ...(field || {}),
      key: `zendesk_${field?.key}`
    };
  });

  const hasNext =
    get(apiState, ["jira_filter_values", "list", actionId, "data", "_metadata", "has_next"], false) ||
    get(apiState, ["zendesk_filter_values", "list", actionId, "data", "_metadata", "has_next"], false);

  yield put(
    restapiData(
      {
        records: [...mappedJiraFields, ...mappedZendeskFields],
        _metadata: { has_next: hasNext },
        custom_fields: [...jiraCustomFields, ...zendeskCustomFields]
      },
      action.uri,
      action.method,
      actionId
    )
  );
  yield put(restapiLoading(false, action.uri, action.method, actionId));
}

export function* jiraSalesforceFiltersEffectSaga(action) {
  // what is going to be the action.data here?
  // make sure to sort out the fields correctly
  const actionId = action.id;
  yield put(restapiLoading(true, action.uri, action.method, actionId));
  const jiraFields = (action.data.fields || [])
    .filter(field => field.includes("jira"))
    .map(field => field.replace("jira_", ""));
  const salesforceFields = (action.data.fields || [])
    .filter(field => field.includes("salesforce_"))
    .map(field => field.replace("salesforce_", ""));
  yield put(
    actionTypes.widgetFilterValuesGet(
      "jira_filter_values",
      { ...action.data, fields: jiraFields },
      `COMPLETE_JIRA_FILTERS`,
      actionId
    )
  );
  yield take(`COMPLETE_JIRA_FILTERS`);

  const salesforce_filters_action_id = action.id + "_salesforce";

  // now put the action for salesforce filters with complete
  yield put(
    actionTypes.widgetFilterValuesGet(
      "salesforce_filter_values",
      { ...action.data, fields: salesforceFields },
      `COMPLETE_SALESFORCE_FILTERS`,
      salesforce_filters_action_id
    )
  );
  yield take(`COMPLETE_SALESFORCE_FILTERS`);

  const apiState = yield select(restapiState);
  const mappedJiraFields = get(apiState, ["jira_filter_values", "list", actionId, "data", "records"], []).map(
    record => {
      const labelKey = Object.keys(record || [null])[0];
      if (labelKey) {
        return {
          [`jira_${labelKey}`]: record[labelKey]
        };
      }
      return true;
    }
  );
  const mappedSalesforceFields = get(
    apiState,
    ["salesforce_filter_values", "list", salesforce_filters_action_id, "data", "records"],
    []
  ).map(record => {
    const labelKey = Object.keys(record || [null])[0];
    if (labelKey) {
      return {
        [`salesforce_${labelKey}`]: record[labelKey]
      };
    }
    return true;
  });

  const jiraCustomFields = get(apiState, ["jira_filter_values", "list", actionId, "data", "custom_fields"], []).map(
    field => {
      return {
        ...(field || {}),
        key: `jira_${field?.key}`
      };
    }
  );

  const hasNext =
    get(apiState, ["jira_filter_values", "list", actionId, "data", "_metadata", "has_next"], false) ||
    get(
      apiState,
      ["salesforce_filter_values", "list", salesforce_filters_action_id, "data", "_metadata", "has_next"],
      false
    );

  yield put(
    restapiData(
      {
        records: [...mappedJiraFields, ...mappedSalesforceFields],
        _metadata: { has_next: hasNext },
        custom_fields: jiraCustomFields
      },
      action.uri,
      action.method,
      actionId
    )
  );
  yield put(restapiLoading(false, action.uri, action.method, actionId));
}

export function* leadTimeFiltersEffectSaga(action) {
  // what is going to be the action.data here?
  // make sure to sort out the fields correctly
  const actionId = action.id;
  yield put(restapiLoading(true, action.uri, action.method, actionId));
  const jiraFields = (action.data.fields || [])
    .filter(field => field.includes("jira"))
    .map(field => field.replace("jira_", ""));

  const cicdFields = (action.data.fields || []).filter(field => !field.includes("jira"));

  if (jiraFields.length) {
    yield put(
      actionTypes.widgetFilterValuesGet(
        "jira_filter_values",
        { ...action.data, fields: jiraFields },
        `COMPLETE_JIRA_FILTERS`,
        actionId
      )
    );
    yield take(`COMPLETE_JIRA_FILTERS`);
  }

  // now put the action for salesforce filters with complete
  if (cicdFields.length) {
    yield put(
      actionTypes.widgetFilterValuesGet(
        "github_prs_filter_values",
        { ...action.data, fields: cicdFields },
        `COMPLETE_CICD_FILTERS`,
        actionId
      )
    );
    yield take(`COMPLETE_CICD_FILTERS`);
  }

  const apiState = yield select(restapiState);

  const mappedJiraFields = get(apiState, ["jira_filter_values", "list", actionId, "data", "records"], []).map(
    record => {
      const labelKey = Object.keys(record || [null])[0];
      if (labelKey) {
        if (labelKey.includes("customfield_")) {
          return {
            [labelKey]: record[labelKey]
          };
        } else {
          return {
            [`jira_${labelKey}`]: record[labelKey]
          };
        }
      }
      return true;
    }
  );
  const mappedCicdFields = get(apiState, ["github_prs_filter_values", "list", actionId, "data", "records"], []).map(
    record => {
      const labelKey = Object.keys(record || [null])[0];
      if (labelKey) {
        return {
          [labelKey]: record[labelKey]
        };
      }
      return true;
    }
  );

  const jiraCustomFields = get(apiState, ["jira_filter_values", "list", actionId, "data", "custom_fields"], []).map(
    field => {
      return {
        ...(field || {}),
        key: field?.key
      };
    }
  );

  const hasNext =
    get(apiState, ["jira_filter_values", "list", actionId, "data", "_metadata", "has_next"], false) ||
    get(apiState, ["github_prs_filter_values", "list", actionId, "data", "_metadata", "has_next"], false);

  let data = {
    _metadata: { has_next: hasNext },
    records: []
  };

  if (jiraFields.length) {
    data = {
      ...data,
      records: [...mappedJiraFields],
      custom_fields: jiraCustomFields
    };
  }

  if (cicdFields.length) {
    data = {
      ...data,
      records: [...data.records, ...mappedCicdFields]
    };
  }

  yield put(restapiData(data, action.uri, action.method, actionId));
  yield put(restapiLoading(false, action.uri, action.method, actionId));
}

export function* jiraFiltersWatcherSaga() {
  yield takeEvery([JIRA_FILTER_VALUES], jiraFiltersEffectSaga);
}

export function* jiraFiltersPreFetchedWatcherSaga() {
  yield takeEvery([JIRA_FILTER_VALUES_PRE_FETCHED], jiraFiltersPreFetchedEffectSaga);
}

export function* jiraZendeskFiltersWatcherSaga() {
  yield takeEvery([JIRA_ZENDESK_FILTER_VALUES], jiraZendeskFiltersEffectSaga);
}

export function* jiraSalesforceFiltersWatcherSaga() {
  yield takeEvery([JIRA_SALESFORCE_FILTER_VALUES], jiraSalesforceFiltersEffectSaga);
}

export function* leadTimeFiltersEffectWatcherSaga() {
  yield takeEvery([LEAD_TIME_FILTER_VALUES], leadTimeFiltersEffectSaga);
}
