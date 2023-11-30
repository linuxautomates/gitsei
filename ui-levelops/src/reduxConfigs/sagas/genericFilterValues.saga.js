import { all, put, select, take, takeEvery } from "redux-saga/effects";
import { GENERIC_FILTER_VALUES } from "../actions/actionTypes";
import * as actionTypes from "reduxConfigs/actions/restapi";
import { getData, getError } from "../../utils/loadingUtils";
import { restapiData } from "../actions/restapi";

const restapiState = state => state.restapiReducer;

export function* genericFiltersEffectSaga(action) {
  const actionId = action.id;
  const supportedFilters = action.supportedFilters;
  const integrationIds = action.integrationIds;
  const apiState = yield select(restapiState);

  if (Array.isArray(supportedFilters)) {
    let calls = supportedFilters.map(filter => {
      return {
        uri: filter.uri,
        filter: {
          fields: filter.values,
          integration_ids: integrationIds,
          type: filter.type || undefined,
          filter: {
            integration_ids: integrationIds
          }
        }
      };
    });

    calls.forEach(item => {
      Object.keys(item).forEach(key => {
        if (item[key] === undefined) {
          delete item[key];
        }
      });
    });

    yield all(
      calls.map(call => put(actionTypes.genericList(call.uri, "list", call.filter, `COMPLETE_${call.uri}`, actionId)))
    );

    yield all(calls.map(call => take(`COMPLETE_${call.uri}`)));

    calls.forEach(call => {
      if (getError(apiState, call.uri, "list", actionId)) {
      }
    });

    calls = calls.map(call => {
      return {
        ...call,
        data: getData(apiState, call.uri, "list", actionId)
      };
    });

    yield all(calls.map(call => put(restapiData(call.data, call.uri, "list", actionId))));
  } else {
    let filters = {
      fields: supportedFilters.values,
      integration_ids: integrationIds,
      type: supportedFilters.type || undefined,
      filter: {
        integration_ids: integrationIds,
        ...(action.additionalFilters || {})
      }
    };

    if (action.removeIntegration) {
      filters = {
        fields: supportedFilters.values,
        integration_ids: integrationIds,
        type: supportedFilters.type || undefined,
        filter: {
          ...(action.additionalFilters || {})
        }
      };
    }

    Object.keys(filters).forEach(key => {
      if (filters[key] === undefined) {
        delete filters[key];
      }
    });

    yield put(
      actionTypes.genericList(supportedFilters.uri, "list", filters, `COMPLETE_${supportedFilters.uri}`, actionId)
    );

    yield take(`COMPLETE_${supportedFilters.uri}`);

    if (getError(apiState, supportedFilters.uri, "list", actionId)) {
      return;
    }

    const data = getData(apiState, supportedFilters.uri, "list", actionId);

    yield put(restapiData(data, supportedFilters.uri, "list", actionId));
  }
}

export function* genericFiltersWatcherSaga() {
  yield takeEvery([GENERIC_FILTER_VALUES], genericFiltersEffectSaga);
}
