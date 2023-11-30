import { notification } from "antd";
import { TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";

import { filter, map } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { TICKET_CATEGORIZATION_SCHEME_DELETE } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { ticketCategorizationSchemesRestListSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { restapiEffectSaga } from "../restapiSaga";

const uri: string = "ticket_categorization_scheme";

function* deleteTicketCategorizationSchemeSaga(action: any): any {
  const { id } = action;

  try {
    const schemes = yield select(ticketCategorizationSchemesRestListSelector, {
      id: TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID
    });

    yield call(restapiEffectSaga, { uri, method: "delete", id });
    const _schemes = filter(
      map(schemes, scheme => scheme.json),
      scheme => scheme.id !== id
    );

    yield put(genericRestAPISet(_schemes, uri, "list", TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID));
    notification.success({ message: "Scheme Deleted successfully" });
  } catch (e) {
    console.error("Failed to delete scheme", e);
  }
}

export function* deleteTicketCategorizationSchemeSagaWatcher() {
  yield takeLatest(TICKET_CATEGORIZATION_SCHEME_DELETE, deleteTicketCategorizationSchemeSaga);
}
