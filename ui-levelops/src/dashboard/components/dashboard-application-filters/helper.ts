import { cloneDeep, get, uniq, uniqBy } from "lodash";
import { toTitleCase } from "../../../utils/stringUtils";
import widgetConstants from "../../constants/widgetConstants";
import { githubJiraFilesSupportedFilters, jiraSupportedFilters } from "../../constants/supported-filters.constant";
import { uiFiltersMapping, supportedFiltersLabelMapping } from "./AddFiltersComponent/filterConstants";
import { getOrderedFilterKeys } from "./AddFiltersComponent/helpers";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const getSupportedFilterURI = (filter: any): string => {
  if (filter) {
    if (Array.isArray(filter)) {
      return filter.map(fil => fil.uri).join("-");
    } else return filter.uri;
  } else return "";
};

const mergeFilter = (prevQuery: any, newQuery: any, filter: string) => {
  return Object.keys(newQuery).includes(filter) ? newQuery[filter] : prevQuery[filter];
};

export const appendAndUpdateFilters = (prevFilters: any, newFilters: any) => {
  if (Object.keys(prevFilters).length === 0) {
    return newFilters;
  } else {
    let updatedFilters = {};
    let excludeFilters: any = {};
    const prevExcludedFilters = get(prevFilters, ["exclude"], {});
    const newExcludedFilters = get(newFilters, ["exclude"], {});
    const prevCustomFields = get(prevFilters, ["custom_fields"], {});
    const newCustomFields = get(newFilters, ["custom_fields"], {});
    const excludeKeys = uniq([...Object.keys(prevExcludedFilters), ...Object.keys(newExcludedFilters)]);
    const filtersKeys = uniq([...Object.keys(prevFilters), ...Object.keys(newFilters)]).filter(
      fil => fil !== "exclude" && !excludeKeys.includes(fil)
    );
    filtersKeys.forEach(filter => {
      updatedFilters = {
        ...updatedFilters,
        [filter]: mergeFilter(prevFilters, newFilters, filter)
      };
    });
    excludeKeys.forEach(filter => {
      excludeFilters = {
        ...excludeFilters,
        [filter]: mergeFilter(prevExcludedFilters, newExcludedFilters, filter)
      };
    });
    if (excludeKeys.includes("custom_fields")) {
      let updatedCustomFilters: any = {};
      const excludeCustomFieldKeys = Object.keys(excludeFilters["custom_fields"]);

      const keys = uniq([...Object.keys(prevCustomFields), ...Object.keys(newCustomFields)]);
      keys.forEach(filter => {
        updatedCustomFilters = {
          ...updatedCustomFilters,
          [filter]: mergeFilter(prevCustomFields, newCustomFields, filter)
        };
      });

      const newCustomFilters = Object.keys(updatedCustomFilters)
        .filter((key: string) => !excludeCustomFieldKeys.includes(key))
        .reduce((acc: any, next: string) => ({ ...acc, [next]: updatedCustomFilters[next] }), {});

      updatedFilters = {
        ...updatedFilters,
        custom_fields: newCustomFilters || {}
      };
    }
    if (Object.keys(excludeFilters).length > 0) {
      return {
        ...updatedFilters,
        exclude: excludeFilters
      };
    }
    return updatedFilters;
  }
};

export const updateLayoutWithNewApplicationFilters = (data: any[], newFilters: any) => {
  const widgets = cloneDeep(data.filter(wid => wid.type !== undefined));
  let updatedWidgets: any[] = [];
  let updatedIds: string[] = [];
  if (widgets.length > 0) {
    widgets.forEach((widget: any) => {
      const sFilter = get(widgetConstants, [widget.type, "supported_filters"], {});
      const sFilterURI = getSupportedFilterURI(sFilter);
      const uriSpecificFilter = get(newFilters, [sFilterURI], undefined);
      const newMetadata = get(newFilters, ["metadata", sFilterURI], undefined);
      if (uriSpecificFilter) {
        let newWidget = widget;
        newWidget.query = appendAndUpdateFilters(newWidget.query, uriSpecificFilter);
        newWidget.metadata = { ...(newWidget.metadata || {}), ...(newMetadata || {}) };
        updatedIds.push(newWidget.id);
        updatedWidgets.push(newWidget);
      } else if (newMetadata) {
        let newWidget = widget;
        newWidget.metadata = { ...(newWidget.metadata || {}), ...(newMetadata || {}) };
        updatedIds.push(newWidget.id);
        updatedWidgets.push(newWidget);
      } else updatedWidgets.push(widget || {});
    });
  }
  return [updatedWidgets, updatedIds];
};

