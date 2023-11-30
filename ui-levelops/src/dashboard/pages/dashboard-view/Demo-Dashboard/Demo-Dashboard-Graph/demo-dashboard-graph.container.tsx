import { useDemoDashboardDataId } from "custom-hooks/useDemoDashboardDataKey";
import { JIRA_MANAGEMENT_TICKET_REPORT } from "dashboard/constants/applications/names";
import { GLOBAL_SETTINGS_UUID } from "dashboard/constants/uuid.constants";
import { get } from "lodash";
import queryString from "query-string";
import React, { useMemo } from "react";
import { useLocation } from "react-router-dom";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  backlogTrendReportChartType,
  getProps,
  resolutionTimeReportChartType
} from "shared-resources/containers/widget-api-wrapper/helper";
import DemoChartContainer from "./demo-chart-container";
import { GET_WIDGET_CHART_PROPS } from "dashboard/constants/filter-name.mapping";
import { WebRoutes } from "../../../../../routes/WebRoutes";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";

interface DemoDashboardGraphsContainerProps {
  widgetData: any;
  widgetConfig: any;
  id: string;
  reportType: string;
  widgetType: string;
  dashboardId: string;
  onChartClick?: (data: any) => void;
}

const DemoDashboardGraphsContainer: React.FC<DemoDashboardGraphsContainerProps> = (
  props: DemoDashboardGraphsContainerProps
) => {
  const { widgetData, widgetConfig, id, reportType, widgetType, onChartClick, dashboardId } = props;

  const demoDatakey: string | undefined = useDemoDashboardDataId(id);
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const ou_uuid = queryString.parse(location.search).ou_category_id as string;
  const globalSettingsState = useParamSelector(getGenericRestAPISelector, {
    uri: "configs",
    method: "list",
    uuid: GLOBAL_SETTINGS_UUID
  });
  const dashboardOuIds = useMemo(() => {
    return queryParamOU ? [queryParamOU] : [];
  }, [queryParamOU]);
  const {
    utils: { getLocationPathName }
  } = useParentProvider();

  const ouCategoryOuId = useMemo(() => {
    return ou_uuid ? [ou_uuid] : [];
  }, [ou_uuid]);
  const colorSchema = useMemo(() => {
    const globalColorSchema = globalSettingsState?.data?.records?.find(
      (item: any) => item?.name === "DASHBOARD_COLOR_SCHEME"
    );
    const scheme = globalColorSchema
      ? typeof globalColorSchema?.value === "string"
        ? JSON.parse(globalColorSchema?.value)
        : globalColorSchema?.value
      : [];
    return scheme.reduce((acc: Record<string, string>, item: any) => {
      acc = { ...acc, [`${item.key?.toLowerCase() ?? ""}`]: item.value };
      return acc;
    }, {});
  }, [globalSettingsState]);

  const chartType = useMemo(() => {
    let baseChartType = get(widgetConfig, ["chart_type"], "");
    const filters = get(widgetData, ["query", "filter"], {});
    switch (reportType) {
      case JIRA_MANAGEMENT_TICKET_REPORT.RESOLUTION_TIME_REPORT:
        baseChartType = resolutionTimeReportChartType(filters);
      case JIRA_MANAGEMENT_TICKET_REPORT.BACKLOG_TREND_REPORT:
        baseChartType = backlogTrendReportChartType(filters);
    }
    return baseChartType;
  }, [widgetConfig, reportType]);

  const getChartUnits: string[] = useMemo(() => {
    const getChartUnitHelper = get(widgetConfig, ["getChartUnits"]);
    if (getChartUnitHelper) {
      return getChartUnitHelper(get(widgetData, ["query"], {}));
    }
    return [];
  }, [widgetData, widgetConfig]);

  const handleChartClick = (data: any) => {
    onChartClick?.(data);
  };

  const chartProps = () => {
    const demoData = demoDatakey ? get(widgetData, ["data", demoDatakey, "data"], []) : [];
    const prevChartProps = get(widgetConfig, ["chart_props"], {});
    const applicationType = get(widgetConfig, ["application"], "");
    const widgetFilters = get(widgetData, ["query"], []);
    const clickEnabled = get(widgetData, ["isClickEnabled"], true);
    let chartProps = getProps(reportType, prevChartProps, { data: demoData }, widgetFilters, {});
    const getCustomChartProps = get(widgetConfig, ["get_custom_chart_props"]);
    const getDynamicColumns: any = get(widgetConfig, ["getDynamicColumns"], undefined);
    const getChartProps = get(widgetConfig, ["getChartProps"], {});
    const getWidgetChartProps = get(widgetConfig, [GET_WIDGET_CHART_PROPS], undefined);
    if (getCustomChartProps) {
      chartProps = getCustomChartProps(chartProps, widgetFilters, widgetFilters, demoData);
    }

    const onRowClick = (
      id: any,
      record: any,
      dashboardId: string,
      index: number,
      event: Event,
      interval: string | null,
      ou_id: Array<string>
    ) => {
      const dateInterval = "last_quarter";
      const url = WebRoutes.dashboard.demoDevProductivityDashboard(
        id,
        ou_id?.[0],
        dashboardId,
        index,
        record.ou_uuid,
        dateInterval
      );
      window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
    };

    const onRowDevScoreReportClick = (
      id: any,
      record: any,
      dashboardId: string,
      index: number,
      event: Event,
      interval: string | null,
      ou_id: Array<string>
    ) => {
      const dateInterval = "last_12_months";
      if (index !== 0) {
        const url = WebRoutes.dashboard.demoScoreCard(
          record.org_user_id,
          id,
          dashboardId,
          index,
          record.ou_uuid,
          ou_id,
          dateInterval
        );
        window.open(`${removeLastSlash(getLocationPathName?.())}${url}`);
      }
    };

    let initialChartProps = { tableFilters: new Map() };
    if (applicationType === "dev_productivity") {
      if (reportType === "dev_productivity_org_unit_score_report") {
        chartProps.onRowDemoTableClick = onRowClick;
      }
      if (reportType === "dev_productivity_score_report") {
        chartProps.onRowDemoTableClick = onRowDevScoreReportClick;
      }
      let total_count = reportType === "dev_productivity_score_report" ? demoData.length - 1 : demoData.length;
      if (reportType === "individual_raw_stats_report") {
        widgetConfig.selected_columns = [
          "Number of PRs per month",
          "Number of Commits per month",
          "Average Coding days per week",
          "Average PR Cycle Time",
          "Average time spent working on Issues",
          "Number of PRs commented on per month",
          "Number of PRs approved per month",
          "Percentage of Rework"
        ];
        initialChartProps = { ...initialChartProps, ...getChartProps(widgetConfig) };
      }
      if (reportType === "org_raw_stats_report") {
        widgetConfig.selected_columns = [
          "Number of PRs per month",
          "Number of Commits per month",
          "Lines of Code per month",
          "Average Coding days per week",
          "Average PR Cycle Time",
          "Number of PRs approved per month"
        ];
        initialChartProps = { ...initialChartProps, ...getChartProps(widgetConfig) };
      }
      if (getWidgetChartProps) {
        chartProps = { ...chartProps, ...getWidgetChartProps(widgetFilters) };
      }
      if (getDynamicColumns) {
        const dynamicColumns = getDynamicColumns((demoData as any) || []);
        chartProps = {
          ...chartProps,
          columns: [...chartProps?.columns, ...dynamicColumns],
          apisMetaData: { [id]: { total_count } }
        };
      }
    }

    return {
      id: props.id,
      ...chartProps,
      ...initialChartProps,
      xUnit: "",
      onClick: handleChartClick,
      isDemo: true,
      reportType: props.reportType,
      colorSchema: colorSchema,
      ...widgetData,
      data: demoDatakey ? get(widgetData, ["data", demoDatakey ?? "", "data"], []) : [],
      dataSource: demoDatakey ? get(widgetData, ["data", demoDatakey ?? "", "data"], []) : [],
      units: getChartUnits,
      hasClickEvents: clickEnabled,
      widgetFilters: get(widgetData, ["widget_filters"], {}),
      ou_id: [dashboardOuIds],
      ou_uuid: [ouCategoryOuId],
      dashboardId: dashboardId
    };
  };

  return (
    <div>
      <DemoChartContainer widgetType={widgetType} chartType={chartType} chartProps={chartProps()} />
    </div>
  );
};

export default DemoDashboardGraphsContainer;
