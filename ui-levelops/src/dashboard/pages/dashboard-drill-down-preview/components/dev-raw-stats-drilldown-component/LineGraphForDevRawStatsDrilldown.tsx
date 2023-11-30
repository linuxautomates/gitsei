import React, { useEffect, useMemo } from "react";
import { useDispatch } from "react-redux";
import Loader from "components/Loader/Loader";
import { getWidgetGraphDataAction } from "reduxConfigs/actions/widgetGraphAPIActions";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { getWidgetGraphDataSelector } from "reduxConfigs/selectors/widgetGraphAPISelector";
import { AntText } from "shared-resources/components";
import { LineChartGraph } from "shared-resources/charts";
import "./LineGraphForDevRawStatsDrilldown.style.scss";
import { transformGraphData } from "./helper";
import { Empty } from "antd";
import { intervalToTrendStringMap } from "./constants";
import { rawStatsColumns } from "dashboard/reports/dev-productivity/rawStatsTable.config";

interface LineGraphForDevRawProps {
  widgetId: string;
  filters: any;
  columnName: string;
  interval: string;
  onChartClick?: (data: any) => void;
  additionalKey?: string;
}

const LineGraphForDevRawStatsDrilldown: React.FC<LineGraphForDevRawProps> = ({
  filters,
  widgetId,
  columnName,
  interval,
  onChartClick,
  additionalKey
}) => {
  const dispatch = useDispatch();

  useEffect(() => {
    dispatch(getWidgetGraphDataAction(widgetId as string, filters));
  }, [filters, widgetId]);

  const widgetDataGraphState = useParamSelector(getWidgetGraphDataSelector, { widgetId });
  const agg_interval = filters?.filter?.agg_interval;
  const columnTitle: string = useMemo(
    () => rawStatsColumns[columnName]?.titleForCSV || (rawStatsColumns[columnName]?.title as string) || "",
    [columnName]
  );

  const dataSource = useMemo(
    () =>
      widgetDataGraphState.data && columnName
        ? transformGraphData(widgetDataGraphState.data, columnName, agg_interval)
        : [],
    [widgetDataGraphState, columnName, agg_interval]
  );

  const getLineChart = () => (
    <LineChartGraph
      data={dataSource}
      chartProps={{
        margin: {
          top: 20,
          bottom: 20,
          left: 20,
          right: 20
        }
      }}
      lines={["value"]}
      lineType="linear"
      xLabelValue="Duration"
      xAxisDataKey="key"
      yLabelValue={columnTitle}
      customizedDotLabel
      onClick={onChartClick}
      additionalKey={additionalKey}
    />
  );

  return (
    <div className="drilldownLineGraph">
      <AntText className="trendGraphTitle">{`Trend of ${columnTitle} by ${intervalToTrendStringMap[interval]}`}</AntText>
      <hr className="horRow" />
      <div className="graphContainer">
        {widgetDataGraphState.isLoading ? (
          <Loader />
        ) : widgetDataGraphState.error || dataSource.length === 0 ? (
          <Empty description="No Data" />
        ) : (
          getLineChart()
        )}
      </div>
    </div>
  );
};

export default LineGraphForDevRawStatsDrilldown;
