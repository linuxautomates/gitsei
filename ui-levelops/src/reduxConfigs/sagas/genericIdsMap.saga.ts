import * as actionTypes from "reduxConfigs/actions/restapi";
import { put, select, take, takeEvery, all } from "redux-saga/effects";
import { get } from "lodash";
import { GENERIC_IDS_MAP } from "reduxConfigs/actions/actionTypes";
import * as formActions from "reduxConfigs/actions/formActions";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

const restapiState = (state: any) => state.restapiReducer;

export type LabelIdMap = { id: string; name: string; email?: string };

export type IdsMapFormType = {
  questionnaire_template_ids?: LabelIdMap[];
  user_ids?: LabelIdMap[];
  tag_ids?: LabelIdMap[];
  product_ids?: LabelIdMap[];
  state_ids?: LabelIdMap[];
  effort_investment_profile_ids?: LabelIdMap[];
  lead_time_profile_ids?: LabelIdMap[];
};

export const idsMapFormKeys = [
  "questionnaire_template_ids",
  "user_ids",
  "tag_ids",
  "product_ids",
  "state_ids",
  "effort_investment_profile_ids",
  "lead_time_profile_ids"
];

export function* genericIdsMapSaga(action: any): any {
  try {
    const { filters, formName } = action;
    yield put(formActions.formInitialize(formName, {}));
    const baseComplete = `GENERIC_IDS_MAP`;
    const baseId = `GENERIC`;
    let apiCallsAray = [];
    let formData: IdsMapFormType = {};

    const questionnaire_template_ids = get(filters, ["questionnaire_template_ids"], []);
    const user_ids = get(filters, ["user_ids"], []);
    const tag_ids = get(filters, ["tag_ids"], []);
    const product_ids = get(filters, ["product_ids"], []);
    const state_ids = get(filters, ["state_ids"], []);
    const effort_investment_profile_ids = get(filters, ["effort_investment_profile_ids"], []);
    const lead_time_profile_ids = get(filters, ["lead_time_profile_ids"], []);

    if (questionnaire_template_ids.length) {
      apiCallsAray.push({
        uri: "questionnaires",
        filters: { filter: { ids: questionnaire_template_ids } },
        id: `${baseId}_questionnaires_0`,
        complete: `${baseComplete}_questionnaires_0`
      });
    }

    if (user_ids.length) {
      apiCallsAray.push({
        uri: "users",
        filters: { filter: { ids: user_ids.filter((id: any) => id !== "unassigned") } },
        id: `${baseId}_users_0`,
        complete: `${baseComplete}_users_0`
      });
    }

    if (tag_ids.length) {
      apiCallsAray.push({
        uri: "tags",
        filters: { filter: { tag_ids } },
        id: `${baseId}_tags_0`,
        complete: `${baseComplete}_tags_0`
      });
    }

    if (product_ids.length) {
      apiCallsAray.push({
        uri: "products",
        filters: { filter: { ids: product_ids } },
        id: `${baseId}_products_0`,
        complete: `${baseComplete}_products_0`
      });
    }

    if (state_ids.length) {
      apiCallsAray.push({
        uri: "states",
        filters: { filter: { ids: state_ids } },
        id: `${baseId}__states_0`,
        complete: `${baseComplete}_states_0`
      });
    }

    if (effort_investment_profile_ids.length) {
      apiCallsAray.push({
        uri: "ticket_categorization_scheme",
        filters: { filter: { ids: effort_investment_profile_ids } },
        id: `${baseId}_effort_investment_profile_0`,
        complete: `${baseComplete}_effort_investment_profile_0`
      });
    }

    if (lead_time_profile_ids.length) {
      apiCallsAray.push({
        uri: "velocity_configs",
        filters: { filter: { ids: lead_time_profile_ids } },
        id: `${baseId}_lead_time_profile_0`,
        complete: `${baseComplete}_lead_time_profile_0`
      });
    }

    yield all(
      apiCallsAray.map(action =>
        // @ts-ignore
        put(actionTypes.genericList(action.uri, "list", action.filters, action.complete, action.id, false))
      )
    );

    yield all(apiCallsAray.map(action => take(action.complete)));

    const rstate = yield select(restapiState);

    apiCallsAray.forEach(action => {
      const data = get(rstate, [action.uri, "list", action.id, "data", "records"]);
      if (action.uri === "questionnaires") {
        let questionnaire_data: LabelIdMap[] = [];
        questionnaire_template_ids.forEach((id: any) => {
          const record = data.find((record: any) => record.id === id);
          if (record) {
            questionnaire_data.push({ id, name: record.name });
          }
        });
        formData = {
          ...formData,
          questionnaire_template_ids: questionnaire_data
        };
      }

      if (action.uri === "users") {
        let users_data: LabelIdMap[] = [];
        user_ids.forEach((id: any) => {
          const record = data.find((record: any) => record.id === id);
          if (record) {
            users_data.push({ id, name: `${record.first_name} ${record.last_name}`, email: record.email });
          }
        });
        formData = {
          ...formData,
          user_ids: users_data
        };
      }

      if (action.uri === "tags") {
        let tags_data: LabelIdMap[] = [];
        tag_ids.forEach((id: any) => {
          const record = data.find((record: any) => record.id === id);
          if (record) {
            tags_data.push({ id, name: record.name });
          }
        });
        formData = {
          ...formData,
          tag_ids: tags_data
        };
      }

      if (action.uri === "products") {
        let products_data: LabelIdMap[] = [];
        product_ids.forEach((id: any) => {
          const record = data.find((record: any) => record.id === id);
          if (record) {
            products_data.push({ id, name: record.name });
          }
        });
        formData = {
          ...formData,
          product_ids: products_data
        };
      }

      if (action.uri === "states") {
        let states_data: LabelIdMap[] = [];
        state_ids.forEach((id: any) => {
          const record = data.find((record: any) => record.id === id);
          if (record) {
            states_data.push({ id, name: record.name });
          }
        });
        formData = {
          ...formData,
          state_ids: states_data
        };
      }

      if (action.uri === "ticket_categorization_scheme") {
        let ticket_categorization_scheme_data: LabelIdMap[] = [];
        effort_investment_profile_ids.forEach((id: any) => {
          const record = data.find((record: any) => record.id === id);
          if (record) {
            ticket_categorization_scheme_data.push({ id, name: record.name });
          }
        });
        formData = {
          ...formData,
          effort_investment_profile_ids: ticket_categorization_scheme_data
        };
      }

      if (action.uri === "velocity_configs") {
        let velocity_configs_data: LabelIdMap[] = [];
        lead_time_profile_ids.forEach((id: any) => {
          const record = data.find((record: any) => record.id === id);
          if (record) {
            velocity_configs_data.push({ id, name: record.name });
          }
        });
        formData = {
          ...formData,
          lead_time_profile_ids: velocity_configs_data
        };
      }
    });

    yield all(apiCallsAray.map(action => put(actionTypes.restapiClear(action.uri, "list", action.id))));
    yield put(formActions.formUpdateObj(formName, formData));
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.APIS,
        data: { e, action }
      }
    });
  }
}

export function* genericIdsMapWatcherSaga() {
  yield takeEvery([GENERIC_IDS_MAP], genericIdsMapSaga);
}
