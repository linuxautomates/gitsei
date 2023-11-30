import { Empty } from "antd";
import {
  AZURE_LEAD_TIME_ISSUE_REPORT,
  DEV_PRODUCTIVITY_REPORTS,
  LEAD_MTTR_DORA_REPORTS,
  LEAD_TIME_REPORTS
} from "dashboard/constants/applications/names";
import { get } from "lodash";
import React, { useEffect, useMemo, useState } from "react";
import { ResponsiveContainer } from "recharts";
import { CompositeChart, ScatterChart, ScoreChart } from "shared-resources/charts";
import { GraphStatChartProps, ScoreChartProps } from "shared-resources/charts/chart-types";
import { EffortInvestmentStat } from "shared-resources/charts";
import DemoEffortTrendChartContainer from "../Widgets-Graph/Demo-Effort-Trend-Chart/demo-effort-trend-chart.container";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import DemoBarChartComponent from "../Widgets-Graph/demo-bar-chart.component";
import DemoCircleChartComponent from "../Widgets-Graph/demo-circle-chart";
import DemoEngineerTable from "../Widgets-Graph/Demo-Engineer-Table/demo-engineer-table.chart";
import DemoGridViewChart from "../Widgets-Graph/demo-grid-chart.component";
import DemoScmReviewSankeyChartComponent from "../Widgets-Graph/Demo-SCM-Sanky/demo-scm-sanky-chart.component";
import DemoStatsChartComponent from "../Widgets-Graph/demo-stat-graph.component";
import DemoLeadTimePhaseChart from "../Widgets-Graph/Lead-Time-Phase-Chart/demo-lead-time-phase-chart";
import { DemoBarChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-bar-chart.types";
import { DemoCircleChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-circle-chart-props";
import { DemoEngineerTableProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-engineer-table.types";
import { DemoGridViewChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-grid-view-chart.types";
import { DemoLeadTimePhaseChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-lead-time-phase-chart.types";
import { DemoScatterChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-scatter-chart.types";
import { DemoEffortTrendChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-effort-trend.types";
import { DemoEffortProgressChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-effort-progress.types";
import DemoEffortProgressChart from "../Widgets-Graph/Demo-Effort-Progress/demo-effort-progress.chart";
import { DemoPRActivityChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-pr-activity-chart.types";
import { DemoScmSankyChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-scm-sanky.types";
import { DemoStatChartProps } from "../Widgets-Graph/Widget-Grapg-Types/demo-stat-chart.types";
import DemoPRActivityComponent from "dashboard/pages/dashboard-view/Demo-Dashboard/Widgets-Graph/PR-Activity/DemoPRActivityComponent";
import DevProdTableChartComponent from "shared-resources/charts/dev-productivity/table-chart.component";
import FilterablePaginatedTable from "shared-resources/containers/filterablePaginatedTable/FilterablePaginatedTable";
import DemoCombinedBarChart from "../Widgets-Graph/DemoCombinedBarChart";

type DemoChartContainerProps =
  | {
      chartType: ChartType.BAR;
      chartProps: DemoBarChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.CIRCLE;
      chartProps: DemoCircleChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.STATS;
      chartProps: DemoStatChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.GRID_VIEW;
      chartProps: DemoGridViewChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.LEAD_TIME_PHASE;
      chartProps: DemoLeadTimePhaseChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.REVIEW_SCM_SANKEY;
      chartProps: DemoScmSankyChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.DEV_PROD_ACTIVE_TABLE_CHART;
      chartProps: DemoScmSankyChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.DEV_PROD_ACTIVE_TABLE_CHART;
      chartProps: DemoScmSankyChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.SCORE;
      chartProps: ScoreChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.EFFORT_INVESTMENT_STAT;
      chartProps: GraphStatChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.JIRA_EFFORT_ALLOCATION_CHART;
      chartProps: DemoEffortTrendChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.JIRA_PROGRESS_CHART;
      chartProps: DemoEffortProgressChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.ENGINEER_TABLE;
      chartProps: DemoEngineerTableProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.COMPOSITE;
      chartProps: DemoBarChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.SCATTER;
      chartProps: DemoScatterChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.SCORE;
      chartProps: ScoreChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.REVIEW_SCM_SANKEY;
      chartProps: DemoScmSankyChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.DEV_PROD_ACTIVE_TABLE_CHART;
      chartProps: DemoScmSankyChartProps;
      widgetType: string;
    }
  | {
      chartType: ChartType.DEV_PROD_TABLE_CHART;
      chartProps: any;
      widgetType: string;
    }
  | {
      chartType: ChartType.FILTERABLE_RAW_STATS_TABLE;
      chartProps: any;
      widgetType: string;
    }
  | {
      chartType: ChartType.DORA_COMBINED_BAR_CHART;
      chartProps: any;
      widgetType: string;
    };

const DemoChartContainer: React.FC<DemoChartContainerProps> = (props: DemoChartContainerProps) => {
  const { chartType, chartProps, widgetType } = props;
  const containerRef = React.createRef<HTMLDivElement>();

  const [containerHeight, setContainerHeight] = useState(0);

  useEffect(() => {
    if (containerRef && containerRef.current) {
      setContainerHeight(containerRef.current.clientHeight);
    }
  }, [containerRef]); // eslint-disable-line react-hooks/exhaustive-deps

  function renderChart() {
    switch (chartType) {
      case ChartType.CIRCLE:
        return <DemoCircleChartComponent {...(chartProps as DemoCircleChartProps)} />;
      case ChartType.BAR:
        return <DemoBarChartComponent {...(chartProps as DemoBarChartProps)} />;
      case ChartType.STATS:
        return <DemoStatsChartComponent {...(chartProps as DemoStatChartProps)} />;
      case ChartType.GRID_VIEW:
        return <DemoGridViewChart {...(chartProps as DemoGridViewChartProps)} />;
      case ChartType.REVIEW_SCM_SANKEY:
        return <DemoScmReviewSankeyChartComponent {...(chartProps as DemoScmSankyChartProps)} />;
      case ChartType.LEAD_TIME_PHASE:
        // @ts-ignore
        if (["dora_lead_time_for_change", "dora_mean_time_to_restore"].includes(chartProps.type)) {
          return <DemoCombinedBarChart {...(chartProps as any)} chartType={chartType} />;
        }
        return <DemoLeadTimePhaseChart {...(chartProps as DemoLeadTimePhaseChartProps)} />;
      case ChartType.COMPOSITE:
        return <CompositeChart {...(chartProps as DemoBarChartProps)} />;
      case ChartType.SCATTER:
        return <ScatterChart {...(chartProps as DemoScatterChartProps)} isDemo={true} />;
      case ChartType.SCORE:
        return <ScoreChart {...(chartProps as ScoreChartProps)} />;
      case ChartType.EFFORT_INVESTMENT_STAT:
        return <EffortInvestmentStat {...(chartProps as GraphStatChartProps)} />;
      case ChartType.JIRA_EFFORT_ALLOCATION_CHART:
        return <DemoEffortTrendChartContainer {...(chartProps as DemoEffortTrendChartProps)} />;
      case ChartType.JIRA_PROGRESS_CHART:
        return <DemoEffortProgressChart {...(chartProps as DemoEffortProgressChartProps)} />;
      case ChartType.ENGINEER_TABLE:
        return <DemoEngineerTable {...(chartProps as DemoEngineerTableProps)} />;
      case ChartType.DEV_PROD_ACTIVE_TABLE_CHART:
        return <DemoPRActivityComponent {...(chartProps as DemoPRActivityChartProps)} />;
      case ChartType.DEV_PROD_TABLE_CHART:
        return <DevProdTableChartComponent {...(chartProps as any)} />;
      case ChartType.FILTERABLE_RAW_STATS_TABLE:
        return <FilterablePaginatedTable {...(chartProps as any)} isDemo={true} />;
      case ChartType.DORA_COMBINED_BAR_CHART:
        return <DemoCombinedBarChart {...(chartProps as any)} chartType={chartType} />;
      default:
        return <Empty imageStyle={{ height: "80px" }} />;
    }
  }

  const height = useMemo(() => {
    const reportType = get(chartProps, ["reportType"], undefined);
    if (
      [
        LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
        LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT,
        AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_Time_BY_STAGE_REPORT,
        "review_collaboration_report",
        "effort_investment_single_stat",
        "progress_single_report",
        "jira_effort_investment_engineer_report",
        DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT,
        "individual_raw_stats_report",
        "dev_productivity_score_report",
        "org_raw_stats_report",
        "dev_productivity_org_unit_score_report",
        LEAD_MTTR_DORA_REPORTS.LEAD_TIME_FOR_CHANGE,
        LEAD_MTTR_DORA_REPORTS.MEAN_TIME_TO_RESTORE
      ].includes(reportType)
    ) {
      return "100%";
    }
    return widgetType === "stats" ? "100px" : "350px";
  }, [containerHeight, chartProps, widgetType]);

  return (
    <div ref={containerRef} className="chart-container-parent">
      <ResponsiveContainer height={height} className="chart-container-component">
        {renderChart()}
      </ResponsiveContainer>
    </div>
  );
};

export default DemoChartContainer;
