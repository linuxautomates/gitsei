import { get } from "lodash";
import { createSelector } from "reselect";
import { createParameterSelector } from "./selector";

/**
 * @deprecated Please use newPageSettings
 */
export const pageSettings = store => {
  return store.pageSettingsReducer.toJS();
};

export const getReportPageSettings = store => {
  const pages = pageSettings(store);
  const reportPage = pages.report_page || {};
  return reportPage;
};

export const newPageSettings = store => {
  return store.pageSettingsReducer;
};

export const getPageSettingsSelector = createSelector(newPageSettings, data => {
  return data && data.toJS();
});

const getLocation = createParameterSelector(params => params.location);
const getButton = createParameterSelector(params => params.buttonType);

export const getGenericPageLocationSelector = createSelector(getPageSettingsSelector, getLocation, (data, location) => {
  return get(data, [location], {});
});

export const getGenericPageButtonTypeSelector = createSelector(
  getGenericPageLocationSelector,
  getButton,
  (data, buttonType) => {
    return get(data, [buttonType], {});
  }
);
