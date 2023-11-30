import React, { useContext, useEffect, useMemo, useCallback, ReactNode } from "react";
import { RestWidget } from "classes/RestDashboards";
import Loader from "components/Loader/Loader";
import { WidgetFilterContext, WidgetIntervalContext, WidgetLoadingContext } from "dashboard/pages/context";
import { useDispatch } from "react-redux";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import ChartContainer from "../chart-container/chart-container.component";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import { getWidgetaDataAction } from "reduxConfigs/actions/restapi/widgetAPIActions";
import { getWidgetDataSelector } from "reduxConfigs/selectors/widgetAPISelector";
import { get } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { GET_FILTERS } from "dashboard/constants/applications/names";
import { widgetTableFiltersUpdate } from "reduxConfigs/actions/restapi";
import { EmptyWidgetPreviewArgsType } from "model/report/dev-productivity/baseDevProductivityReports.constants";
import "../widget-api-wrapper/widgetApiWrapper.styles.scss";
import { ChartType } from "../chart-container/ChartType";
interface DevProductivityAPIWrapperProps {
  id: string;
  globalFilters: any;
  filterApplyReload?: number;
  onChartClick?: (data: any) => void;
  chartClickEnable?: boolean;
  application: string;
  dashboardMetaData?: any;
}

const DevProductivityAPIWrapper: React.FC<DevProductivityAPIWrapperProps> = ({
  globalFilters,
  id: widgetId,
  onChartClick,
  dashboardMetaData
}) => {
  const { filters: contextFilters } = useContext(WidgetFilterContext);

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const reportType = widget?.type;
  const location = useLocation();
  const queryParamOU = queryString.parse(location.search).OU as string;
  const dashboardOuIds = useMemo(() => {
    return queryParamOU ? [queryParamOU] : [];
  }, [queryParamOU]);
  const widgetConstant = get(widgetConstants, [reportType], []);
  const { setWidgetLoading } = useContext(WidgetLoadingContext);
  const { tempWidgetInterval } = useContext(WidgetIntervalContext);

  const dispatch = useDispatch();
  const widgetEntitlementsCheck = getWidgetConstant(reportType, "widgetEntitlements", undefined);
  const widgetEntitlements: any = widgetEntitlementsCheck ? widgetEntitlementsCheck() : widgetEntitlementsCheck;
  const getFilters = () => {
    const getFilter = getWidgetConstant(reportType, GET_FILTERS);
    if (getFilter) {
      return getFilter({
        widgetFilters: widget?.widgetFilters,
        filters: widget?.query,
        globalFilters,
        contextFilters,
        dashboardOuIds,
        tempWidgetInterval: tempWidgetInterval?.[widgetId] || undefined
      });
    }
  };

  const saveTableFilters = (filters: Map<string, { key: string; value: string }>) => {
    dispatch(widgetTableFiltersUpdate(widget.id, Object.fromEntries(filters)));
  };

  useEffect(() => {
    dispatch(getWidgetaDataAction(reportType, widgetId, getFilters()));
  }, [reportType, widgetId, queryParamOU, tempWidgetInterval?.[widgetId]]);

  const widgetDataState = useParamSelector(getWidgetDataSelector, { widgetId });

  useEffect(() => {
    setWidgetLoading(widgetId, widgetDataState.isLoading);
  }, [widgetDataState]);

  const handleCellClick = useCallback(
    (data: any) => {
      data.interval = tempWidgetInterval?.[widgetId] || data?.interval;
      onChartClick && onChartClick(data);
    },
    [getFilters, tempWidgetInterval, widgetId]
  );

  const tableFilters = useMemo(
    () => (widget.table_filters ? new Map(Object.entries(widget.table_filters)) : new Map()),
    [widget.table_filters]
  );

  const chartProps = useMemo(() => {
    const initialChartProps = widgetConstant.getChartProps
      ? widgetConstant.getChartProps(widget, tempWidgetInterval?.[widgetId])
      : {};
    return {
      ...initialChartProps,
      onClick: handleCellClick,
      reportType,
      id: widgetId,
      loading: widgetDataState.isLoading,
      dataSource: widgetDataState.data,
      filters: getFilters(),
      dashboardOuIds: dashboardOuIds,
      tableFilters,
      saveTableFilters,
      tempWidgetInterval: tempWidgetInterval?.[widgetId] || undefined,
      widgetEntitlements
    };
  }, [
    reportType,
    widgetId,
    widgetDataState,
    widgetConstant,
    handleCellClick,
    getFilters,
    tempWidgetInterval,
    widgetEntitlements
  ]);

  const renderData = useMemo(() => {
    const apiErrorCode = get(widgetDataState, ["error", "response", "status"], undefined);
    const emptyWidgetPreview: (args: EmptyWidgetPreviewArgsType) => ReactNode = getWidgetConstant(
      reportType,
      "render_empty_widget_preview_func",
      undefined
    );

    if (apiErrorCode && !!emptyWidgetPreview) {
      return emptyWidgetPreview({ errorCode: apiErrorCode as number });
    }

    return <ChartContainer chartType={ChartType.FILTERABLE_RAW_STATS_TABLE} chartProps={chartProps} />;
  }, [widgetDataState, chartProps, reportType]);

  if (widgetDataState.isLoading) return <Loader />;

  return <div className="widget-api-container">{renderData}</div>;
};

export default DevProductivityAPIWrapper;
