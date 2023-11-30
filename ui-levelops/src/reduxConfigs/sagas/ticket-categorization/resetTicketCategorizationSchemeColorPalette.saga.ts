import { notification } from "antd";
import {
  RestTicketCategorizationCategory,
  RestTicketCategorizationScheme
} from "classes/RestTicketCategorizationScheme";

import { find, map, unset } from "lodash";
import { call, put, select, takeLatest } from "redux-saga/effects";
import { TICKET_CATEGORIZATION_SCHEME_RESET_COLOR_PALETTE } from "reduxConfigs/actions/actionTypes";
import { ticketCategorizationSchemesRestListSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID } from "../../../configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { restapiEffectSaga } from "../restapiSaga";
import { genericRestAPISet } from "../../actions/restapi/genericSet.action";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
const DEFAULT_COLOR = "#FC91AA";
const uri: string = "ticket_categorization_scheme";

function* resetTicketCategorizationSchemeColorPaletteSaga(action: any): any {
  const { id } = action;

  try {
    notification.info({ message: "Resetting color palette..." });
    const schemes = yield select(ticketCategorizationSchemesRestListSelector, {
      id: TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID
    });

    const scheme = find(schemes, (scheme: RestTicketCategorizationScheme) => scheme.id === id);

    if (scheme) {
      scheme.uncategorized_color = DEFAULT_COLOR;
      const categories = scheme.categories;
      categories.sort(
        (a: RestTicketCategorizationCategory, b: RestTicketCategorizationCategory) => (a?.index || 0) - (b?.index || 0)
      );
      scheme.categories = (scheme.categories || []).map((category: RestTicketCategorizationCategory) => {
        const unusedColor = scheme.getTopUnusedColor();
        scheme.updateCategoryColorMapping(category.background_color, unusedColor);
        category.background_color = unusedColor;
        return category.json;
      });

      const _schemes = map(schemes, scheme => scheme.json);
      const profile = scheme?.json;
      unset(profile, ["config", "categoryColorMapping"]);
      yield call(restapiEffectSaga, { uri, method: "update", id, data: profile ?? {} });
      yield put(genericRestAPISet({}, uri, "update", id));
      yield put(genericRestAPISet(_schemes, uri, "list", TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID));
    } else {
      notification.error({ message: "Scheme not found" });
    }
  } catch (e) {
    handleError({
      showNotfication: true,
      message: "Failed to reset color palette",
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.EFFORT_INVESTMENT,
        data: { e, action }
      }
    });
  }
}

export function* resetTicketCategorizationSchemeColorPaletteSagaWatcher() {
  yield takeLatest(TICKET_CATEGORIZATION_SCHEME_RESET_COLOR_PALETTE, resetTicketCategorizationSchemeColorPaletteSaga);
}
