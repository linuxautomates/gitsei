import * as actionTypes from "reduxConfigs/actions/restapi";
import { getData, getError } from "../../utils/loadingUtils";
import { put, select, take, takeEvery } from "redux-saga/effects";
import { get, uniqBy } from "lodash";
import { LEVELOPS_WIDGETS } from "reduxConfigs/actions/actionTypes";
import { restapiData } from "../actions/restapi";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

const restapiState = state => state.restapiReducer;

export function* levelopsWidgetsSaga(action) {
  try {
    const { uri, method, data, id } = action;
    const origId = id;
    const origUri = uri;
    yield put(actionTypes.restapiClear(origUri, method, origId));
    const baseComplete = "LEVELOPS_WIDGETS_COMPLETE";
    const complete = `${baseComplete}_${uri}_${id}`;
    yield put(actionTypes.genericList(uri, method, data, complete, id, false));
    yield take(complete);

    const restState = yield select(restapiState);

    if (getError(restState, uri, method, id)) {
      return;
    }
    const listData = getData(restState, uri, method, id);
    const listRecords = listData.records || [];

    listRecords.forEach((record, index) => {
      listRecords[index] = {
        ...(listRecords[index] || {}),
        id: record.key
      };
    });

    if (listRecords.length) {
      let apiUri = "";
      let filters = {};
      let apiId = "";
      const across = get(data, ["across"], "");
      const ids = listRecords.reduce((acc, obj) => {
        if (!acc.includes(obj.key)) {
          acc.push(obj.key);
          return acc;
        }
        return acc;
      }, []);

      if (across === "questionnaire_template_id") {
        apiUri = "questionnaires";
        filters = { filter: { ids } };
        apiId = "questionnaires_0";
      }

      if (across === "assignee") {
        apiUri = "users";
        filters = { filter: { ids: ids.filter(id => id !== "unassigned") } };
        apiId = "users_0";
      }

      if (across === "tag") {
        apiUri = "tags";
        filters = { filter: { tag_ids: ids } };
        apiId = "tags_0";
      }

      if (across === "state") {
        apiUri = "states";
        filters = { filter: { ids } };
        apiId = "states_0";
      }

      if (across === "product") {
        apiUri = "products";
        filters = { filter: { ids } };
        apiId = "products_0";
      }

      if (apiUri.length) {
        const apiCall = {
          uri: apiUri,
          method: "list",
          filters,
          complete: `${baseComplete}_${apiId}`,
          id: apiId
        };

        yield put(
          actionTypes.genericList(apiCall.uri, apiCall.method, apiCall.filters, apiCall.complete, apiCall.id, false)
        );
        yield take(`${baseComplete}_${apiId}`);

        const rState = yield select(restapiState);

        const records = get(rState, [apiCall.uri, apiCall.method, apiCall.id, "data", "records"], []);

        listRecords.forEach((wData, index) => {
          const record = records.find(r => r.id.toString() === wData.key.toString());
          if (record) {
            let name = record.name;
            if (apiCall.uri === "users") {
              name = `${record.email}`;
            }
            listRecords[index] = {
              ...listRecords[index],
              key: name
            };
          }
        });

        yield put(actionTypes.restapiClear(apiCall.uri, apiCall.method, apiCall.id));
      }

      const stacks = get(data, ["stacks"], []);
      if (stacks.length) {
        let stackUri = "";
        let stackFilters = {};
        let stackId = "";

        const stackIds = listRecords.reduce((acc, obj) => {
          if (obj.stacks && obj.stacks.length) {
            obj.stacks.forEach(stack => {
              if (!acc.includes(stack.key)) {
                acc.push(stack.key);
                return acc;
              }
            });
          }
          return acc;
        }, []);

        if (stacks[0] === "questionnaire_template_id") {
          stackUri = "questionnaires";
          stackFilters = { filter: { ids: stackIds } };
          stackId = "stack_questionnaires_0";
        }

        if (stacks[0] === "assignee") {
          stackUri = "users";
          stackFilters = { filter: { ids: stackIds.filter(id => id !== "unassigned") } };
          stackId = "stack_users_0";
        }

        if (stacks[0] === "tag") {
          stackUri = "tags";
          stackFilters = { filter: { tag_ids: stackIds } };
          stackId = "stack_tags_0";
        }

        if (stacks[0] === "state") {
          stackUri = "states";
          stackFilters = { filter: { ids: stackIds } };
          stackId = "stack_states_0";
        }

        if (stacks[0] === "product") {
          stackUri = "products";
          stackFilters = { filter: { ids: stackIds } };
          stackId = "stack_products_0";
        }

        if (stackUri.length) {
          const stackApiCall = {
            uri: stackUri,
            method: "list",
            filters: stackFilters,
            complete: `${baseComplete}_${stackId}`,
            id: stackId
          };

          yield put(
            actionTypes.genericList(
              stackApiCall.uri,
              stackApiCall.method,
              stackApiCall.filters,
              stackApiCall.complete,
              stackApiCall.id,
              false
            )
          );
          yield take(`${baseComplete}_${stackId}`);

          const sState = yield select(restapiState);

          const records = get(sState, [stackApiCall.uri, stackApiCall.method, stackApiCall.id, "data", "records"], []);

          let newRecords = [];

          listRecords.forEach((wData, index) => {
            if (wData.stacks && wData.stacks.length) {
              let stacks = wData.stacks.reduce((acc, stack) => {
                const record = records.find(r => (r.id || "").toString() === stack.key);
                if (record) {
                  let name = record.name;
                  if (stackApiCall.uri === "users") {
                    name = `${record.email}`;
                  }
                  acc.push({ ...stack, key: name, id: stack.key });
                  return acc;
                }
                return acc;
              }, []);
              newRecords.push({ ...listRecords[index], stacks: stacks });
            } else {
              newRecords.push({ ...listRecords[index] });
            }
          });

          listData.records = uniqBy(newRecords, "id");

          yield put(actionTypes.restapiClear(stackApiCall.uri, stackApiCall.method, stackApiCall.id));
        }
      }
    }

    yield put(restapiData(listData, origUri, method, origId));
    yield put(actionTypes.restapiLoading(false, origUri, method, origId, null));
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

export function* levelOpsWidgetsWatcherSaga() {
  yield takeEvery([LEVELOPS_WIDGETS], levelopsWidgetsSaga);
}
