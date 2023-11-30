import { notification } from "antd";
import { issueContextTypes, severityTypes } from "bugsnag";
import { TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { handleError } from "helper/errorReporting.helper";

import { cloneDeep, find, get, map } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import {
  TICKET_CATEGORIZATION_SCHEME_CLONE_LIST,
  TICKET_CATEGORIZATION_SCHEME_CLONE_GET
} from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { ticketCategorizationSchemesListSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { restapiEffectSaga } from "../restapiSaga";

const uri: string = "ticket_categorization_scheme";

function* listCloneTicketCategorizationSchemeSaga(action: any): any {
  const { id } = action;

  try {
    notification.info({ message: "Cloning Scheme..." });
    const schemes = yield select(ticketCategorizationSchemesListSelector, {
      id: TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID
    });

    const schemeToClone = find(schemes, (scheme: { id: string }) => scheme.id === id);

    if (schemeToClone) {
      const newScheme = cloneDeep(schemeToClone);
      newScheme.name = `Copy of ${newScheme.name}`;
      newScheme.default_scheme = false;
      yield call(restapiEffectSaga, { uri, method: "create", data: newScheme });

      let restState = yield select(restapiState);

      const newSchemeId = get(restState, [uri, "create", "0", "data", "id"], undefined);

      if (newSchemeId) {
        yield call(restapiEffectSaga, { uri, method: "get", id: newSchemeId });
        restState = yield select(restapiState);

        const clonedScheme = get(restState, [uri, "get", newSchemeId, "data"], {});
        const _schemes = [...(schemes ?? []), clonedScheme];

        yield put(genericRestAPISet(_schemes, uri, "list", TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID));
        yield put(genericRestAPISet({}, uri, "create", "0"));
        notification.success({ message: "Scheme Cloned successfully" });
      }
    } else {
      notification.error({ message: "Scheme not found" });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to clone scheme",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.EFFORT_INVESTMENT,
        data: { e, action }
      }
    });
  }
}

export function* listCloneTicketCategorizationSchemeSagaWatcher() {
  yield takeLatest(TICKET_CATEGORIZATION_SCHEME_CLONE_LIST, listCloneTicketCategorizationSchemeSaga);
}
