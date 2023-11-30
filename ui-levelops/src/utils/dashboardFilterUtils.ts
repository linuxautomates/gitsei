import { get, isEqual, set, uniqBy } from "lodash";
import { getIsTrendReport } from "dashboard/graph-filters/components/utils/getIsTrendReport";
import { TREND_REPORT_TIME_RANGE_LIMIT } from "dashboard/constants/constants";
import * as AppNames from "dashboard/constants/applications/names";
import { ReportsAlias } from "../dashboard/constants/applications/alias";
import { timeBoundFilterKeys } from "../dashboard/graph-filters/components/DateOptionConstants";
import { allowWidgetDataSorting, widgetFilterOptionsNode } from "../dashboard/helpers/helper";
import {
  widgetDataSortingOptionsDefaultValue,
} from "../dashboard/constants/WidgetDataSortingFilter.constant";
import { WIDGET_DATA_SORT_FILTER_KEY } from "dashboard/constants/filter-name.mapping";
import { PREV_COMPOSITE_REPORT_TRANSFORMER, PREV_REPORT_TRANSFORMER } from "dashboard/constants/applications/names";
import { jiraADOPrevQueryTransformer } from "dashboard/reports/helper";
import { IntegrationTypes } from "constants/IntegrationTypes";
import widgetConstants from "dashboard/constants/widgetConstants";

export const transformFilters = (filters: any, type: any) => {
  const filtersWithoutJiraKeys = Object.keys(filters).filter(k => !k.startsWith("jira_"));

  let typeFilterKeys = [];
  let otherFilterKeys = [];

  for (const key of filtersWithoutJiraKeys) {
    if (key.startsWith(type)) {
      typeFilterKeys.push(key);
    } else {
      otherFilterKeys.push(key);
    }
  }

  // oldFilters = ALL Filters - (Jira Filters + Zendesk/Salesforce Filters)
  const oldFilters = otherFilterKeys.reduce(
    (map, key) => ({
      ...map,
      [key]: filters[key]
    }),
    {}
  );

  // Renaming Zendesk/Salesforce filters
  // by removing zendesk_/salesforce_ prefix
  const typeFilters = typeFilterKeys.reduce(
    (map, key) => ({
      ...map,
      [key.slice(type.length)]: filters[key]
    }),
    {}
  );

  return {
    ...oldFilters,
    ...typeFilters
  };
};

const updateSCMPRSTrendsReport = (responseData: any) => {
  if (responseData && Array.isArray(responseData.widgets)) {
    const scm_prs_report_ids = responseData.widgets.reduce((acc: any, next: any) => {
      if (next.type === "github_prs_report_trends") {
        return [...acc, next.id];
      }
      return acc;
    }, []);
    let need_to_remove_widgets: string[] = [];
    scm_prs_report_ids?.forEach((id: string) => {
      let flag = false;
      responseData.widgets.forEach((widget: any) => {
        if (widget.type === "" && widget?.metadata?.children.includes(id)) {
          flag = true;
        }
        if (widget.type === "" && widget.metadata.children.length === 0) {
          need_to_remove_widgets.push(widget.id);
        }
      });
      if (!flag) {
        need_to_remove_widgets.push(id);
      }
    });
    const widgets = responseData.widgets.filter((widget: any) => !need_to_remove_widgets.includes(widget.id));
    responseData.widgets = widgets;
    return responseData;
  }
  return responseData;
};

export const updateWidgets = (responseData: any, velocityData: any[]) => {
  if (responseData && Array.isArray(responseData.widgets)) {
    const dashboardData = { ...responseData };
    const widgets = dashboardData.widgets.map((widget: any) => {
      const useProfile = get(widgetConstants, [widget?.type, AppNames.GET_VELOCITY_CONFIG], undefined);
      if (useProfile) {
        const velocityConfigId = widget?.query?.velocity_config_id;
        const profile = (velocityData || [])?.find((profiles: any) => profiles?.id === velocityConfigId);
        const widgetMetadata = {
          ...(widget?.metadata || {}),
          commit_created: profile?.starting_event_is_commit_created ?? false
        };
        widget.metadata = widgetMetadata;
      }
      return widget;
    });
    dashboardData.widgets = widgets;
    return dashboardData;
  }
  return responseData;
};

