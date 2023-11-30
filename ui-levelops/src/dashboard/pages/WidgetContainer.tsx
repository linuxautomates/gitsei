import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { RouteComponentProps, useParams, withRouter } from "react-router-dom";
import { Col } from "antd";
import { get, unset } from "lodash";
import { valuesToFilters } from "../constants/constants";
import widgetConstants, { getWidgetConstant } from "../constants/widgetConstants";
import { AntCard, AntText } from "../../shared-resources/components";
import { ChartType } from "../../shared-resources/containers/chart-container/ChartType";
import { DashboardGraphsContainer } from "../containers";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { dashboardWidgetChildrenSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { RestWidget } from "../../classes/RestDashboards";
import { ADD_PROJECT_INTEGRATION, REPORT_NOT_SUPPORTED } from "constants/formWarnings";
import { FileReports, WIDGET_MIN_HEIGHT } from "../constants/helper";
import { DASHBOARD_ROUTES, getBaseUrl } from "../../constants/routePaths";
import { jiraBAReportTypes } from "dashboard/constants/enums/jira-ba-reports.enum";
import { Widget } from "../components/widgets/widget";
import { transformCompositeWidgetsWithSingleReportToSingleReport } from "../../utils/widgetUtils";
import { WidgetType } from "dashboard/helpers/helper";
import DashboardNotesPreviewComponent from "../../configurable-dashboard/components/widget-preview/custom-preview/dashboard-notes-preview/dashboard-notes.component";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useDispatch, useSelector } from "react-redux";
import { jiraStatusFilterValues, _widgetUpdateCall, _widgetUpdate } from "reduxConfigs/actions/restapi";
import Loader from "components/Loader/Loader";
import { JiraReports, JiraStatReports } from "../constants/enums/jira-reports.enum";
import { GLOBAL_SETTINGS_UUID } from "../constants/uuid.constants";
import { SPRINT_GRACE_OPTIONS_REPORTS } from "dashboard/graph-filters/components/helper";
import { LEVELOPS_REPORTS } from "../reports/levelops/constant";
import { GET_CUSTOMIZE_TITLE } from "dashboard/constants/filter-key.mapping";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import { ProjectPathProps } from "classes/routeInterface";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";
interface WidgetContainerProps {
  widget: RestWidget;
  globalFilters: any;
  filterApplyReload: number;
  minHeight: string;
  widgetSpan: number;
  dashboardId: any;
  handleShowDrillDownPreview: (widgetId: string, filter: any, applicationType: any, stackFilters?: string[]) => void;
  drilldownSelected: boolean;
  availableApplications: string[];
  showActionButtons: boolean;
}

