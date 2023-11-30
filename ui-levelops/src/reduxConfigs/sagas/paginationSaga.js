import { restapiData } from "../actions/restapi";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { all, put, select, take, takeEvery } from "redux-saga/effects";
import { issueContextTypes, notifyRestAPIError, severityTypes } from "bugsnag";
import { getData, getError } from "../../utils/loadingUtils";
import { DEV_PRODUCTIVITY_SCORE_REPORT_LIST, PAGINATION_GET } from "../actions/actionTypes";
import { getContentType, PRIMITIVE_CONTENT_TYPES } from "../../classes/RestPropel";
import { get, reduce, filter, forEach, uniqBy, uniq, map, cloneDeep, unset } from "lodash";
import { RBAC } from "constants/localStorageKeys";
import {
  JIRA_SPRINT_REPORTS,
  SPRINT_JIRA_ISSUE_KEYS,
  AZURE_SPRINT_REPORTS,
  LEAD_TIME_REPORTS
} from "dashboard/constants/applications/names";
import { deriveSprintJiraIssueKeysHelper } from "./saga-helpers/paginationSaga.helper";
import { DEV_PRODUCTIVITY_REPORTS } from "../../dashboard/constants/applications/names";
import { VELOCITY_CONFIG_LIST_ID, VELOCITY_CONFIGS } from "reduxConfigs/selectors/velocityConfigs.selector";
import { handleError } from "helper/errorReporting.helper";

const restapiState = state => state.restapiReducer;

