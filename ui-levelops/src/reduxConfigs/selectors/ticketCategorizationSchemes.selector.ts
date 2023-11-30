import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import {
  EFFORT_INVESTMENT_CATEGORIES_VALUES_NODE,
  TICKET_CATEGORIZATION_SCHEME
} from "configurations/pages/ticket-categorization/constants/ticket-categorization.constants";
import { EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID } from "dashboard/components/dashboard-settings-modal/constant";

import { get, map } from "lodash";
import { createSelector } from "reselect";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";

const getID = createParameterSelector((params: any) => params.id || "0");
const getSchemeID = createParameterSelector((params: any) => params.scheme_id || "0");

const ticketCategorizationSelector = createSelector(restapiState, (data: any) => {
  return get(data, [TICKET_CATEGORIZATION_SCHEME], {});
});

//list
const _ticketCategorizationSchemesListSelector = createSelector(
  ticketCategorizationSelector,
  (ticketCategorization: any) => {
    return get(ticketCategorization, ["list"], {});
  }
);

export const ticketCategorizationSchemesListSelector = createSelector(
  _ticketCategorizationSchemesListSelector,
  getID,
  (listState: any, listId: string) => {
    return get(listState, [listId, "data", "records"], []);
  }
);

export const ticketCategorizationSchemesRestListSelector = createSelector(
  ticketCategorizationSchemesListSelector,
  (records: any[]) => {
    return map(records, record => new RestTicketCategorizationScheme(record));
  }
);

//get
const _ticketCategorizationSchemesGetSelector = createSelector(
  ticketCategorizationSelector,
  (ticketCategorization: any) => {
    return get(ticketCategorization, ["get"], {});
  }
);

export const ticketCategorizationSchemesGetSelector = createSelector(
  _ticketCategorizationSchemesGetSelector,
  getSchemeID,
  (getState: any, id: string) => {
    return get(getState, [id, "data"], {});
  }
);

export const ticketCategorizationSchemesRestGetSelector = createSelector(
  ticketCategorizationSchemesGetSelector,
  (record: any) => {
    return new RestTicketCategorizationScheme(record);
  }
);

//create
export const _ticketCategorizationSchemesCreateSelector = createSelector(
  ticketCategorizationSelector,
  (ticketCategorization: any) => {
    return get(ticketCategorization, ["create", "new", "data"], {});
  }
);

export const ticketCategorizationSchemesRestCreateSelector = createSelector(
  _ticketCategorizationSchemesCreateSelector,
  (record: any) => {
    return new RestTicketCategorizationScheme(record);
  }
);

export const categoriesFiltersValueSelector = createSelector(restapiState, (data: any) =>
  get(data, [EFFORT_INVESTMENT_CATEGORIES_VALUES_NODE], { loading: true, error: false })
);

export const ticketcategorizationListSelector = createSelector(restapiState, (data: any) =>
  get(data, ["ticket_categorization_scheme", "list", EFFORT_INVESTMENT_DEFAULT_SCHEME_UUID], {
    loading: true,
    error: false
  })
);