const updateDashboard = (responseData: any, integrationData: any) => {
  const queryIntegration = get(responseData, ["query", "integration_ids"], []);
  let deletedIntegration = false;
  const availableIntegrations = integrationData.map((item: any) => item?.id);
  queryIntegration?.forEach((integration: string) => {
    if (!availableIntegrations.includes(integration)) {
      deletedIntegration = true;
    }
  });
  if (deletedIntegration) {
    const dashboardData = { ...responseData };
    const dashboardQuery = { ...get(responseData, ["query"], {}), integration_ids: availableIntegrations };
    dashboardData.query = dashboardQuery;
    return dashboardData;
  }
  return responseData;
};
const fixCompositeWidgetQuery = (responseData: any) => {
  if (responseData && Array.isArray(responseData.widgets)) {
    let updatedWidgets = [...responseData.widgets];
    (responseData.widgets || []).forEach((widget: any) => {
      // this transformer holds function to transform existing widgets as per latest requirements
      const getPrevCompositeReportTransformer = get(
        widgetConstants,
        [widget?.type, PREV_COMPOSITE_REPORT_TRANSFORMER],
        undefined
      );
      if (getPrevCompositeReportTransformer) {
        updatedWidgets = getPrevCompositeReportTransformer(widget, updatedWidgets || []);
      }
    });
    responseData.widgets = updatedWidgets;
  }
  return responseData;
};
const updateDashboardIntegrationIds = (responseData: any, integration_ids: string[]) => {
  if (responseData && responseData?.query) {
    const prevIntegrations = get(responseData, ["query", "integration_ids"], []).sort((a: any, b: any) => a - b);
    const newIntegration_ids = integration_ids.sort((a: any, b: any) => a - b);
    if (!isEqual(prevIntegrations, newIntegration_ids)) {
      return { ...responseData, query: { ...responseData?.query, integration_ids: integration_ids } };
    }
  }
  return responseData;
};
export const processDashboardApiResponse = (
  response: any,
  customTimeFilterKeys: Array<string>,
  velocityData: any,
  integrationData: any,
  integration_ids: string[]
) => {
  let responseData = { ...response.data };
  responseData = updateDashboardIntegrationIds(responseData, integration_ids);
  responseData = fixCompositeWidgetQuery(responseData);
  responseData = limitFiltersByMaxTime(TREND_REPORT_TIME_RANGE_LIMIT, responseData);
  responseData = fixPagerDutyServiceId(responseData);
  responseData = fixWidgetQuery(responseData);
  responseData = fixCodeAreaValueInWidgetQuery(responseData);
  responseData = reportAlias(responseData);
  responseData = updateExistingTimeFiltersDefaultSortOrder(responseData, customTimeFilterKeys);
  responseData = updateSCMPRSTrendsReport(responseData);
  responseData = updateWidgets(responseData, velocityData);
  responseData = updateDashboard(responseData, integrationData);
  return responseData;
};

const getAllParentsOfCodeArea = (item: string) => {
  let parents = [];
  while (item.includes("\\")) {
    const to = item.lastIndexOf("\\");
    const parent = item.substring(0, to);
    parents.push(parent);
    item = item.substring(0, to);
  }
  return parents;
};

const fixCodeAreaValueInWidgetQuery = (responseData: any) => {
  if (responseData && Array.isArray(responseData.widgets)) {
    (responseData.widgets || []).forEach((widget: any) => {
      const { query } = widget;
      const { workitem_attributes } = query;

      if (
        !!workitem_attributes &&
        workitem_attributes?.hasOwnProperty("code_area") &&
        workitem_attributes?.code_area?.length > 0
      ) {
        let codeAreaValues = workitem_attributes?.code_area
          ?.map((_item: any) => _item?.child)
          .filter((_child: any) => !!_child);
        codeAreaValues = codeAreaValues.map((_val: string) => ({ child: _val }));
        set(widget, ["query", "workitem_attributes", "code_area"], uniqBy(codeAreaValues, "child"));
      }
    });
  }
  return responseData;
};

// Quick fix for now.
const fixWidgetQuery = (responseData: any) => {
  const dashboardMetadata = responseData?.metadata || {};
  if (responseData && Array.isArray(responseData.widgets)) {
    (responseData.widgets || []).forEach((widget: any) => {
      // this transformer holds function to transform existing widgets as per latest requirements
      const getPrevReportTransformer = get(widgetConstants, [widget?.type, PREV_REPORT_TRANSFORMER], undefined);
      if (getPrevReportTransformer) {
        getPrevReportTransformer(widget || {}, dashboardMetadata);
      }
      const application = get(widgetConstants, [widget?.type, "application"]);
      if (application && [IntegrationTypes.JIRA, IntegrationTypes.AZURE].includes(application)) {
        jiraADOPrevQueryTransformer(widget || {});
      }
    });
  }
  return responseData;
};