export const checkForCompositeParentIds = (data: any[], updatedIds: any[]) => {
  const widgets = cloneDeep(data);
  let newIds: any[] = [];
  updatedIds.forEach(id => {
    widgets.forEach(widget => {
      const childrens = get(widget, ["children"], []);
      if (childrens.includes(id)) {
        newIds.push(widget.id);
      } else newIds.push(id);
    });
  });
  return newIds;
};

const getFilterName = (application: string, uri: string): string => {
  switch (application) {
    case "jira":
      switch (uri) {
        case "lead_time_filter_values":
          return "Jira Velocity Filters";
        default:
          return toTitleCase(`${application} Filters`);
      }
    case "zendesk":
    case "salesforce":
      return toTitleCase(`${application} Filters`);
    case "sonarqube":
      switch (uri) {
        case "sonarqube_metrics_values":
          return toTitleCase(`${uri} Filters`);
        default:
          return toTitleCase(`${application} Filters`);
      }
    case "jenkins":
      switch (uri) {
        case "jenkins_job_config_filter_values":
          return "Jenkins Job Config Filters";
        case "jenkins_pipelines_jobs_filter_values":
          return "Jenkins Pipelines Jobs Filter";
        default:
          return toTitleCase(`${application} Filters`);
      }
    case "github":
      switch (uri) {
        case "github_prs_filter_values":
          return "SCM PRs Filters";
        case "github_commits_filter_values":
          return "SCM Commits Filters";
        case "scm_issues_filter_values":
          return "SCM Issues Filters";
        case "scm_files_filter_values":
          return "SCM Files Filters";
        default:
          return toTitleCase(`${application} Filters`);
      }
    case "jirazendesk":
      return "Jira/Zendesk Filters";
    case "jenkinsgithub":
      switch (uri) {
        case "jenkins_jobs_filter_values":
          return "Jenkins Jobs Filters";
        case "cicd_filter_values":
          return "Jenkins Filters";
        case "jenkins_job_config_filter_values":
          return "Jenkis Github Job Filters";
        default:
          return toTitleCase(`${application} Filters`);
      }
    case "pagerduty":
      switch (uri) {
        case "pagerduty_filter_values":
          return toTitleCase(`${application} Filters`);
        case "services_report_aggregate_filter_values":
          return toTitleCase(`${application} Aggs Filters`);
        default:
          return toTitleCase(`${application} Filters`);
      }
    default:
      return toTitleCase(`${application} Filters`);
  }
};

export const getApplicationFilters = (): { [key: string]: any } => {
  // eslint-disable-next-line array-callback-return
  const allFilters = Object.keys(widgetConstants).map((report: any) => {
    const sFilter = get(widgetConstants, [report, "supported_filters"], undefined);
    const application = get(widgetConstants, [report, "application"], "");
    if (
      sFilter &&
      !Array.isArray(sFilter) &&
      !["issue_management_workitem_values", "scm_issue_management_workitem_values"].includes(sFilter.uri)
    ) {
      return {
        name: getFilterName(application, sFilter.uri),
        report,
        application,
        uri: sFilter.uri,
        filter: sFilter
      };
    }
  });
  let _filters = uniqBy(
    allFilters.filter((fil: any) => fil !== undefined),
    "uri"
  ).reduce(
    (acc: { [key: string]: any }, next: any) => ({
      ...acc,
      [next.uri]: next
    }),
    {}
  );
  _filters = {
    ..._filters,
    "scm_files_filter_values-jira_filter_values": {
      name: "SCM/Jira Files Filters",
      application: IntegrationTypes.GITHUBJIRA,
      report: "scm_jira_files_report",
      uri: "scm_files_filter_values-jira_filter_values",
      filter: githubJiraFilesSupportedFilters
    }
  };
  return _filters;
};

