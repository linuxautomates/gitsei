import { notification } from "antd";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID } from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { find, map } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { TICKET_CATEGORIZATION_SCHEME_SET_TO_DEFAULT } from "reduxConfigs/actions/actionTypes";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { ticketCategorizationSchemesRestListSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { restapiEffectSaga } from "../restapiSaga";

const uri: string = "ticket_categorization_scheme";

function* setDefaultTicketCategorizationSchemeSaga(action: any): any {
  const { id } = action;

  try {
    const schemes = yield select(ticketCategorizationSchemesRestListSelector, {
      id: TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID
    });

    let schemeToSetDefault: RestTicketCategorizationScheme = find(
      schemes,
      (scheme: RestTicketCategorizationScheme) => scheme.id === id
    );
    let schemeToRemoveDefault: RestTicketCategorizationScheme = find(
      schemes,
      (scheme: RestTicketCategorizationScheme) => scheme.defaultScheme
    );

    schemeToSetDefault.defaultScheme = true;

    if (schemeToRemoveDefault) {
      schemeToRemoveDefault.defaultScheme = false;
    }

    const _schemes = map(schemes, scheme => scheme.json);

    yield call(restapiEffectSaga, { uri, method: "update", id, data: schemeToSetDefault?.json ?? {} });
    yield put(genericRestAPISet({}, uri, "update", id));
    yield put(genericRestAPISet(_schemes, uri, "list", TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID));
    notification.success({ message: "Scheme set to default successfully" });
  } catch (e) {
    console.error("Failed to set default scheme", e);
  }
}

export function* setDefaultTicketCategorizationSchemeSagaWatcher() {
  yield takeLatest(TICKET_CATEGORIZATION_SCHEME_SET_TO_DEFAULT, setDefaultTicketCategorizationSchemeSaga);
}