export function* paginationEffectSaga(action) {
  try {
    // console.log(state);
    // console.log(action);
    const uri = action.uri;
    const method = action.method;
    const filters = cloneDeep(action.filters);
    const queryparams = action?.payload?.queryParam || {};
    const id = action.id || "0";
    const currentPage = get(filters, ["page"], 0);
    const deriveOnly = action.deriveOnly;
    const complete = `COMPLETE_${uri}_${method}_pagination_${id}`;
    const rbac = localStorage.getItem(RBAC);
    const report = get(action.payload || {}, ["report"], "");
    const sprintUri = report.includes("azure_") ? "issue_management_list" : "jira_tickets";
    if (
      [
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_TREND,
        AZURE_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND
      ].includes(report)
    ) {
      // Sprint endpoint do not consider across filter so removing it from request to avoid confusions
      delete filters.across;
      if ((deriveOnly || []).includes(SPRINT_JIRA_ISSUE_KEYS)) {
        filters["page"] = 0;
      }
    }
    // This is done for csv of developer productivity widget
    if ([DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_SCORE_REPORT].includes(uri)) {
      yield put({
        type: DEV_PRODUCTIVITY_SCORE_REPORT_LIST,
        uri: "dev_productivity_score_report",
        method: "list",
        id: id,
        data: action?.filters,
        complete
      });
    } else {
      yield put(actionTypes.genericList(uri, method, filters, complete, id, true, queryparams));
    }
    yield take(complete);

    if (!action.derive) {
      if (action.hasOwnProperty("complete") && action.complete !== null) {
        yield put({ type: action.complete });
      }
      // i dont want to do any additional fetching for ids
      return;
    }

    const shouldDerive = item => {
      if (deriveOnly === "all") {
        return true;
      }
      if (Array.isArray(deriveOnly)) {
        const value = deriveOnly.includes(item);
        return value;
      }
      return true;
    };

    const lstate = yield select(restapiState);

    if (getError(lstate, uri, method, id)) {
      console.log(`Ran into error for ${uri} ${id}`);
      return;
    }
    const listData = getData(lstate, uri, method, id);
    const listRecords =
      uri === "dev_productivity_report_drilldown" ? listData.records[0]?.records || [] : listData.records || [];

    //yield put(restapiData(listData, uri, method, id));
    //yield put(actionTypes.restapiLoading(false, uri, method, id, null));

    if (listRecords.length > 0) {
      let allDerived = [];

      if (["quiz_aggs_response_time_report", "quiz_aggs_response_time_table_report"].includes(action.uri)) {
        if (["assignee", "questionnaire_template_id"].includes(action.filters.across)) {
          const resultId = id === "0" ? "bulk" : id;
          let complete = `PAGINATION_quize_${uri}_${resultId}`;
          const ids = listRecords
            .reduce((acc, obj) => {
              if (!acc.includes(obj.key)) {
                acc.push(obj.key);
                return acc;
              }
              return acc;
            }, [])
            .filter(key => key !== "unassigned");
          const filters = { filter: { ids } };
          allDerived.push({
            uri: action.filters.across === "assignee" ? "users" : "questionnaires",
            method: "list",
            filters,
            complete,
            id: resultId
          });
        }

        if (action.filters.across === "tag") {
          const resultId = id === "0" ? "bulk" : id;
          let complete = `PAGINATION_quize_${uri}_${resultId}`;
          const ids = listRecords.reduce((acc, obj) => {
            if (!acc.includes(obj.key)) {
              acc.push(obj.key);
              return acc;
            }
            return acc;
          }, []);

          const filters = {
            filter: { tag_ids: ids }
          };
          allDerived.push({
            uri: "tags",
            method: "list",
            filters,
            complete,
            id: resultId
          });
        }
      }

      if (action.uri === "jenkins_pipeline_job_stages") {
        const resultId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_stages_${uri}_${resultId}`;
        const hitFilters = {
          page_size: listRecords.length * 10,
          filter: {
            job_ids: [
              action.filters.filter.job_run_id,
              ...listRecords.filter(record => record.type !== "CICD_STAGE").map(record => record.id)
            ],
            stage_ids: listRecords.filter(record => record.type === "CICD_STAGE").map(record => record.id)
          }
        };
        allDerived.push({
          uri: "triage_rule_results",
          method: "list",
          filters: hitFilters,
          complete: complete,
          id: resultId
        });
      }

      if (action.uri === "cicd_job_aggs") {
        listRecords.map(record => {
          if (record && record.aggs && record.aggs.length) {
            record.aggs.map(({ key, totals }) => {
              record[`dynamic_column_aggs_${key}`] = totals;
              // record["dynamic_column_group_by"] = ["dynamic_column_aggs"];
            });
          }
        });
      }

      let allSprintJiraIssueKeys = [];
      if (
        shouldDerive(SPRINT_JIRA_ISSUE_KEYS) &&
        (listRecords[0].hasOwnProperty("story_points_by_issue") ||
          listRecords[0].hasOwnProperty("story_points_by_workitem"))
      ) {
        const report = get(action, ["payload", "report"], "");
        const sprintJiraIssueId = id === "0" ? "SPRINT_JIRA_ISSUE_ID" : `SPRINT_JIRA_ISSUE_ID-${id}`;
        let complete = `SPRINT_JIRA_ISSUE_COMPLETE_${uri}_${sprintJiraIssueId}`;
        const bulkId = id === "0" ? "bulk" : id;
        let integrationComplete = `PAGINATION_INTEGRATIONS_${uri}_${bulkId}`;
        forEach(listRecords || [], record => {
          const key = report.includes("azure_") ? "story_points_by_workitem" : "story_points_by_issue";
          allSprintJiraIssueKeys = [...allSprintJiraIssueKeys, ...Object.keys(get(record, key, {}))];
        });

        allSprintJiraIssueKeys = uniq(allSprintJiraIssueKeys);

        /** changes as per LFE-3089 : needed to send only keys and integration_ids in the payload */
        let jiraAPIFilter = {
          integration_ids: get(action.filters, ["filter", "integration_ids"], []),
          keys: allSprintJiraIssueKeys
        };

        allDerived.push(
          {
            uri: sprintUri,
            method: "list",
            filters: {
              page: get(action?.filters, ["page"]),
              page_size: get(action?.filters, ["page_size"]),
              filter: jiraAPIFilter
            },
            complete: complete,
            id: sprintJiraIssueId
          },
          {
            uri: "integrations",
            method: "list",
            filters: { filter: { integration_ids: get(filters, ["filter", "integration_ids"], []) } },
            complete: integrationComplete,
            id: bulkId
          }
        );
        yield put(actionTypes.restapiClear(uri, method, id));
      }

      if (shouldDerive("dashboard_id") && listRecords[0].hasOwnProperty("dashboard_id")) {
        const dashboardId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_dashboards_${uri}_${dashboardId}`;
        let objects = listRecords.reduce((acc, obj) => {
          if (!acc.includes(obj.dashboard_id)) {
            acc.push(obj.dashboard_id);
            return acc;
          }
          return acc;
        }, []);
        if (objects.length > 0) {
          allDerived.push({
            uri: "dashboards",
            method: "list",
            filters: { filter: { ids: objects } },
            complete: complete,
            id: dashboardId
          });
        }
      }

      if (shouldDerive("custom_fields") && listRecords[0].hasOwnProperty("custom_fields")) {
        const configId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_jira_integration_config_${uri}_${configId}`;
        const _filters = {
          filter: {
            integration_ids: filters?.filter?.integration_ids || [],
            product_id: filters?.filter?.product_id
          }
        };
        allDerived.push({
          uri: "jira_integration_config",
          method: "list",
          complete,
          id: configId,
          filters: _filters
        });
      }

      if (shouldDerive("custom_case_fields") && listRecords[0].hasOwnProperty("custom_case_fields")) {
        const configId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_jira_integration_config_${uri}_${configId}`;
        const _filters = {
          filter: {
            integration_ids: filters?.filter?.integration_ids || [],
            product_id: filters?.filter?.product_id
          }
        };
        allDerived.push({
          uri: "jira_integration_config",
          method: "list",
          complete,
          id: configId,
          filters: _filters
        });
      }

      if (shouldDerive("created_by") && listRecords[0].hasOwnProperty("created_by")) {
        console.log("deriving created by");
        const dashboardId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_dashboardusers_${uri}_${dashboardId}`;
        const users = listRecords.reduce((acc, obj) => {
          if (!acc.includes(obj.created_by)) {
            acc.push(obj.created_by);
            return acc;
          }
          return acc;
        }, []);
        if (users.length > 0) {
          allDerived.push({
            uri: "users",
            method: "list",
            filters: { filter: { ids: users } },
            complete: complete,
            id: dashboardId
          });
        }
      }

      if (
        (shouldDerive("tags") || shouldDerive("tag_ids")) &&
        (listRecords[0].hasOwnProperty("tags") || listRecords[0].hasOwnProperty("tag_ids"))
      ) {
        const tagId = id === "0" ? "bulk" : id;
        const key = listRecords[0].hasOwnProperty("tags") ? "tags" : "tag_ids";
        let complete = `PAGINATION_tags_${uri}_${tagId}`;
        let tagIds = {};
        listRecords.forEach(record => {
          let tags = record[key] || [];
          tags.forEach(tag => {
            if (typeof tag === "string") {
              tagIds[tag] = true;
            } else {
              const tag_id = Object.keys(tag || {})[0];
              if (tag_id) {
                tagIds[`${tag_id}`] = true;
              }
            }
          });
        });
        allDerived.push({
          uri: "tags",
          method: "list",
          filters: { filter: { tag_ids: Object.keys(tagIds) } },
          complete: complete,
          id: tagId
        });
      }

      if (
        (shouldDerive("product_id") || shouldDerive("product_ids")) &&
        (listRecords[0].hasOwnProperty("product_ids") || listRecords[0].hasOwnProperty("product_id"))
      ) {
        let bulkProductIds = [];
        const bulkId = id === "0" ? "bulk" : id;
        const identifier = listRecords[0].hasOwnProperty("product_ids") ? "product_ids" : "product_id";
        let complete = `PAGINATION_PRODUCTS_${uri}_${bulkId}`;
        listRecords.forEach(record => {
          if (identifier === "product_ids") {
            const ids = record.product_ids.filter(id => !bulkProductIds.includes(id));
            bulkProductIds.push(...ids);
          } else {
            if (!bulkProductIds.includes(record.product_id)) {
              bulkProductIds.push(record.product_id);
            }
          }
        });
        bulkProductIds = bulkProductIds.filter(id => id !== undefined).map(id => parseInt(id));
        allDerived.push({
          uri: "workspace",
          method: "list",
          filters: { filter: { workspace_id: bulkProductIds } },
          complete: complete,
          id: bulkId
        });
      }
      if (shouldDerive("integration_ids") && listRecords[0].hasOwnProperty("integration_ids")) {
        let bulkIntegIds = [];
        const bulkId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_INTEGRATIONS_${uri}_${bulkId}`;
        listRecords.forEach(record => {
          const ids = record.integration_ids.filter(id => !bulkIntegIds.includes(id));
          bulkIntegIds.push(...ids);
        });
        bulkIntegIds = bulkIntegIds.map(item => item.toString());
        allDerived.push({
          uri: "integrations",
          method: "list",
          filters: { filter: { integration_ids: bulkIntegIds } },
          complete: complete,
          id: bulkId
        });
      }

      if (shouldDerive("integration_id") && listRecords[0].hasOwnProperty("integration_id")) {
        let bulkIntegIds = [];
        const bulkId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_INTEGRATIONS_${uri}_${bulkId}`;
        listRecords.forEach(record => {
          if (!bulkIntegIds.includes(record.integration_id)) {
            bulkIntegIds.push(record.integration_id.toString());
          }
        });
        allDerived.push({
          uri: "integrations",
          method: "list",
          filters: { filter: { integration_ids: bulkIntegIds } },
          complete: complete,
          id: bulkId
        });
      }

      if (shouldDerive("runbook_id") && listRecords[0].hasOwnProperty("runbook_id")) {
        let propelIds = [];
        listRecords.forEach(record => {
          if (!propelIds.includes(record.runbook_id)) {
            propelIds.push(record.runbook_id);
          }
        });
        const bulkId = id === "0" ? "bulk" : id;
        const complete = `PAGINATION_PROPEL_${uri}_${bulkId}`;
        allDerived.push({
          uri: "propels",
          method: "list",
          filters: { filter: { runbook_ids: propelIds } },
          complete: complete,
          id: bulkId
        });
      }

      if (shouldDerive("state_id") && listRecords[0].hasOwnProperty("state_id")) {
        const bulkId = id === "0" ? "bulk" : id;
        const complete = `PAGINATION_TICKETS_STATES_${bulkId}`;
        allDerived.push({
          uri: "states",
          method: "list",
          filters: { filter: {} },
          complete: complete,
          id: bulkId
        });
      }

      if (shouldDerive("work_item_id") && listRecords[0].hasOwnProperty("work_item_id")) {
        const bulkId = id === "0" ? "bulk" : `${id}-bulk`;
        let workitemIds = [];
        listRecords.forEach(record => {
          if (!workitemIds.includes(record.work_item_id) && record.work_item_id) {
            workitemIds.push(record.work_item_id);
          }
        });
        const complete = `PAGINATION_TICKETS_WORKITEM_${bulkId}`;
        allDerived.push({
          uri: "workitem",
          method: "list",
          filters: { filter: { ids: workitemIds } },
          complete: complete,
          id: bulkId
        });
      }

      if (
        shouldDerive("owner_id") &&
        (listRecords[0].hasOwnProperty("owner_id") ||
          (listRecords.length > 1 && listRecords[1].hasOwnProperty("owner_id"))) &&
        rbac !== "PUBLIC_DASHBOARD"
      ) {
        const bulkId = id === "0" ? "bulk" : id;
        let userIds = [];
        listRecords.forEach(record => {
          if (!userIds.includes(record.owner_id) && record.owner_id) {
            userIds.push(record.owner_id);
          }
        });
        const complete = `PAGINATION_TICKETS_USERS_${bulkId}`;
        allDerived.push({
          uri: "users",
          method: "list",
          filters: { filter: { ids: userIds } },
          complete: complete,
          id: bulkId
        });
      }

      if (uri === "org_users") {
        let bulkIntegIds = [];
        const bulkId = id === "0" ? "bulk" : id;
        let complete = `PAGINATION_INTEGRATIONS_${uri}_${bulkId}`;
        listRecords.forEach(record => {
          const integrationIds = (record.integration_user_ids || []).map(item => item.integration_id);
          bulkIntegIds = uniq([...bulkIntegIds, ...integrationIds]);
        });
        allDerived.push({
          uri: "integrations",
          method: "list",
          filters: { filter: { integration_ids: bulkIntegIds } },
          complete: complete,
          id: bulkId
        });
      }

      if (uri === "propel_node_templates" || uri === "propel_trigger_templates") {
        let contentTypes = [];
        listRecords.forEach(record => {
          // input
          Object.keys(record.fields || {}).forEach(field => {
            const fieldObj = record.fields[field];
            //console.log(fieldObj);
            const type = getContentType(fieldObj.content_type);
            //console.log(type);
            if (
              fieldObj.content_type &&
              !PRIMITIVE_CONTENT_TYPES.includes(fieldObj.content_type) &&
              !contentTypes.includes(type)
            ) {
              contentTypes.push(type);
            }
          });
          Object.keys(record.input || {}).forEach(field => {
            const fieldObj = record.input[field];
            const type = getContentType(fieldObj.content_type);
            if (
              fieldObj.content_type &&
              !PRIMITIVE_CONTENT_TYPES.includes(fieldObj.content_type) &&
              !contentTypes.includes(type)
            ) {
              contentTypes.push(type);
            }
          });
          // output
          Object.keys(record.output || {}).forEach(field => {
            const fieldObj = record.output[field];
            const type = getContentType(fieldObj.content_type);
            if (
              fieldObj.content_type &&
              !PRIMITIVE_CONTENT_TYPES.includes(fieldObj.content_type) &&
              !contentTypes.includes(type)
            ) {
              contentTypes.push(type);
            }
          });
        });

        if (contentTypes.length > 0) {
          const bulkId = id === "0" ? `bulk_${uri}` : id;
          const complete = `PAGINATION_CONTENT_SCHEMA_${bulkId}_${uri}`;
          allDerived.push({
            uri: "content_schema",
            method: "list",
            filters: { filter: { content_types: contentTypes } },
            complete: complete,
            id: bulkId
          });
        }
      }

      yield all(
        allDerived.map(action =>
          put(actionTypes.genericList(action.uri, action.method, action.filters, action.complete, action.id, false))
        )
      );

      yield all(allDerived.map(action => take(action.complete)));

      const rstate = yield select(restapiState);

      if (
        shouldDerive(SPRINT_JIRA_ISSUE_KEYS) &&
        (listRecords[0].hasOwnProperty("story_points_by_issue") ||
          listRecords[0].hasOwnProperty("story_points_by_workitem"))
      ) {
        const sprintJiraIssueId = id === "0" ? "SPRINT_JIRA_ISSUE_ID" : `SPRINT_JIRA_ISSUE_ID-${id}`;
        const bulkId = id === "0" ? "bulk" : id;
        const jiraIssueRecords = get(rstate, [sprintUri, "list", sprintJiraIssueId, "data", "records"], []);
        const integrationIdsRecords = get(rstate, ["integrations", "list", bulkId, "data", "records"], []);
        const sprintRecords = cloneDeep(listRecords[0]);
        const newListRecords = deriveSprintJiraIssueKeysHelper(
          jiraIssueRecords,
          integrationIdsRecords,
          sprintRecords,
          report.includes("azure_")
        );
        listData["records"] = newListRecords;
        listData["_metadata"].total_count = get(
          rstate,
          [sprintUri, "list", sprintJiraIssueId, "data", "_metadata", "total_count"],
          0
        );
        yield put(actionTypes.restapiClear("integrations", "list", bulkId));
      }

      if (uri === "jira_tickets" && shouldDerive("velocity_config")) {
        const velocityConfigId = get(filters, ["filter", "velocity_config_id"]);
        if (velocityConfigId) {
          const velocityConfigsList = get(
            rstate,
            [VELOCITY_CONFIGS, "list", VELOCITY_CONFIG_LIST_ID, "data", "records"],
            []
          );
          const selectedConfig = velocityConfigsList?.find(config => config.id === velocityConfigId);
          const preVelocityStageResults = get(selectedConfig, ["pre_development_custom_stages"], []);
          const postVelocityStageResults = get(selectedConfig, ["post_development_custom_stages"], []);
          const fixedStageResults = get(selectedConfig, ["fixed_stages"], []);
          const allStagesResult = [...preVelocityStageResults, ...postVelocityStageResults, ...fixedStageResults];
          const selectedStage = get(filters, ["filter", "velocity_stages", 0], "");
          const corVelocityStageResult = allStagesResult.find(
            result => result?.name?.toLowerCase() === selectedStage?.toLowerCase()
          );
          const stagesData = listRecords.map(data => {
            return {
              ...(data ?? {}),
              velocity_stage_result: {
                ...(corVelocityStageResult ?? {})
              }
            };
          });
          listData["records"] = stagesData;
        }
      }

      if (["quiz_aggs_response_time_report", "quiz_aggs_response_time_table_report"].includes(action.uri)) {
        if (["assignee", "questionnaire_template_id"].includes(action.filters.across)) {
          const resultId = id === "0" ? "bulk" : id;
          const api = action.filters.across === "assignee" ? "users" : "questionnaires";
          const records = get(rstate, [api, "list", resultId, "data", "records"], []);
          listRecords.forEach((record, index) => {
            const item = records.find(_item => _item.id === record.key);
            if (item) {
              listRecords[index].id = item.id;
              listRecords[index].key = api === "users" ? item.email : item.name;
            }
          });
        }

        if (action.filters.across === "tag") {
          const resultId = id === "0" ? "bulk" : id;
          const records = get(rstate, ["tags", "list", resultId, "data", "records"], []);
          listRecords.forEach((record, index) => {
            const item = records.find(_item => _item.id === record.key);
            if (item) {
              listRecords[index].id = item.id;
              listRecords[index].key = item.name;
            }
          });
        }
      }

      if (action.uri === "jenkins_pipeline_job_stages") {
        const resultId = id === "0" ? "bulk" : id;
        const triageResultRecords = get(rstate, ["triage_rule_results", "list", resultId, "data", "records"], []);
        const jobOnlyRecords = triageResultRecords.filter(rec => rec.stage_id === undefined);
        listRecords.forEach((record, index) => {
          const hits = triageResultRecords.filter(rec => rec.stage_id === record.id);
          listRecords[index] = {
            ...record,
            hits: hits
          };
        });
        yield put(actionTypes.restapiClear("triage_rule_results", "list", resultId));
      }

      if (
        (shouldDerive("tags") || shouldDerive("tag_ids")) &&
        (listRecords[0].hasOwnProperty("tags") || listRecords[0].hasOwnProperty("tag_ids"))
      ) {
        const tagId = id === "0" ? "bulk" : id;
        const key = listRecords[0].hasOwnProperty("tags") ? "tags" : "tag_ids";
        let tagIds = {};
        let tagsListData = rstate.tags.list[tagId].data.records;
        tagsListData.forEach(tag => {
          tagIds[tag.id] = tag.name;
        });
        for (let i = 0; i < listRecords.length; i++) {
          let tags = listRecords[i][key] || [];
          let newTags = [];

          let newTagObjs = [];
          const validTags = !isNaN(parseInt(tags?.[0]));

          if (validTags) {
            tags.forEach(tag => {
              newTags.push(tagIds[tag]);
              newTagObjs.push({
                key: tag,
                label: tagIds[tag]
              });
            });
            listRecords[i][key] = newTags;
            listRecords[i][`_tags`] = newTagObjs;
          }
        }
        yield put(actionTypes.restapiClear("tags", "list", tagId));
      }

      if (
        (shouldDerive("product_id") || shouldDerive("product_ids")) &&
        (listRecords[0].hasOwnProperty("product_ids") || listRecords[0].hasOwnProperty("product_id"))
      ) {
        const bulkId = id === "0" ? "bulk" : id;
        const identifier = listRecords[0].hasOwnProperty("product_ids") ? "product_ids" : "product_id";
        const workspaces = rstate?.workspace?.list[bulkId]?.data?.records || [];
        listRecords.forEach(record => {
          const productNames =
            identifier === "product_ids"
              ? workspaces
                  .filter(prod => record?.product_ids && record?.product_ids.includes(prod?.id))
                  .map(obj => obj?.name)
              : workspaces
                  .filter(prod => record.product_id && record.product_id.toString() === prod.id.toString())
                  .map(obj => obj.name);
          record.workspaces = productNames;
        });
        yield put(actionTypes.restapiClear("workspace", "list", bulkId));
      }
      if (shouldDerive("integration_ids") && listRecords[0].hasOwnProperty("integration_ids")) {
        const bulkId = id === "0" ? "bulk" : id;
        const integrations = rstate.integrations.list[bulkId].data.records;
        listRecords.forEach(record => {
          const integNames = integrations
            .filter(integ => record.integration_ids.includes(integ.id))
            .map(obj => obj.name);
          record.integrations = integNames;
        });
        yield put(actionTypes.restapiClear("integrations", "list", bulkId));
      }

      if (shouldDerive("integration_id") && listRecords[0].hasOwnProperty("integration_id")) {
        const bulkId = id === "0" ? "bulk" : id;
        const integrations = rstate.integrations.list[bulkId].data.records;
        listRecords.forEach(record => {
          const integs = integrations.filter(integ => record.integration_id === integ.id);
          record.integration_name = integs.length === 0 ? "" : integs[0].name;
          record.integration_url = integs.length === 0 ? "" : integs[0].url;
          record.integration_application = integs?.[0]?.application || "";
        });
        if (action?.payload?.report === LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT) {
          listData["records"] = listRecords; //report check to avoid any side effect's & testing footprints
        }
        yield put(actionTypes.restapiClear("integrations", "list", bulkId));
      }

      if (uri === "org_users") {
        const bulkId = id === "0" ? "bulk" : id;
        const integrations = rstate.integrations.list[bulkId].data.records;
        listRecords.forEach(record => {
          record.integration_user_ids = (record.integration_user_ids || []).map(item => {
            const integs = integrations.filter(integ => item.integration_id === integ.id);
            return {
              ...item,
              name: integs.length === 0 ? "" : integs[0].name,
              url: integs.length === 0 ? "" : integs[0].url,
              application: integs?.[0]?.application || ""
            };
          });
        });
        yield put(actionTypes.restapiClear("integrations", "list", bulkId));
      }

      if (shouldDerive("runbook_id") && listRecords[0].hasOwnProperty("runbook_id")) {
        const bulkId = id === "0" ? "bulk" : id;
        const propels = rstate.propels.list[bulkId].data.records;
        listRecords.forEach(record => {
          const propelName = propels.find(book => book.id === record.runbook_id);
          record.propel_name = propelName ? propelName.name : "";
        });
        yield put(actionTypes.restapiClear("propels", "list", bulkId));
      }

      if (shouldDerive("state_id") && listRecords[0].hasOwnProperty("state_id")) {
        const bulkId = id === "0" ? "bulk" : id;
        const states = getData(rstate, "states", "list", bulkId).records || [];
        listRecords.forEach(record => {
          const state = states.find(state => state.id === record.state_id);
          record.state_name = state.name;
        });
        yield put(actionTypes.restapiClear("states", "list", bulkId));
      }

      if (shouldDerive("work_item_id") && listRecords[0].hasOwnProperty("work_item_id")) {
        const bulkId = id === "0" ? "bulk" : `${id}-bulk`;
        const workItems = getData(rstate, "workitem", "list", bulkId).records || [];
        listRecords.forEach(record => {
          const workItem = workItems.find(workitem => workitem.id === record.work_item_id);
          if (workItem) {
            record.vanity_id = workItem.vanity_id;
            record.assignees = workItem?.assignees?.map(k => k?.user_email) || [];
            record.status = workItem?.status || "";
          }
        });
        yield put(actionTypes.restapiClear("workitem", "list", bulkId));
      }

      if (shouldDerive("custom_fields") && listRecords[0].hasOwnProperty("custom_fields")) {
        const configId = id === "0" ? "bulk" : id;
        const configs = getData(rstate, "jira_integration_config", "list", configId).records || [];
        const cFields = filter(
          reduce(
            configs,
            (acc, obj) => {
              const fields = get(obj, ["config", "agg_custom_fields"], []);
              acc.push(...fields);
              return acc;
            },
            []
          ),
          field => field.key.includes("customfield_") || field.key.includes("Custom.")
        );
        forEach(listRecords, record => {
          const customKeys = Object.keys(record.custom_fields);
          const mappings = filter(cFields, field => customKeys.includes(field.key));
          record.custom_fields_mappings = uniqBy(mappings, "key");
        });
        yield put(actionTypes.restapiClear("jira_integration_config", "list", configId));
      }

      if (shouldDerive("custom_case_fields") && listRecords[0].hasOwnProperty("custom_case_fields")) {
        const configId = id === "0" ? "bulk" : id;
        const configs = getData(rstate, "testrails_fields", "list","testrails_application_field_list", configId).records || [];
        const cFields = configs.map((data)=> {
          return{"name": data.label, "key": data.system_name}
        });
   
        forEach(listRecords, record => {
          const customKeys = Object.keys(record.custom_case_fields);
          const mappings = filter(cFields, field => customKeys.includes(field.key));
          record.custom_fields_mappings = uniqBy(mappings, "key");
        });
        yield put(actionTypes.restapiClear("jira_integration_config", "list", configId));
      }

      if (
        shouldDerive("owner_id") &&
        (listRecords[0].hasOwnProperty("owner_id") ||
          (listRecords.length > 1 && listRecords[1].hasOwnProperty("owner_id")))
      ) {
        const bulkId = id === "0" ? "bulk" : id;
        const users = getData(rstate, "users", "list", bulkId).records || [];
        listRecords.forEach(record => {
          const user = users.find(u => u.id === record.owner_id);
          if (user) {
            record.owner = user.email;
          }
        });
        yield put(actionTypes.restapiClear("users", "list", bulkId));
      }

      if (uri === "propel_node_templates" || uri === "propel_trigger_templates") {
        const bulkId = id === "0" ? `bulk_${uri}` : id;
        const contentSchemas = getData(rstate, "content_schema", "list", bulkId).records || [];
        listRecords.forEach(record => {
          Object.keys(record.fields || {}).forEach(field => {
            const fieldObj = record.fields[field];
            const type = getContentType(fieldObj.content_type);
            const schema = contentSchemas.find(sch => sch.content_type === type);
            fieldObj.content_schema = schema;
          });
          Object.keys(record.input || {}).forEach(field => {
            const fieldObj = record.input[field];
            const type = getContentType(fieldObj.content_type);
            const schema = contentSchemas.find(sch => sch.content_type === type);
            fieldObj.content_schema = schema;
          });
          Object.keys(record.output || {}).forEach(field => {
            const fieldObj = record.output[field];
            const type = getContentType(fieldObj.content_type);
            const schema = contentSchemas.find(sch => sch.content_type === type);
            fieldObj.content_schema = schema;
          });
        });
        yield put(actionTypes.restapiClear("content_schema", "list", bulkId));
      }

      if (shouldDerive("dashboard_id") && listRecords[0].hasOwnProperty("dashboard_id")) {
        const dashboardId = id === "0" ? "bulk" : id;
        const dashboards = get(rstate, ["dashboards", "list", dashboardId, "data", "records"], []);
        listRecords.forEach(record => {
          const dashboard = dashboards.find(d => d.id === record.dashboard_id);
          if (dashboard) {
            record.dashboard_name = dashboard.name;
          }
        });
      }

      if (shouldDerive("created_by") && listRecords[0].hasOwnProperty("created_by")) {
        const dashboardId = id === "0" ? "bulk" : id;
        const users = get(rstate, ["users", "list", dashboardId, "data", "records"], []);
        listRecords.forEach(record => {
          const user = users.find(d => d.id === record.created_by);
          if (user) {
            record.created_name = user.email;
          }
        });
      }
    }

    // console.log(`Resetting loading for ${uri} ${id}`);
    yield put(restapiData(listData, uri, method, id));
    yield put(actionTypes.restapiLoading(false, uri, method, id, null));

    if (action.hasOwnProperty("complete") && action.complete !== null) {
      yield put({ type: action.complete });
    }
  } catch (e) {
    handleError({
      bugsnag: {
        message: e?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.WIDGETS,
        data: { e, action }
      }
    });
  }
}

export function* paginationWatcherSaga() {
  //yield throttle(1000,[PAGINATION_GET],paginationEffectSaga);
  //yield takeLatest([PAGINATION_GET], paginationEffectSaga);
  yield takeEvery([PAGINATION_GET], paginationEffectSaga);
}
