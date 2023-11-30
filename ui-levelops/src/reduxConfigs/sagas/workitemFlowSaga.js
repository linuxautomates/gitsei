import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { get } from "lodash";
import { all, call, put, select, take, takeLatest } from "redux-saga/effects";
import * as formActions from "reduxConfigs/actions/formActions";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { getData, getError } from "utils/loadingUtils";
import { RestWorkItem } from "../../classes/RestWorkItem";
import { DEFAULT_FIELDS } from "../../constants/fieldTypes";
import RestapiService from "../../services/restapiService";
import { WORKITEM_FLOW } from "../actions/actionTypes";
import { restapiClear, restapiData } from "../actions/restapi";

export function* workItemFlowEffectSaga(action) {
  const restState = yield select(restapiState);
  const statesLoading = get(restState, ["states", "list", "workitems", "loading"], true);
  const productsLoading = get(restState, ["products", "list", "workitems", "loading"], true);

  if (statesLoading) {
    yield put(actionTypes.genericList("states", "list", {}, null, "workitems"));
  }
  if (productsLoading) {
    yield put(actionTypes.genericList("products", "list", {}, null, "workitems"));
  }

  const workItemId = `vanity-id/${action.id}`;
  let workItem = undefined;
  const workItemData = get(restState, ["workitem", "get", workItemId, "data"], undefined);
  const workItemListData = get(restState, ["workitem", "get", `list/${workItemId}`, "data"], undefined);
  if (workItemData) {
    workItem = new RestWorkItem(workItemData);
    // update the list records with the new workItem
    let workItemList = get(restState, ["workitem", "list", "0", "data"], { records: [] });
    workItemList.records.forEach((item, index) => {
      if (item.id === workItemData.id) {
        workItemList[index] = workItemData;
      }
    });
  } else {
    console.log("list data");
    workItem = new RestWorkItem(workItemListData);
    yield put(restapiClear("workitem", "get", `list/${workItemId}`));

    let restService = new RestapiService();
    let response;
    try {
      response = yield call(restService.workitem.get, workItemId);
    } catch (e) {
      handleError({
        showNotfication: true,
        message: `Could not find issue ${action.id}`,
        bugsnag: {
          message: e?.message,
          severity: severityTypes.WARNING,
          context: issueContextTypes.WORKITEM_ISSUES,
          data: { e, action }
        }
      });
      return;
    }

    workItem = new RestWorkItem(response.data);
  }

  yield put(formActions.formUpdateObj("workitem_form", workItem));

  let workItemIds = [workItem.id, ...workItem.child_ids];
  if (workItem.parent_id) {
    workItemIds.push(workItem.parent_id);
  }

  let quizActions = [
    {
      uri: "quiz",
      method: "list",
      filter: { filter: { work_item_ids: workItemIds } },
      complete: `COMPLETE_QUIZ_${workItem.id}`,
      id: workItem.id,
      set_loading: false
    },
    {
      uri: "workitem",
      method: "list",
      filter: { filter: { ids: workItemIds } },
      complete: `COMPLETE_QUIZ_WORKITEMS_${workItem.id}`,
      id: `quiz-${workItem.id}`,
      set_loading: false
    }
  ];

  const ticketTemplateId = workItem.ticket_template_id;
  if (ticketTemplateId !== undefined) {
    quizActions.push({
      uri: "ticket_templates",
      id: ticketTemplateId,
      complete: `COMPLETE_TEMPLATES_WORKITEMS_${workItem.id}`,
      set_loading: false
    });
  }

  // get tags here too
  if (workItem.tag_ids && workItem.tag_ids.length > 0) {
    const complete = `WORKITEM_FLOW_TAGS_${workItem.id}`;
    quizActions.push({
      uri: "tags",
      method: "list",
      filter: { filter: { tag_ids: workItem.tag_ids } },
      complete: complete,
      id: workItem.id,
      set_loading: true
    });
  }

  yield all(
    quizActions.map(act =>
      act.method === "list"
        ? put(actionTypes.genericList(act.uri, act.method, act.filter, act.complete, act.id, act.set_loading))
        : put(actionTypes.genericGet(act.uri, act.id, act.complete, act.set_loading))
    )
  );

  //yield put(formActions.formUpdateObj("workitem_form", workItem));
  //yield delay(100);

  yield all(quizActions.map(act => take(act.complete)));
  const quizState = yield select(restapiState);
  let quizList = get(quizState, ["quiz", "list", workItem.id, "data"], { records: [] });
  const workRecords = get(quizState, ["workitem", "list", `quiz-${workItem.id}`, "data", "records"], []);
  (quizList.records || []).forEach(record => {
    const work = workRecords.find(rec => rec.id === record.work_item_id);
    if (work) {
      record.vanity_id = work.vanity_id;
    }
  });

  yield put(restapiData(quizList, "quiz", "list", workItem.id));
  //yield delay(20);
  yield put(actionTypes.restapiLoading(false, "quiz", "list", workItem.id, null));

  if (workItem.parent_id !== undefined) {
    const parentWorkItem = workRecords.find(record => record.id === workItem.parent_id);
    if (parentWorkItem) {
      workItem.parent_vanity_id = parentWorkItem.vanity_id;
    }
  }

  //yield put(formActions.formUpdateObj("workitem_form", workItem));

  if (ticketTemplateId !== undefined) {
    const ticketTemplate = get(quizState, ["ticket_templates", "get", ticketTemplateId, "data"], {});
    workItem.default_fields = ticketTemplate.default_fields || DEFAULT_FIELDS;
    let fields = [];
    let idCalls = {};
    (ticketTemplate.ticket_fields || [])
      .filter(field => field.deleted !== true)
      .forEach(field => {
        const ticketField = workItem.ticket_data_values.find(data => data.ticket_field_id === field.id);
        if (ticketField) {
          fields.push({
            ...field,
            ...ticketField
          });
          if (field.dynamic_resource_name !== undefined) {
            const uri = field.dynamic_resource_name;
            let difference = [];
            if (idCalls[uri] === undefined) {
              idCalls[uri] = [];
              difference = ticketField.values.map(value => value.value).filter(id => id !== undefined);
            } else {
              difference = ticketField.values
                .filter(x => !idCalls[uri].includes(x.value))
                .filter(id => id !== undefined);
            }
            if (difference.length > 0) {
              idCalls[uri].push(...difference);
            }
          }
        } else {
          fields.push({
            ...field,
            values: [],
            ticket_field_id: field.id,
            id: undefined
          });
        }
      });

    workItem.ticket_data_values = fields;

    const numCalls = Object.keys(idCalls).reduce((num, calls) => {
      num = num + idCalls[calls].length;
      return num;
    }, 0);

    if (numCalls > 0) {
      yield all(
        Object.keys(idCalls).reduce((actions, uri) => {
          actions.push(...idCalls[uri].map(id => put(actionTypes.genericGet(uri, id, `COMPLETE_${uri}_${id}`))));
          return actions;
        }, [])
      );

      // now wait for every one of those things to complete
      const allActions = Object.keys(idCalls).reduce((actions, uri) => {
        actions.push(...idCalls[uri].map(id => `COMPLETE_${uri}_${id}`));
        return actions;
      }, []);
      yield all(allActions.map(action => take(action)));
    }

    // now map back all the values
    const newapiState = yield select(restapiState);

    fields.forEach(field => {
      if (field.dynamic_resource_name !== undefined && field.type.includes("dynamic")) {
        field.values = field.values.map(value => {
          if (!getError(newapiState, field.dynamic_resource_name, "get", value.value)) {
            const data = getData(newapiState, field.dynamic_resource_name, "get", value.value);
            const searchField = field.search_field ? field.search_field : "name";
            return { value: { label: data[searchField], key: data.id } };
          } else {
            handleError({
              showNotfication: true,
              message: `Failed to retrieve ${field.dynamic_resource_name} ${value}`,
              bugsnag: {
                message: "Failed to retrieve configurations",
                severity: severityTypes.WARNING,
                context: issueContextTypes.WORKITEM_ISSUES,
                data: { field, value }
              }
            });
            return { value: {} };
          }
        });
      }
    });
    workItem.ticket_data_values = fields;
    if (numCalls > 0) {
      yield all(
        Object.keys(idCalls).reduce((actions, uri) => {
          actions.push(...idCalls[uri].map(id => put(actionTypes.restapiClear(uri, "get", id))));
          return actions;
        }, [])
      );
    }
  }

  if (workItem.child_ids !== undefined && workItem.child_ids.length > 0) {
    yield put(
      actionTypes.workItemList(
        {
          filter: {
            ids: workItem.child_ids
          }
        },
        workItem.id
      )
    );
  }
  // else {
  //   yield put(restapiLoading(false, "workitem", "list", workItemId));
  // }

  workItem.loading = false;
  yield put(formActions.formUpdateObj("workitem_form", workItem));
  yield put(actionTypes.restapiClear("workitem", "list", `quiz-${workItem.id}`));
}

export function* workItemFlowWatcherSaga() {
  yield takeLatest([WORKITEM_FLOW], workItemFlowEffectSaga);
}
