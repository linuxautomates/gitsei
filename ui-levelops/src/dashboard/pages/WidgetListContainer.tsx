import { Row } from "antd";
import { RestIntegrations } from "classes/RestIntegrations";
import { useIntegrationList } from "custom-hooks/useIntegrationList";
import {
  HYGIENE_TREND_REPORT,
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  JIRA_SPRINT_DISTRIBUTION_REPORTS,
  JIRA_SPRINT_REPORTS,
  PAGERDUTY_REPORT,
  SCM_ADDITIONAL_KEYS_APPLICATIONS,
  SCM_FILES_REPORT
} from "dashboard/constants/applications/names";
import { IGNORED_ADDITIONAL_KEY_REPORTS_FOR_DRILLDOWN_TITLE } from "dashboard/constants/constants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { WidgetType } from "dashboard/helpers/helper";
import { get, isEqual, isNull, isUndefined } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { SlideDown } from "react-slidedown";
import { toTitleCase } from "utils/stringUtils";
import { RestWidget } from "../../classes/RestDashboards";
import { DASHBOARD_PAGE_SIZE } from "../../constants/dashboard";
import {
  dashboardGraphWidgetsSelector,
  dashboardStatWidgetsSelector,
  dashboardWidgetsDataSelector
} from "reduxConfigs/selectors/dashboardSelector";
import { jiraIntegrationConfigListSelector } from "reduxConfigs/selectors/jira.selector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { WidgetDrilldownHandlerContext } from "./context";
import { DashboardDrillDownPreview } from "./index";
import WidgetContainer from "./WidgetContainer";

interface WidgetListContainerProps {
  type: string;
  dashboardId: string;
  query: any;
  updatedWidgetsMap: any;
  widgetPage: number;
  addNewParent: any;
  drilldownIndex: number | undefined;
  setDrilldownIndex: any;
  drilldownType: WidgetType;
  setDrilldownType: any;
  drilldownWidgetId: string | undefined;
  setDrilldownWidgetId: any;
  updateModuleForSalesforceReport?: any;
  showActionButtons: boolean;
}

