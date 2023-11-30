import widgetConstants from "dashboard/constants/widgetConstants";
import { get, isArray } from "lodash";
import React, { useEffect, useMemo, useRef, useState } from "react";
import { effortInvestmentDonutBarColors } from "shared-resources/charts/chart-themes";
import { JiraEffortAllocationChartProps, SingleBarProps } from "shared-resources/charts/chart-types";
import { getFilteredData, getInitialFilters } from "shared-resources/charts/helper";
import { EmptyWidget } from "shared-resources/components";
import NewBarChartComponent from "../components/effort-investment-bar-chart/effortInvestmentBarChartComponent";
import NewLegendComponent from "../components/LegendComponent/EffortInvestmentLegend";
import { effortInvestmentIgnoreKeys, uncategorizedCategoryKeyForColorMapping } from "../constant";
import "./jiraEffortAllocationChart.scss";
import { basicMappingType } from "dashboard/dashboard-types/common-types";

const JiraEffortAlloactionChartContainer: React.FC<JiraEffortAllocationChartProps> = props => {
  const { onClick, data: chartData, chartProps, stacked, hideLegend, previewOnly, reportType } = props;
  const { records, barProps, unit, showXAxisTooltip, categoryColorMapping } = chartData || {};
  const [filters, setFilters] = useState<any>({});
  const [barFilteredData, setBarFilteredData] = useState<any>([]);
  const [activeCategoryKey, setActiveCategoryKey] = useState<any>(undefined);
  const effortInvestmentTrendChartRef = useRef<any>(undefined);

  useEffect(() => {
    if ((records || []).length) {
      setBarFilteredData(records as any);
      const defaultFilterKey = get(widgetConstants, [props.reportType || "", "defaultFilterKey"], "");
      const initialFilters = getInitialFilters(records, effortInvestmentIgnoreKeys, defaultFilterKey);
      setFilters(initialFilters as any);
    }
  }, [records]);

  useEffect(() => {
    if (filters && records && isArray(records)) {
      const newBarData = getFilteredData(records || [], filters);
      setBarFilteredData(newBarData);
    }
  }, [filters]);

  const colorsData = useMemo(() => {
    const length = Object.keys(filters || {}).length;
    return Object.keys(filters).reduce((acc: basicMappingType<string>, next: string, index: number) => {
      let colorFetchCategoryKey = next;
      if (colorFetchCategoryKey === uncategorizedCategoryKeyForColorMapping) {
        colorFetchCategoryKey = "Other";
      }
      return {
        ...acc,
        [next]:
          categoryColorMapping[colorFetchCategoryKey ?? index + ""] || effortInvestmentDonutBarColors[index % length]
      };
    }, {} as basicMappingType<string>);
  }, [filters, categoryColorMapping]);

  // helps to get dynamic width of effort investment chart
  const getEffortAllocationChartWidth = (totalBars: number, barSize: number) => {
    let finalWidthPercentage = "100%";
    if (totalBars <= 1) return finalWidthPercentage;

    const paddingOffset = 6.0,
      widthAdjustFactor = 170.0;

    const width = totalBars * (barSize + paddingOffset) + widthAdjustFactor;
    const effortInvestmentBarWrapper: HTMLElement | undefined = effortInvestmentTrendChartRef.current;

    if (!!effortInvestmentBarWrapper) {
      const wrapperDivWidth = (effortInvestmentBarWrapper.offsetWidth || 0) * 1.0;

      if (wrapperDivWidth) {
        finalWidthPercentage = `${Math.min(100, Math.round((width * 100) / wrapperDivWidth))}%`;
      }
    }

    return finalWidthPercentage;
  };

  if ((barFilteredData || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <div className="combined-donut-bar-wrapper">
      <div ref={effortInvestmentTrendChartRef} className="donut-bar-wrapper">
        <div
          className="bar-wrapper"
          style={{
            width: getEffortAllocationChartWidth(
              (barFilteredData ?? []).length,
              barProps?.length ? barProps[0]?.barSize : 0
            )
          }}>
          <NewBarChartComponent
            id="effort_investment-bar-chart"
            data={barFilteredData}
            barProps={barProps}
            filters={filters}
            chartProps={chartProps}
            stacked={stacked}
            unit={unit}
            hideTotalInTooltip={true}
            hideLegend={hideLegend}
            onClick={onClick}
            hasClickEvents={true}
            previewOnly={previewOnly}
            config={{ showXAxisTooltip }}
            reportType={reportType}
            colorsData={colorsData}
            activeCategoryKey={activeCategoryKey}
            setActiveCategoryKey={setActiveCategoryKey}
          />
        </div>
      </div>
      <NewLegendComponent setFilters={setFilters} filters={filters} colors={colorsData} data={records} />
    </div>
  );
};

export default JiraEffortAlloactionChartContainer;
