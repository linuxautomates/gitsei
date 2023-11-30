import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { RestTicketCategorizationProfileJSONType } from "configurations/pages/ticket-categorization/types/ticketCategorization.types";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import * as actions from "../actionTypes";
import { TICKET_CATEGORIZATION_SCHEME_RESET_COLOR_PALETTE } from "../actionTypes";

const uri: string = "ticket_categorization_scheme";

export const ticketCategorizationSchemesList = (filters: any, id: string = "0", complete = null) => ({
  type: actions.RESTAPI_READ,
  data: filters,
  complete,
  uri,
  id,
  method: "list"
});

export const ticketCategorizationSchemesDelete = (id: string) => ({
  type: actions.TICKET_CATEGORIZATION_SCHEME_DELETE,
  id
});

export const ticketCategorizationSchemeGet = (id: string) => ({
  type: actions.RESTAPI_READ,
  uri,
  id,
  method: "get"
});

export const ticketCategorizationSchemeCreate = (scheme: basicMappingType<any>) => ({
  type: actions.RESTAPI_READ,
  uri,
  data: scheme,
  method: "create"
});

export const ticketCategorizationSchemeUpdate = (scheme: basicMappingType<any>) => ({
  type: actions.RESTAPI_READ,
  uri,
  data: scheme,
  id: scheme.id,
  method: "update"
});

export const ticketCategorizationSchemeCreateUpdate = (
  scheme: RestTicketCategorizationProfileJSONType,
  method: "create" | "update"
) => ({
  type: actions.TICKET_CATEGORIZATION_CREATE_UPDATE,
  uri,
  data: scheme,
  method
});

export const ticketCategorizationSchemeClone = (cloneId: string, method: "list" | "get") => ({
  type:
    method === "list"
      ? actions.TICKET_CATEGORIZATION_SCHEME_CLONE_LIST
      : actions.TICKET_CATEGORIZATION_SCHEME_CLONE_GET,
  uri,
  id: cloneId
});

export const ticketCategorizationSchemeSetToDefault = (id: string) => ({
  type: actions.TICKET_CATEGORIZATION_SCHEME_SET_TO_DEFAULT,
  uri,
  id
});

export const ticketCategorizationSchemeResetColorPalette = (id: string) => ({
  type: actions.TICKET_CATEGORIZATION_SCHEME_RESET_COLOR_PALETTE,
  uri,
  id
});

// for calling CategoryFiltersValues.saga
export const categoriesFiltersValues = (integrationIds = []) => ({
  type: actions.CATEGORIES_FILTERS_VALUES,
  integrationIds
});

export const azureCategoriesFiltersValues = () => ({
  type: actions.AZURE_CATEGORIES_FILTER_VALUES
});

// for calling categoriesFiltersValuesTrellisProfiles.saga
export const categoriesFiltersTrellisProfileValues = (integrationIds = []) => ({
  type: actions.CATEGORIES_FILTERS_VALUES_WITH_STATUS,
  integrationIds
});

export const azureCategoriesFiltersTrellisProfileValues = () => ({
  type: actions.AZURE_CATEGORIES_FILTER_VALUES_WITH_STATUS
});

// for setting categories filters values in the store
export const setCategoriesFiltersValuesData = (payload: { loading?: boolean; error?: boolean; payload?: any }) => ({
  type: actions.SET_CATEGORIES_VALUES_DATA,
  payload
});
