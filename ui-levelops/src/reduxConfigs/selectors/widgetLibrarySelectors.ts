import { createSelector } from "reselect";
import { get } from "lodash";
import { selectedDashboardIntegrations } from "./integrationSelector";
import CompactReport from "../../model/report/CompactReport";
import Report from "../../model/report/Report";
import { getIntegrationApplications, isSupportedByIntegration } from "../../utils/reportListUtils";

const LIBRARY_REPORTS = "widgetLibraryReducer";
const LIBRARY_REPORTS_LIST = "list";
const LIBRARY_REPORTS_FILTERED_LIST = "filtered_list";
const LIBRARY_REPORTS_CATEGORY_LIST = "categories_list";
const LIBRARY_REPORTS_FILTERS = "filters";
const SELECTED_APPLICATIONS = "applications";
const SELECTED_CATEGORIES = "categories";
const SEARCH_QUERY = "search_query";

export const libraryReportsSelector = (state: any) => {
  return get(state, [LIBRARY_REPORTS], {});
};

export const libraryReportListSelector = createSelector(
  libraryReportsSelector,
  selectedDashboardIntegrations,
  (data: any, integrations: []) => {
    const applications = getIntegrationApplications(integrations);
    const reports = get(data, [LIBRARY_REPORTS_FILTERED_LIST], []);
    reports.map((report: CompactReport) => {
      report.supported_by_integration = isSupportedByIntegration(report as Report, !!integrations.length, applications);
    });
    return reports;
  }
);

export const libraryReportListByCategorySelector = createSelector(
  libraryReportsSelector,
  selectedDashboardIntegrations,
  (data: any, integrations: []) => {
    const applications = getIntegrationApplications(integrations);
    const categories = get(data, [LIBRARY_REPORTS_CATEGORY_LIST], []);
    categories.map((category: any) => {
      const reports: any = Object.values(category)?.[0] || [];
      reports.map((report: CompactReport) => {
        report.supported_by_integration = isSupportedByIntegration(
          report as Report,
          !!integrations.length,
          applications
        );
      });
    });
    return categories;
  }
);

export const libraryReportFiltersSelector = createSelector(libraryReportsSelector, (data: any) => {
  return get(data, [LIBRARY_REPORTS_FILTERS], {});
});

export const showSupportedOnlyReportsSelector = createSelector(libraryReportFiltersSelector, (filters: any) => {
  return get(filters, ["supported_only"], false);
});

export const libraryReportSelectedApplicationsSelector = createSelector(libraryReportFiltersSelector, (data: any) => {
  return get(data, [SELECTED_APPLICATIONS], []);
});

export const libraryReportSelectedCategoriesSelector = createSelector(libraryReportFiltersSelector, (data: any) => {
  return get(data, [SELECTED_CATEGORIES], []);
});

export const libraryReportSearchQuerySelector = createSelector(libraryReportsSelector, (data: any) => {
  return get(data, [SEARCH_QUERY], "");
});