const WidgetContainer: React.FC<WidgetContainerProps & RouteComponentProps> = (
  props: WidgetContainerProps & RouteComponentProps
) => {
  const {
    widget: _widget,
    filterApplyReload,
    globalFilters,
    handleShowDrillDownPreview,
    dashboardId,
    widgetSpan,
    minHeight,
    drilldownSelected,
    availableApplications,
    showActionButtons
  } = props;

  const dispatch = useDispatch();
  const projectParams = useParams<ProjectPathProps>();
  const {
    utils: { getLocationPathName }
  } = useParentProvider();
  let children = useParamSelector(dashboardWidgetChildrenSelector, {
    dashboard_id: dashboardId,
    widget_id: _widget.id
  });

  const firstChild = children[0];

  const widget: RestWidget = useMemo(() => {
    if (_widget.isComposite && _widget.children.length === 1) {
      return transformCompositeWidgetsWithSingleReportToSingleReport(_widget, firstChild);
    }
    return _widget;
  }, [_widget, firstChild]);

  if (_widget.isComposite && _widget.children.length === 1) {
    children = [];
  }

  const dashboard = useSelector(selectedDashboard);

  const [excludeFiltersLoading, setExcludeFiltersLoading] = useState(true);
  const [scmGlobalSettingsLoading, setSCMGlobalSettingsLoading] = useState(true);

  const excludeStatusState = useParamSelector(getGenericRestAPISelector, {
    uri: "jira_filter_values",
    method: "list",
    uuid: "exclude_status"
  });

  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });

  const gType = widget.widget_type;
  const typeInConstant = get(widgetConstants, [widget.type], undefined);
  const uri = get(widgetConstants, [widget.type, "uri"], undefined);
  const method = get(widgetConstants, [widget.type, "method"], undefined);
  const chartType = get(widgetConstants, [widget.type, "chart_type"], undefined);
  const chartProps = get(widgetConstants, [widget.type, "chart_props"], undefined);
  const applicationType = get(widgetConstants, [widget.type, "application"], "");
  const widgetFilters = get(widgetConstants, [widget.type, "filters"], undefined);
  const clickEnabled = get(widgetConstants, [widget.type, "chart_click_enable"], true);
  const getCustomTitle = get(widgetConstants, [widget.type, GET_CUSTOMIZE_TITLE], undefined);
  const hiddenFilters = useMemo(() => {
    return get(widgetConstants, [widget.type, "hidden_filters"], {});
  }, [widget.type]);

  const filters = widget.query || {};
  const graphType = widget.widget_type;
  const renderChartType = graphType.includes("composite") ? ChartType.COMPOSITE : chartType;
  const customHygienes = get(widget, ["metadata", "custom_hygienes"], []);
  const widgetMetaData = get(widget, ["metadata"], {});
  const validWidget = gType.includes("composite")
    ? widget.children && widget.children.length > 0
    : widget.type &&
      widget.type !== "" &&
      (typeInConstant !== undefined ||
        [LEVELOPS_REPORTS.TABLE_REPORT, LEVELOPS_REPORTS.TABLE_STAT_REPORT].includes(widget.type as any));

  useEffect(() => {
    let hasResolutionTimeReport = [
      JiraReports.RESOLUTION_TIME_REPORT,
      JiraReports.RESOLUTION_TIME_REPORT_TRENDS,
      JiraStatReports.RESOLUTION_TIME_COUNTS_STAT,
      ...SPRINT_GRACE_OPTIONS_REPORTS
    ].includes(widget.type);
    const isResolutionTrendReport = children.find((child: any) => child.type === "resolution_time_report_trends");
    hasResolutionTimeReport = hasResolutionTimeReport || isResolutionTrendReport;
    if (hasResolutionTimeReport) {
      const data = get(excludeStatusState, "data", {});

      if (Object.keys(data).length > 0) {
        setExcludeFiltersLoading(false);
      } else {
        dispatch(
          jiraStatusFilterValues(
            {
              fields: ["status"],
              filter: {
                status_categories: ["Done", "DONE"],
                integration_ids: dashboard?.query?.integration_ids || []
              }
            },
            "exclude_status"
          )
        );
      }
    } else {
      setExcludeFiltersLoading(false);
    }
  }, []);

  useEffect(() => {
    const loading = get(excludeStatusState, "loading", true);
    if (!loading) {
      setExcludeFiltersLoading(false);
    }
  }, [excludeStatusState]);

  useEffect(() => {
    const loading = get(globalSettingsState, "loading", true);
    if (!loading) {
      setSCMGlobalSettingsLoading(false);
    }
  }, [globalSettingsState]);

  const handleChartClick = (filter: any, applicationType: string, reportFilters?: any, stackFilters?: string[]) => {
    let OUFilters: any = undefined;
    const ou_ids = reportFilters?.ou_ids || [];
    if (ou_ids?.length) {
      OUFilters = {
        ou_ids
      };
      if (Object.keys(reportFilters?.ou_user_filter_designation || {}).length) {
        OUFilters["ou_user_filter_designation"] = reportFilters?.ou_user_filter_designation;
      }

      if ((reportFilters?.ou_exclusions || []).length) {
        OUFilters["ou_exclusions"] = reportFilters?.ou_exclusions;
      }
      if (reportFilters?.hasOwnProperty("apply_ou_on_velocity_report")) {
        OUFilters.apply_ou_on_velocity_report = reportFilters?.apply_ou_on_velocity_report;
      }
    }

    if (
      [
        FileReports.JIRA_SALESFORCE_FILES_REPORT,
        FileReports.JIRA_ZENDESK_FILES_REPORT,
        FileReports.SCM_FILES_REPORT,
        FileReports.SCM_JIRA_FILES_REPORT
      ].includes(widget.type) &&
      typeof filter === "object" &&
      filter?.type
    ) {
      const module = widget.type === "scm_jira_files_report" ? filter?.scm_module : filter?.module;
      let url = `${getBaseUrl(projectParams)}${
        DASHBOARD_ROUTES.DRILL_DOWN
      }?application=${applicationType}&dashboardId=${dashboardId}&widgetId=${
        widget.id
      }&x=${module}&filters=${JSON.stringify(reportFilters)}&across=${reportFilters?.across}`;
      if (OUFilters && Object.keys(OUFilters || {}).length) {
        url = `${url}&OUFilter=${JSON.stringify(OUFilters)}`;
      }
      window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
    } else if (
      [
        jiraBAReportTypes.JIRA_PROGRESS_REPORT,
        azureBAReportTypes.AZURE_ISSUES_PROGRESS_REPORT,
        jiraBAReportTypes.EPIC_PRIORITY_TREND_REPORT,
        azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT
      ].includes(widget.type)
    ) {
      const drilldownValueToFilters = get(widgetConstants, [widget.type, "valuesToFilters"], undefined);
      let key = get(valuesToFilters, [reportFilters?.across], reportFilters?.across);
      if (drilldownValueToFilters) {
        key = get(drilldownValueToFilters, [reportFilters?.across], reportFilters?.across);
      }
      let finalFilters = { ...reportFilters, filter: { ...reportFilters?.filter } };
      if (typeof filter === "object" && widget.type === azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT) {
        const buildDrilldownFilters = get(
          widgetConstants,
          [widget.type, "drilldown", "drilldownTransformFunction"],
          undefined
        );
        if (buildDrilldownFilters) {
          finalFilters = buildDrilldownFilters({ filter: finalFilters, key: key, onClickData: filter });
        }
      } else {
        finalFilters = { ...reportFilters, filter: { ...reportFilters?.filter, [key]: [filter] } };
      }

      unset(finalFilters, ["filter", "workitem_parent_workitem_types"]);
      let url = `${getBaseUrl(projectParams)}${
        DASHBOARD_ROUTES.DRILL_DOWN
      }?application=${applicationType}&dashboardId=${dashboardId}&widgetId=${
        widget.id
      }&x=${filter}&filters=${JSON.stringify(finalFilters)}&across=${reportFilters?.across}`;
      if (OUFilters && Object.keys(OUFilters || {}).length) {
        url = `${url}&OUFilter=${JSON.stringify(OUFilters)}`;
      }
      window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
    } else {
      handleShowDrillDownPreview(widget.id, filter, applicationType, stackFilters);
    }
  };

  const transformIconType = useCallback(
    (applicationType: string) => {
      if (applicationType.includes("github") || applicationType.includes("bitbucket")) {
        applicationType = applicationType.replace("github", "").replace("bitbucket", "");
        if (availableApplications.includes("github")) {
          applicationType = applicationType + "github";
        }
        if (availableApplications.includes("gitlab")) {
          applicationType = applicationType + "gitlab";
        }
        if (availableApplications.includes("bitbucket")) {
          applicationType = applicationType + "bitbucket";
        }
        if (availableApplications.includes("helix")) {
          applicationType = applicationType + "perforce_helix_core" + "perforce_helix_swarm";
        }
        if (availableApplications.includes("helix_core")) {
          applicationType = applicationType + "perforce_helix_core";
        }
        if (availableApplications.includes("helix_swarm")) {
          applicationType = applicationType + "perforce_helix_swarm";
        }
        if (applicationType.includes("jenkins")) {
          applicationType = applicationType.replace("jenkins", "");
          if (availableApplications.includes("azure_devops")) {
            applicationType += "azure_devops";
          }
          if (availableApplications.includes("jenkins")) {
            applicationType += "jenkins";
          }
        }
        return applicationType;
      }
      return applicationType;
    },
    [availableApplications]
  );

  const setWidth = useCallback(() => {}, []);
  const setReload = useCallback(() => {}, []);

  const style = useMemo(() => {
    return {
      display: "flex",
      minHeight: minHeight,
      alignItems: "center",
      justifyContent: "center"
    };
  }, [minHeight]);

  if (!validWidget) {
    const title = getCustomTitle
      ? getCustomTitle({
          title: widget.name,
          interval: widget.query?.interval,
          widgetId: widget.id,
          displayOnlyTitle: true
        })
      : widget.name || "Unnamed Widget";
    return (
      <>
        <Col span={widgetSpan} key={widget.id}>
          <AntCard className="invalid-widget" title={title} bordered>
            <div style={style}>
              <AntText type={"secondary"}>
                {typeInConstant === undefined ? REPORT_NOT_SUPPORTED : ADD_PROJECT_INTEGRATION}
              </AntText>
            </div>
          </AntCard>
        </Col>
      </>
    );
  }

  if ([WidgetType.STATS_NOTES, WidgetType.GRAPH_NOTES].includes(widget.widget_type)) {
    return (
      <DashboardNotesPreviewComponent
        widgetId={widget.id}
        dashboardId={dashboardId}
        previewOnly={false}
        hidden={widget.hidden}
      />
    );
  }
  const renderWidget = (
    <Col span={widgetSpan} id={widget.id}>
      <Widget
        dashboardId={dashboardId}
        showActionButtons={showActionButtons}
        id={widget.id}
        setWidth={setWidth}
        width={widget.width}
        title={widget.name}
        description={widget.description}
        hidden={widget.hidden}
        widgetType={widget.widget_type}
        reportType={widget.type}
        applicationType={applicationType}
        globalFilters={globalFilters}
        icon_type={transformIconType(applicationType)}
        drilldownSelected={drilldownSelected}
        setReload={setReload}
        disableGlobalFilters={get(widget.metadata, "disable_or_filters", false)}
        getCustomTitle={getCustomTitle}
        interval={widget.query?.interval}
        height={getWidgetConstant(widget.type, WIDGET_MIN_HEIGHT)}>
        {excludeFiltersLoading || scmGlobalSettingsLoading ? (
          <Loader />
        ) : (
          <DashboardGraphsContainer
            widgetId={widget.id}
            reportType={widget.type}
            applicationType={applicationType}
            uri={uri}
            method={method}
            chartType={renderChartType}
            chartProps={chartProps}
            globalFilters={globalFilters}
            localFilters={filters}
            widgetFilters={widgetFilters}
            hiddenFilters={hiddenFilters}
            reload={false}
            setReload={setReload}
            children={children}
            graphType={graphType}
            weights={widget.weights}
            maxRecords={widget.max_records}
            chartClickEnable={clickEnabled !== undefined ? clickEnabled : true}
            customHygienes={customHygienes}
            filterApplyReload={filterApplyReload}
            widgetMetaData={widgetMetaData}
            onChartClick={handleChartClick}
            jiraOrFilters={dashboard?.jira_or_query}
            dashboardMetaData={dashboard?._metadata}
            dashboardId={dashboardId}
          />
        )}
      </Widget>
    </Col>
  );

  return renderWidget;
};

export default React.memo(withRouter(WidgetContainer));
