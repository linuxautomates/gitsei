import React, { useEffect, useMemo, useState } from "react";
import "../../scorecard/components/RelativeScoreChart.scss";
import { Empty } from "antd";
import { ResponsiveContainer } from "recharts";
import { LineChart } from "../../../../shared-resources/charts";
import { getLastFourQuartersWithYear, getLastTwelveMonthsWithyear } from "utils/dateUtils";

interface DemoRelativeScoreChartProps {
  extraFilters: React.ReactNode;
  data: any;
  relativeScoreDataPeriod?: string;
}

const DemoRelativeScoreChart: React.FC<DemoRelativeScoreChartProps> = ({
  extraFilters,
  data,
  relativeScoreDataPeriod
}) => {
  const [transformedData, setTransformedData] = useState<any>([]);

  const transformData = (data: any) => {
    if (data?.length > 0) {
      if (relativeScoreDataPeriod === "relativeScoreDataMonthly") {
        const nameLabelArray = getLastTwelveMonthsWithyear();
        data?.forEach((data: any, index: any) => {
          data.name = nameLabelArray[index];
        });
      }
      if (relativeScoreDataPeriod === "relativeScoreDataQuarterly") {
        const nameLabelArray = getLastFourQuartersWithYear();
        data?.forEach((data: any, index: any) => {
          data.name = nameLabelArray[index];
        });
      }
    }
    return data;
  };

  useEffect(() => {
    setTransformedData(transformData(data));
  }, [data, transformData]);

  return transformedData?.length > 0 ? (
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

export default DemoRelativeScoreChart;