const WidgetListContainer: React.FC<WidgetListContainerProps> = (props: WidgetListContainerProps) => {
  const {
    updatedWidgetsMap,
    dashboardId,
    query,
    addNewParent,
    updateModuleForSalesforceReport,
    widgetPage,
    drilldownIndex,
    setDrilldownIndex,
    drilldownType,
    setDrilldownType,
    drilldownWidgetId,
    setDrilldownWidgetId,
    showActionButtons
  } = props;

  const [drillDownProps, setDrillDownProps] = useState<any | undefined>(undefined);
  const [reloadWidgets, setReloadWidgets] = useState<any>({});

  const _jiraIntegrationConfigState = useSelector(jiraIntegrationConfigListSelector);
  const _graphWidgets = useParamSelector(dashboardGraphWidgetsSelector, { dashboard_id: dashboardId });
  const _statWidgets = useParamSelector(dashboardStatWidgetsSelector, { dashboard_id: dashboardId });
  const [integrationsLoading, integrationList] = useIntegrationList({ dashboardId }, [dashboardId]);
  const dashboardWidgetState = useParamSelector(dashboardWidgetsDataSelector, {
    dashboard_id: dashboardId,
    widget_type: props.type
  });

  const { widgets } = dashboardWidgetState;

  const filteredWidgets = widgets.filter((wid: RestWidget) => !wid.draft);

  const slicedWidgets =
    props.type === WidgetType.GRAPH ? filteredWidgets.slice(0, widgetPage * DASHBOARD_PAGE_SIZE) : filteredWidgets;

  useEffect(() => {
    if (!isEqual(reloadWidgets, updatedWidgetsMap)) {
      setReloadWidgets({ ...updatedWidgetsMap });
    } // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [dashboardWidgetState]);

  const gutter: any = useMemo(() => [32, 32], []);
  const style: any = useMemo(() => ({ margin: 0 }), []);

  const slideDownStyle = useMemo(() => ({ width: "100%" }), []);

  const handleDrillDownPreviewClose = useCallback(() => {
    setDrilldownIndex(undefined);
    setDrilldownWidgetId(undefined);
  }, []);

  const getWidgetFromId = useCallback(
    (widgetId: string) => filteredWidgets.find((widget: RestWidget) => widget.id === widgetId),
    [filteredWidgets]
  );

  const handleShowDrillDownPreview = useCallback(
    (widgetId: string, filter: any, applicationType: string, stackFilters?: string[]) => {
      //Adding this for disabling drilldowns for stats widgets
      const currWidget = getWidgetFromId(widgetId);
      const allowStatDrillown = get(widgetConstants, [currWidget.type, "drilldown", "allowDrilldown"], false);
      if (props.type === WidgetType.STATS && !allowStatDrillown) {
        return;
      }

      if (!widgetId || !filter || !applicationType) {
        return;
      }

      let xAxisOverride;

      let widget = null;
      let index = -1;
      let nextWidth = "";
      let col = 0;

      for (let i = 0; i < _graphWidgets.length; i++) {
        widget = _graphWidgets[i];
        if (i > 0 && _graphWidgets[i - 1].width !== "full") {
          col = col + 1;
        }

        if (i > 0 && _graphWidgets[i - 1].width === "full") {
          col = 0;
        }

        if (col === 2) {
          col = 0;
        }

        if (col === 1 && _graphWidgets[i].width === "full") {
          col = 0;
        }

        if (widget.id === widgetId) {
          index = i;
          nextWidth = _graphWidgets[i + 1] ? _graphWidgets[i + 1].width : "";
          break;
        }
      }
      let drillDownTitle = typeof filter === "object" ? "" : toTitleCase(filter);
      if (filter === "stat") {
        const _index = _statWidgets.findIndex((widget: any) => widget.id === widgetId);
        widget = _statWidgets[_index];
        const statCount = _statWidgets.length;
        index = (Math.floor(_index / 4) + 1) * (statCount >= 4 ? 4 : statCount) - 1;
        if (index >= _statWidgets.length) {
          index = _statWidgets.length - 1;
        }
      }

      if (!widget) {
        return;
      }

      const widgetMetaData = get(widget, ["metadata"], {});
      if ((widget.type === "hygiene_report" || widget.type === "azure_hygiene_report") && typeof filter === "object") {
        const records = get(_jiraIntegrationConfigState, ["data", "records"], []);
        const cHygienes = records.reduce((agg: any[], obj: any) => {
          const hygienes = get(obj, ["custom_hygienes"], []);
          agg.push(...hygienes);
          return agg;
        }, []);
        const custom_hygiene = cHygienes.find((hy: { id: any }) => hy.id === filter.id);
        if (custom_hygiene) {
          xAxisOverride = {
            hygiene: filter.hygiene,
            id: filter.id,
            missing_fields: custom_hygiene.missing_fields || {},
            filter: custom_hygiene.filter || {}
          };
        }
        drillDownTitle = toTitleCase(filter?.hygiene);
      }
      if (["jenkins_job_count_treemap"].includes(applicationType)) {
        if (filter["cicd_job_id"]) {
          addNewParent(widgetId, filter);
        }
        return;
      } else if (["jirazendesk", "jirasalesforce"].includes(applicationType)) {
        xAxisOverride = "status";
      }
      setDrilldownWidgetId(widgetId);

      let additionFilter: any = {};

      let x_axis = filter === "show_all" ? "" : xAxisOverride || filter;
      if (widget.type === SCM_FILES_REPORT) {
        x_axis = filter["repo_ids"];
        additionFilter = { filename: filter["filename"] };
      }

      //TODO: hotfix for LFE:843 , Piyush will have a look at it
      if (
        SCM_ADDITIONAL_KEYS_APPLICATIONS.includes(applicationType) &&
        !IGNORED_ADDITIONAL_KEY_REPORTS_FOR_DRILLDOWN_TITLE.includes(widget?.type)
      ) {
        if (typeof filter === "object") {
          drillDownTitle = filter?.name;
          x_axis = filter?.id || filter;
        } else if (typeof filter === "string") {
          drillDownTitle = filter;
          x_axis = filter;
        }
      }

      if (HYGIENE_TREND_REPORT.includes(widget.type)) {
        drillDownTitle = filter?.name;
        x_axis = filter?.id;

        const records = get(_jiraIntegrationConfigState, ["data", "records"], []);
        const cHygienes = records.reduce((agg: any[], obj: any) => {
          const hygienes = get(obj, ["custom_hygienes"], []);
          agg.push(...hygienes);
          return agg;
        }, []);

        const custom_hygiene = cHygienes.find((hy: { id: any }) => hy.id === filter?.hygiene);
        if (custom_hygiene) {
          additionFilter = {
            missing_fields: custom_hygiene.missing_fields || {},
            filter: custom_hygiene.filter || {}
          };
        }
        if (filter?.hygiene) {
          additionFilter["hygiene_types"] = [filter?.hygiene];
        }
      }

      if (currWidget.type === PAGERDUTY_REPORT.RESPONSE_REPORTS) {
        if (typeof filter === "object") {
          drillDownTitle = filter.name;
          x_axis = filter.id;
        } else if (typeof filter === "string") {
          drillDownTitle = filter;
          x_axis = filter;
        }
      }

      if (
        [JIRA_SPRINT_REPORTS.SPRINT_METRICS_PERCENTAGE_TREND, JIRA_SPRINT_REPORTS.SPRINT_METRICS_TREND].includes(
          currWidget.type as any
        )
      ) {
        x_axis = filter.sprint_name;
        drillDownTitle = filter?.sprint_name;
      }

      if (
        [JIRA_SPRINT_DISTRIBUTION_REPORTS.SPRINT_DISTRIBUTION_REPORT].includes(currWidget.type as any) &&
        filter?.across
      ) {
        drillDownTitle = filter.across;
      }

      if (
        [ISSUE_MANAGEMENT_REPORTS.STAGE_BOUNCE_REPORT, JIRA_MANAGEMENT_TICKET_REPORT.STAGE_BOUNCE_REPORT].includes(
          currWidget.type as any
        )
      ) {
        const value = filter.value;
        if (typeof value == "object") {
          drillDownTitle = value?.name;
        } else {
          drillDownTitle = value;
        }
        x_axis = filter;
      }

      if (currWidget?.type === "review_collaboration_report") {
        drillDownTitle = filter.name;
      }

      const data = {
        application: applicationType,
        dashboardId,
        widgetId,
        x_axis,
        [applicationType]: filter,
        stackFilters,
        widgetMetaData,
        additionFilter,
        drillDownTitle
      };

      if (index === -1) {
        setDrillDownProps(undefined);
        return;
      }

      if (col === 0 && filter !== "stat" && _graphWidgets[index].width === "half" && nextWidth === "half") {
        index = index + 1;
      }

      setDrilldownIndex(index);
      setDrilldownType(props.type);
      setDrillDownProps(data);
      const shouldFocusOnDrilldown = get(widgetConstants, [currWidget?.type, "shouldFocusOnDrilldown"], false);
      let eleId = widgetId;
      if (shouldFocusOnDrilldown) {
        eleId = `${widgetId}-drilldown`;
      }
      setTimeout(() => {
        const ele = document.getElementById(eleId);
        ele?.scrollIntoView({ behavior: "smooth" });
      }, 700);
    },
    [props.type, _graphWidgets, _statWidgets, _jiraIntegrationConfigState, addNewParent, dashboardId, getWidgetFromId]
  );
  const renderPreview = useMemo(() => {
    if (isNull(drilldownIndex) || isUndefined(drilldownIndex) || isUndefined(drillDownProps)) {
      return null;
    }
    return (
      <SlideDown style={slideDownStyle}>
        <DashboardDrillDownPreview drillDownProps={drillDownProps} onDrilldownClose={handleDrillDownPreviewClose} />
      </SlideDown>
    ); // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [drillDownProps, drilldownIndex]);

  const availableApplications = useMemo(() => {
    if (integrationsLoading) return [];
    return (integrationList || ([] as any)).map((integration: RestIntegrations) => integration.application);
  }, [integrationList, integrationsLoading]);

  const widgetDrilldownHandler = useMemo(
    () => ({
      setDrilldown: (payload: any) => {
        if (isNull(payload) || isUndefined(payload)) {
          handleDrillDownPreviewClose();
        }
        setDrillDownProps(payload);
      },
      isDrilldownOpen: !(isNull(drilldownIndex) || isUndefined(drilldownIndex)),
      drilldownWidgetId: drilldownWidgetId ?? ""
    }),
    [drilldownIndex, drillDownProps, drilldownWidgetId]
  );

  return (
    <WidgetDrilldownHandlerContext.Provider value={widgetDrilldownHandler}>
      <Row gutter={gutter} type={"flex"} justify={"start"} style={style}>
        {(slicedWidgets as any).map((widget: RestWidget, index: number) => {
          let widgetSpan = widget.width === "full" ? 24 : 12;
          let minHeight = "350px";
          if (props.type === WidgetType.STATS) {
            widgetSpan = 6;
            minHeight = "60px";
          }

          if (
            widget.type === "effort_investment_single_stat" ||
            widget.type === "azure_effort_investment_single_stat"
          ) {
            widgetSpan = 24;
            minHeight = "60px";
          }

          return (
            <React.Fragment key={widget.id}>
              <WidgetContainer
                availableApplications={availableApplications}
                dashboardId={dashboardId}
                widgetSpan={widgetSpan}
                minHeight={minHeight}
                widget={widget}
                globalFilters={query}
                handleShowDrillDownPreview={handleShowDrillDownPreview}
                filterApplyReload={get(reloadWidgets, [widget.id], 0)}
                drilldownSelected={drilldownWidgetId === widget.id}
                showActionButtons={showActionButtons}
              />
              {index === drilldownIndex && drilldownType === props.type && renderPreview}
            </React.Fragment>
          );
        })}
      </Row>
    </WidgetDrilldownHandlerContext.Provider>
  );
};

export default React.memo(WidgetListContainer);