export const getAllreports = (): { [key: string]: any } => {
  // eslint-disable-next-line array-callback-return
  const allFilters = Object.keys(widgetConstants).map((report: any) => {
    const sFilter = get(widgetConstants, [report, "supported_filters"], undefined);
    const application = get(widgetConstants, [report, "application"], "");
    if (sFilter && !Array.isArray(sFilter)) {
      return {
        name: getFilterName(application, sFilter.uri),
        report,
        application,
        uri: sFilter.uri,
        filter: sFilter
      };
    }
  });
  return allFilters.filter((obj: any) => !!obj);
};
export const getJiraFilterReport = () => {
  return {
    name: "Jira Filters",
    application: IntegrationTypes.JIRA,
    report: "bounce_report",
    uri: jiraSupportedFilters.uri,
    filter: jiraSupportedFilters
  };
};

export const sanitizeMetaDataFilters = (
  filters: { [key: string]: any },
  supportedFilters: { values: string[]; uri: string }
) => {
  const modifiedFilterKeys = supportedFiltersLabelMapping[supportedFilters.uri];
  supportedFilters.values.forEach((filterKey: string) => {
    if (Object.keys(filters || {}).includes(filterKey)) {
      if (Object.keys(modifiedFilterKeys || {}).includes(filterKey)) {
        delete filters[filterKey];
      }
    }

    if (Object.keys(filters.exclude || {}).includes(filterKey)) {
      // check for mapping
      if (Object.keys(modifiedFilterKeys || {}).includes(filterKey)) {
        delete filters.exclude[filterKey];
      }
    }

    if (Object.keys(filters.partial_match || {}).includes(filterKey)) {
      if (Object.keys(modifiedFilterKeys || {}).includes(filterKey)) {
        filters.partial_match[modifiedFilterKeys[filterKey]["key"]] = filters.partial_match[filterKey];
        delete filters.partial_match[filterKey];
      }
    }
  });

  return filters;
};

export const sanitizeGlobalMetaDataFilters = (filters: { [key: string]: any }) => {
  const updatedFilters = Object.keys(filters).reduce((acc: any, uri: string) => {
    const supportedFilter: { values: string[]; uri: string } = get(getApplicationFilters(), [uri, "filter"], {
      values: [],
      uri: ""
    });

    if (Array.isArray(supportedFilter)) {
      const _supportedFilter = {
        values: supportedFilter.reduce((acc: any, filter: any) => [...acc, ...(filter.values || [])], []),
        uri: uri
      };
      return {
        ...acc,
        [uri]: sanitizeMetaDataFilters(filters[uri], _supportedFilter)
      };
    }

    if (!Object.keys(getApplicationFilters()).includes(supportedFilter.uri)) {
      return acc;
    }

    if (!Object.keys(getApplicationFilters()).includes(supportedFilter.uri)) {
      return acc;
    }

    return {
      ...acc,
      [uri]: sanitizeMetaDataFilters(filters[uri], supportedFilter)
    };
  }, {});
  return {
    ...updatedFilters,
    metadata: filters.metadata || {}
  };
};

const reportFilterExcludeKeys = ["partial_match"];

export const sanitizeReportGlobalFilters = (filters: { [key: string]: any }) => {
  const allKeys = Object.keys(filters?.alltypes || {});

  allKeys.forEach(key => {
    if (reportFilterExcludeKeys.includes(key)) {
      delete filters.alltypes[key];
    }
  });

  return filters;
};
