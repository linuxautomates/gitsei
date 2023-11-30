import React, { useMemo } from "react";
import { AntText } from "shared-resources/components";
import { LineChartGraph } from "shared-resources/charts";
import "../dev-raw-stats-drilldown-component/LineGraphForDevRawStatsDrilldown.style.scss";
import { Empty } from "antd";
import { intervalToTrendStringMap, intervalToAggIntervalMapping } from "../dev-raw-stats-drilldown-component/constants";
import { getLastFourQuartersWithMonthAndYear, getIntervalString } from "utils/dateUtils";
import { TIME_INTERVAL_TYPES } from "constants/time.constants";

interface DemoLineGraphForDevRawProps {
  widgetId?: string | undefined;
  columnName: string;
  interval: string;
  graphData?: any;
}

const DemoLineGraphForDevRawStatsDrilldown: React.FC<DemoLineGraphForDevRawProps> = ({
  columnName,
  graphData,
  interval
}) => {
  const transformGraphData = (data: any) => {
    if (data?.length) {
      data.forEach((data: any, index: any) => {
        let nameLabelArray = getLastFourQuartersWithMonthAndYear();
        let additional_key = nameLabelArray[index];
        data.additional_key = additional_key;
        data.key = getIntervalString(additional_key, intervalToAggIntervalMapping[interval] as TIME_INTERVAL_TYPES);
      });
    }
    return data;
  };

  let additionalKeyForDot = getLastFourQuartersWithMonthAndYear()[graphData.length - 1];

  const dataSource = useMemo(() => transformGraphData(graphData), [graphData, transformGraphData, additionalKeyForDot]);

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
      yLabelValue={columnName}
      customizedDotLabel
      additionalKey={additionalKeyForDot}
    />
  );

  return (
    <div className="drilldownLineGraph">
      <AntText className="trendGraphTitle">{`Trend of ${columnName} by ${
        intervalToTrendStringMap[interval.toUpperCase()]
      }`}</AntText>
      <hr className="horRow" />
      <div className="graphContainer">{dataSource.length === 0 ? <Empty description="No Data" /> : getLineChart()}</div>
    </div>
  );
};

export default DemoLineGraphForDevRawStatsDrilldown;
