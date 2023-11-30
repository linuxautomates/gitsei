import { createSelector } from "reselect";
import { get } from "lodash";
import { restapiState } from "./restapiSelector";
import { createParameterSelector } from "./selector";
import { ALL_REPORTS } from "../../dashboard/pages/explore-widget/report.constant";

const REPORT_DOCS = "report_docs";

const getID = createParameterSelector((params: any) => params.report_id);

export const reportDocsSelector = createSelector(restapiState, (data: any) => {
  return get(data, [REPORT_DOCS], {});
});

export const reportDocsListSelector = createSelector(reportDocsSelector, (data: any) => {
  return get(data, ["list", "0"], {});
});

export const reportDocsListDataSelector = createSelector(reportDocsListSelector, (data: any) => {
  return get(data, ["data", "records"], []);
});

export const reportDocsGetSelector = createSelector(reportDocsSelector, getID, (reports: any, id: string) => {
  return get(reports, ["get", id], { loading: true, error: false, isDocLoaded: false });
});

export const allReportsDocsListSelector = createSelector(reportDocsSelector, (data: any) => {
  return get(data, ["list", ALL_REPORTS], {});
});
