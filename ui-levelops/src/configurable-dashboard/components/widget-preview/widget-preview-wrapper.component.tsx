import React, { useCallback, useMemo } from "react";
import { forEach, get } from "lodash";

import "./widget-preview.component.scss";
import { valuesToFilters } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import WidgetPreviewComponent from "./widget-preview.component";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { dashboardWidgetChildrenSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { validateTimeRangeFilter } from "utils/dashboardFilterUtils";
import { WidgetType } from "dashboard/helpers/helper";
import { validateConfigTableWidget } from "../../helpers/helper";
import {
  sprintStatReports,
  NotTimePeriodSupportedReports
} from "dashboard/graph-filters/components/sprintFilters.constant";
import { jiraBAStatReports } from "dashboard/constants/bussiness-alignment-applications/constants";
import {
  ALL_VELOCITY_PROFILE_REPORTS,
  ISSUE_MANAGEMENT_REPORTS,
  JENKINS_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT
} from "dashboard/constants/applications/names";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { DisabledPreviewWidgets } from "./constants";
import useVelocityConfigProfiles from "custom-hooks/useVelocityConfigProfiles";
import { widgetUpdate } from "reduxConfigs/actions/restapi";
import { useDispatch, useSelector } from "react-redux";

interface WidgetPreviewWrapperComponentProps {
  widgetId: string;
  dashboardId: string;
  configurePreview?: boolean;
  previewOnly?: boolean;
}

const WidgetPreviewWrapperComponent: React.FC<WidgetPreviewWrapperComponentProps> = ({
  dashboardId,
  widgetId: _widgetId,
  configurePreview,
  previewOnly
}) => {
  const widgetId = _widgetId || "";
  const dashboard: RestDashboard | null = useSelector(selectedDashboard);
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const widgetChildren: RestWidget[] = useParamSelector(dashboardWidgetChildrenSelector, {
    widget_id: widgetId,
    dashboard_id: dashboardId
  });

  const integrationIds = get(dashboard, ["query", "integration_ids"], []);

  const integrationKey = useMemo(
    () => (integrationIds.length ? integrationIds.sort().join("_") : "0"),
    [integrationIds]
  );

  const globalFilters = useMemo(() => {
    return { integration_ids: integrationIds };
  }, [integrationIds]);

  const dispatch = useDispatch();
  let query = (widget || {}).query || {};

  const { defaultProfile } = useVelocityConfigProfiles(widget?.type);

  const getWidgetConstant = useCallback(
    (data: any, selectedReport) => get(widgetConstants, [selectedReport, data], undefined),
    []
  );

  const application = getWidgetConstant("application", widget?.type);

  if (!widget) {
    return null;
  }

  const { type: selectedReport, widget_type: graphType, metadata, max_records, weights, custom_hygienes } = widget;

  const tableWidgetGraphType = get(widget, ["metadata", "graph"], ChartType.BAR);

  const initialCompositeWidgetCheck = () => {
    if (!widgetChildren.length) return true;
    let invalid = false;
    forEach(widgetChildren, (child: any) => {
      if (child.type === undefined) invalid = true;
      if (!validateTimeRangeFilter(child)) invalid = true;
    });
    return invalid;
  };

  const previewAvailable = (): boolean => {
    if (!validateTimeRangeFilter(widget)) {
      return false;
    }

    const widgetValidFunction = getWidgetConstant(WIDGET_VALIDATION_FUNCTION, selectedReport);
    if (
      widgetValidFunction &&
      [
        "code_volume_vs_deployment_report",
        "tickets_counts_stat",
        "azure_tickets_counts_stat",
        "github_coding_days_report",
        "github_coding_days_single_stat",
        ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT,
        ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_SINGLE_STAT,
        JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_SINGLE_STAT,
        JENKINS_REPORTS.JOBS_COMMITS_LEAD_SINGLE_STAT_REPORT
      ].includes(selectedReport)
    ) {
      return widgetValidFunction(widget);
    }

    if (DisabledPreviewWidgets.includes(widget.type as any)) {
      return false;
    }

    if (["zendesk_time_across_stages", "salesforce_time_across_stages"].includes(widget.type)) {
      if (widget.query["state_transition"]) {
        const fromState = get(widget.query, ["state_transition", "from_state"], "");
        const toState = get(widget.query, ["state_transition", "to_state"], "");
        return fromState.length > 0 && toState.length > 0;
      } else return false;
    }

    if ([WidgetType.CONFIGURE_WIDGET, WidgetType.CONFIGURE_WIDGET_STATS].includes(widget.widget_type)) {
      return validateConfigTableWidget(widget);
    }
    if (widget.widget_type !== WidgetType.COMPOSITE_GRAPH && !widget.type) {
      return false;
    }

    if (widget.type && ["hygiene_report", "hygiene_report_trends"].includes(widget.type)) {
      // return false;
    }

    if (widget.widget_type === WidgetType.COMPOSITE_GRAPH) {
      if (initialCompositeWidgetCheck()) return false;
      const { combinedQuery, requiredFilters } = widgetChildren.reduce(
        (acc: any, widget: any) => ({
          combinedQuery: { ...acc.combinedQuery, ...widget.query },
          requiredFilters: [...acc.requiredFilters, ...get(widgetConstants, [widget.type, "requiredFilters"], [])]
        }),
        { combinedQuery: {}, requiredFilters: [] }
      );
      return requiredFilters.every((key: any) => {
        if (
          widget.query[(valuesToFilters as any)[key]] !== undefined &&
          typeof widget.query[(valuesToFilters as any)[key]] === "object"
        ) {
          return Object.keys(widget.query[(valuesToFilters as any)[key]]).length > 0;
        }
        return combinedQuery[(valuesToFilters as any)[key]];
      });
    }
    const requiredFilters = getWidgetConstant("requiredFilters", selectedReport);
    let requiredFiltersValid = true;
    if (requiredFilters) {
      requiredFiltersValid = requiredFilters
        .map((key: string) => (valuesToFilters as any)[key] || key)
        .every((key: string) => {
          if (typeof widget.query[key] === "object" && widget.query[key] !== undefined) {
            return Object.keys(widget.query[key]).length > 0;
          }
          return widget.query[key];
        });
    }

    if (
      jiraBAStatReports.includes(selectedReport) ||
      [
        jiraBAReportTypes.EFFORT_INVESTMENT_TREND_REPORT,
        ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_TREND_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT
      ].includes(selectedReport)
    ) {
      return requiredFiltersValid;
    }
    if (widget.type && getWidgetConstant("xaxis", selectedReport) === false) {
      if (
        graphType === WidgetType.STATS &&
        !["jira", "jenkinsgithub"].includes(getWidgetConstant("application", selectedReport)) &&
        widget.type !== "github_commits_single_stat"
      ) {
        return widget.name !== undefined && widget.name !== "";
      }
      if (
        ["jira", "jenkinsgithub"].includes(getWidgetConstant("application", selectedReport)) ||
        widget.type === "github_commits_single_stat"
      ) {
        let valid = true;
        if (graphType === WidgetType.STATS) {
          if (sprintStatReports.includes(selectedReport)) {
            return valid;
          }
          if (NotTimePeriodSupportedReports.includes(selectedReport)) {
            return valid && widget.query.agg_type !== undefined;
          }
          return valid && widget.query.time_period !== undefined && widget.query.agg_type !== undefined;
        }
        if (widget.query && widget.query.parameters && widget.query.parameters.length > 0) {
          return (
            valid &&
            widget.name !== undefined &&
            widget.query.parameters
              .map((param: any) => param.name !== undefined && param.name !== "" && param.values.length > 0)
              .reduce((acc: any, obj: boolean) => {
                return acc && obj;
              }, true)
          );
        }
      }
      return requiredFiltersValid;
    }

    if (widget.type && widget.type.includes("hygiene")) {
      const totalWeights = Object.keys(widget.weights).reduce((acc, obj) => {
        acc = acc + widget.weights[obj];
        return acc;
      }, 0);
      return requiredFiltersValid && totalWeights <= 100;
    }
    const across = get(query, ["across"], undefined);
    if ([WidgetType.GRAPH_NOTES, WidgetType.STATS_NOTES].includes(widget.widget_type)) {
      return true;
    }

    return requiredFiltersValid && widget.type && across && across.length > 0;
  };
  if (!defaultProfile && ALL_VELOCITY_PROFILE_REPORTS.includes(widget.type as any)) {
    return null;
  } else if (defaultProfile && !widget?.query?.velocity_config_id) {
    query = {
      ...widget?.query,
      velocity_config_id: defaultProfile?.id
    };
    dispatch(widgetUpdate(widgetId, { ...widget?.json, query }));
  }
  return (
    <WidgetPreviewComponent
      widgetId={widgetId}
      dashboardId={dashboardId}
      application={application}
      selectedReport={selectedReport}
      query={query}
      previewDisabled={!previewAvailable()}
      globalFilters={globalFilters}
      graphType={graphType}
      max_records={max_records}
      weights={weights}
      metadata={metadata}
      custom_hygienes={custom_hygienes}
      tableWidgetGraphType={tableWidgetGraphType}
      configurePreview={configurePreview}
      previewOnly={previewOnly}
    />
  );
};

export default React.memo(WidgetPreviewWrapperComponent);
