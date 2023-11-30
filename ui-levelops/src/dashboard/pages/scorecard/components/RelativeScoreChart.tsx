import React, { useEffect, useState } from "react";
import "./RelativeScoreChart.scss";
import { Empty, Spin } from "antd";
import { ResponsiveContainer } from "recharts";
import { LineChart } from "../../../../shared-resources/charts";
import { uniq } from "lodash";
import { AntText } from "shared-resources/components";
import { getReportScore } from "./helper";

interface RelativeScoreChartProps {
  extraFilters: React.ReactNode;
  data: { [key: string]: any }[];
  loading: boolean;
  error: boolean;
  displayMessage?: string;
}

const RelativeScoreChart: React.FC<RelativeScoreChartProps> = ({
  extraFilters,
  data,
  loading,
  error,
  displayMessage
}) => {
  const [transformedData, setTransformedData] = useState<{ [key: string]: any }[]>([]);

  const transformData = (data: { [key: string]: any }[]) => {
    const newData = data.sort((a: any, b: any) => a.key - b.key).map(getReportScore);
    const SupportedKeys = newData.reduce((acc: any, next: any) => {
      return uniq([...acc, ...Object.keys(next)]);
    }, []);
    const updatedData = newData?.map((item: any) => {
      let updatedItem = { ...item };
      const itemKeys = Object.keys(item || {});
      SupportedKeys.forEach((key: string) => {
        if (!itemKeys?.includes(key)) {
          updatedItem = {
            ...item,
            [key]: 0
          };
        }
      });
      return updatedItem;
    });
    return updatedData;
  };

  useEffect(() => {
    if (!loading && !error) {
      setTransformedData(transformData(data));
    }
  }, [data]);

  if (displayMessage) {
    return (
      <div className="dev-prod-relative-score-container-spinner">
        <AntText>{displayMessage}</AntText>
      </div>
    );
  }
  if (loading) {
    return (
      <div className="dev-prod-relative-score-container-spinner">
        <Spin />
      </div>
    );
  }
  return !error ? (
    <div className="dev-prod-relative-score">
      <div className="dev-prod-relative-score-header">
        <div className="dev-prod-relative-score-header-heading">TRELLIS SCORE OVER TIME</div>
        <div className="dev-prod-relative-score-header-extras">{extraFilters}</div>
      </div>
      <ResponsiveContainer height="90%" width="100%">
        <LineChart
          id={"dev-prod-relative-score"}
          xUnit={"time"}
          unit={"score"}
          data={transformedData}
          chartProps={{
            margin: {
              top: 10,
              bottom: 60,
              left: 10,
              right: 10
            }
          }}
          alwaysShowDot
          showYaxis
        />
      </ResponsiveContainer>
    </div>
  ) : (
    <div className="dev-prod-relative-score-nodata">
      <Empty description="No Data" />
    </div>
  );
};

export default RelativeScoreChart;
