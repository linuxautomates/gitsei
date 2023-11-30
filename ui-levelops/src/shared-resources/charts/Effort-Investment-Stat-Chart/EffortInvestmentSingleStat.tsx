import React from "react";
import { GraphStatChartProps } from "../chart-types";
import GraphStatChart from "../graph-stat-chart/graph-stat-chart.component";
import { Empty } from "antd";
import "./EffortInvestmentSingleStat.scss";
import EffortInvestmentCarouselWrapper from "./effortInvestmentCarousel";

const EffortInvestmentStatChart: React.FC<GraphStatChartProps> = (props: any) => {
  const { data, onClick, isDemo } = props;

  if (data && !Array.isArray(data)) {
    return <Empty description={"No Data"} image={Empty.PRESENTED_IMAGE_SIMPLE} />;
  }

  return (
    <div className="effort-investment-stat-wrapper">
      <EffortInvestmentCarouselWrapper className="carousel-Wrapper stat-chart-wrapper" data={data} refresh={0}>
        {(data || []).map((item: any) => {
          return (
            <div style={{ margin: "50px 20px" }} key={item?.id}>
              <GraphStatChart
                categoryName={item?.name}
                statTrend={item?.statTrend}
                unit="fte"
                stat={item?.stat}
                onClick={onClick}
                idealRange={item?.ideal_range}
                isDemo={isDemo || false}
              />
            </div>
          );
        })}
      </EffortInvestmentCarouselWrapper>
    </div>
  );
};

export default React.memo(EffortInvestmentStatChart);
