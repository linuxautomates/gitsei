import { get } from "lodash";
import { createSelector } from "reselect";

import { restapiState } from "./restapiSelector";

const URI = "ctemplates";

export const getTemplateGetSelector = createSelector(restapiState, (data: any) => {
  return get(data, [URI, "get"], {});
});

export const getTemplateListSelector = createSelector(restapiState, (data: any) => {
  return get(data, [URI, "list"], {});
});

export const getTemplateCreateSelector = createSelector(restapiState, (data: any) => {
  return get(data, [URI, "create"], {});
});

export const getTemplateUpdateSelector = createSelector(restapiState, (data: any) => {
  return get(data, [URI, "update"], {});
});

export const getTemplateDeleteSelector = createSelector(restapiState, (data: any) => {
  return get(data, [URI, "delete"], {});
});

export const getTemplateBulkDeleteSelector = createSelector(restapiState, (data: any) => {
  return get(data, [URI, "bulkDelete"], {});
});