export const fixPagerDutyServiceId = (responseData: any) => {
  if (responseData && Array.isArray(responseData.widgets)) {
    responseData.widgets.forEach((widget: any) => {
      if (widget.type === AppNames.PAGERDUTY_INCIDENT_REPORT_TRENDS_NAME) {
        if (widget.query.pd_service) {
          if (!widget.query.pd_service_id) {
            widget.query.pd_service_id = widget.query.pd_service;
          }
          delete widget.query.pd_service;
        }
      }
    });
  }

  return responseData;
};

// maxTime is time in seconds.
export const limitFiltersByMaxTime = (maxTime: number, response: any) => {
  const responseData = { ...response };
  const widgets = responseData.widgets;

  // This algorithm finds time range filters greater than
  // 90 days and reduces them to 90 days. Applies only to
  // trend widgets.
  if (Array.isArray(widgets)) {
    for (let widget of widgets) {
      const isTrendReport = getIsTrendReport(widget.type);
      if (isTrendReport && widget.query) {
        for (let filterKey of timeBoundFilterKeys) {
          const filter = widget.query[filterKey];
          if (filter && filterKey !== "end_time") {
            if (filter.$gt && filter.$lt) {
              // Some filters aren't strings
              const stringify = typeof filter.$gt === "string";

              const diff = +filter.$lt - +filter.$gt;
              if (diff > maxTime) {
                filter.$gt = +filter.$lt - maxTime;
                if (stringify) {
                  filter.$gt = filter.$gt.toString();
                }
              }
            } else if (filter.$gt) {
              const stringify = typeof filter.$gt === "string";

              filter.$lt = +filter.$gt + maxTime;
              if (stringify) {
                filter.$lt = filter.$lt.toString();
              }
            } else if (filter.$lt) {
              const stringify = typeof filter.$lt === "string";

              filter.$gt = maxTime - +filter.$lt;
              if (stringify) {
                filter.$gt = filter.$gt.toString();
              }
            }
          }
        }
      }
    }
  }

  return responseData;
};

export const validateTimeRangeFilter = (widgetData: any) => {
  // Make sure time range filters have been
  // filled out completely
  if (widgetData && widgetData.query) {
    for (let filterKey of timeBoundFilterKeys) {
      const filter = widgetData.query[filterKey];
      if (filter) {
        if (filter.$gt && filter.$lt) {
          // Everything good.
        } else if (filter.$gt === undefined && filter.$lt === undefined) {
          // This is ok too.
        } else {
          // There's a problem!
          return false;
        }
      }
    }
  }

  return true;
};

export const transformFiltersZendesk = (filter: any) => {
  const allCustomFields = filter?.custom_fields || {};
  let jiraCustomFields = Object.keys(allCustomFields).reduce((acc: any, key: any) => {
    if (key.includes("jira_")) {
      return {
        ...acc,
        [key]: allCustomFields[key]
      };
    }
  }, {});
  let zendeskCustomFields = Object.keys(allCustomFields).reduce((acc: any, key: any) => {
    if (key.includes("zendesk_")) {
      const newKeys = key.split("_");
      const updatedKey = newKeys[0] + "_" + newKeys[2];
      return {
        ...acc,
        [updatedKey]: allCustomFields[key]
      };
    }
  }, {});
  return { jiraCustomFields, zendeskCustomFields };
};

const reportAlias = (responseData: any) => {
  const widgets = responseData.widgets;

  if (Array.isArray(widgets)) {
    for (let widget of widgets) {
      widget.type = get(ReportsAlias, widget.type, widget.type);
    }
  }
  return responseData;
};

const updateExistingTimeFiltersDefaultSortOrder = (responseData: any, customTimeFilterKeys: Array<string>) => {
  responseData?.widgets?.forEach((widget: any) => {
    if (allowWidgetDataSorting(widget.type, widget.query) && !widget?.query?.sort_xaxis) {
      let across = widget.query?.across;

      if (widget.type.includes("trend")) {
        across = "trend";
      }
      const widgetFilterOption = across && widgetFilterOptionsNode(across, customTimeFilterKeys);

      if (widgetFilterOption) {
        // @ts-ignore
        widget.query[WIDGET_DATA_SORT_FILTER_KEY] = widgetDataSortingOptionsDefaultValue[widgetFilterOption];
      }
    }
  });

  return responseData;
};
