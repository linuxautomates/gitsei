import React from "react";
import BarChartComponent from "shared-resources/charts/bar-chart/bar-chart.component";
import { JiraEffortAllocationChartProps } from "shared-resources/charts/chart-types";

const AzureEffortAlloactionChartContainer: React.FC<JiraEffortAllocationChartProps> = props => {
  const {
          onClick,
          data: chartData,
          chartProps,
          stacked,
          hideLegend,
          previewOnly,
          reportType,
          transformFn,
          totalCountTransformFn
        } = props;
  const { records, barProps, unit, showXAxisTooltip, digitsAfterDecimal } = chartData;

  return (
    <BarChartComponent
      id="effort_investment-bar-chart"
      data={records}
      barProps={barProps}
      chartProps={chartProps}
      stacked={stacked}
      unit={unit}
      hideTotalInTooltip={true}
      hideLegend={hideLegend}
      onClick={onClick}
      hasClickEvents={true}
      previewOnly={previewOnly}
      config={{ showXAxisTooltip }}
      transformFn={transformFn}
      totalCountTransformFn={totalCountTransformFn}
      reportType={reportType}
      digitsAfterDecimal={digitsAfterDecimal}
    />
  );
};

export default AzureEffortAlloactionChartContainer;