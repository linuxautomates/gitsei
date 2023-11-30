import {
  AZURE_LEAD_TIME_ISSUE_REPORT,
  DEV_PRODUCTIVITY_REPORTS,
  DORA_REPORTS,
  ISSUE_MANAGEMENT_REPORTS,
  JIRA_MANAGEMENT_TICKET_REPORT,
  LEAD_MTTR_DORA_REPORTS,
  LEAD_TIME_REPORTS
} from "dashboard/constants/applications/names";
import { get } from "lodash";
import React, { useContext, useEffect, useMemo, useState } from "react";
import { ResponsiveContainer } from "recharts";
import AlignmentTable from "shared-resources/charts/alignment-table/AlignmentTable";
import AzureResolutionChartComponent from "shared-resources/charts/composite-chart/azure-resolution-time-chart.component";
import MultiTimeSeriesChartComponent from "shared-resources/charts/composite-chart/multiTimeSeriesChart.component";
import EffortInvestmentTeamChartContainer from "shared-resources/charts/effort-investment-team-chart/EffortInvestmentTeamChartContainer";
import EngineerTable from "shared-resources/charts/engineer-table/EngineerTable";
import JiraBurndownChartContainer from "shared-resources/charts/jira-burndown/JiraBurndownChartContainer";
import ScmReviewSankeyChartComponent from "shared-resources/charts/scm-review-sankey-chart/scm-review-sankey-chart.component";
import { CacheWidgetPreview, DashboardColorSchemaContext } from "../../../dashboard/pages/context";
import {
  AreaChart,
  AzureEffortAllocationChart,
  BarChart,
  BubbleChart,
  CircleChart,
  CompositeChart,
  DonutChart,
  EffortInvestmentStat,
  GraphStatChart,
  GridViewChart,
  JiraEffortAllocationChart,
  JiraPriorityChart,
  JiraProgressChart,
  LineChart,
  PhaseChart,
  RadarChart,
  SankeyChart,
  ScatterChart,
  ScoreChart,
  StatsChart,
  TableChart,
  TreeMapChart,
  TypeChart,
  StageBounceChart,
  HygieneAreaChart,
  HygieneBarChart,
  PhaseChartIssues
} from "../../charts";
import {
  AlignmentTableProps,
  AreaChartProps,
  BarChartProps,
  BubbleChartProps,
  CircleChartProps,
  DonutChartProps,
  EffortInvestmentTeamChartProps,
  EngineerTableProps,
  GraphStatChartProps,
  GridViewChartProps,
  JiraBurndownChartProps,
  JiraEffortAllocationChartProps,
  JiraPriorityChartProps,
  JiraProgressChartProps,
  LeadTimeTypeChartProps,
  PropeloTableChartProps,
  LineChartProps,
  PhaseChartProps,
  RadarChartProps,
  SankeyChartProps,
  ScatterChartProps,
  ScmReviewSankeyChartProps,
  ScoreChartProps,
  StatsChartProps,
  TableChartProps,
  TreeMapChartProps
} from "../../charts/chart-types";
import "./chart-container.styles.scss";
import { jiraBAReportTypes } from "../../../dashboard/constants/enums/jira-ba-reports.enum";
import DevProdTableChartComponent from "shared-resources/charts/dev-productivity/table-chart.component";
import ActivityTable from "shared-resources/charts/activity-table/ActivityTable";
import FilterablePaginatedTable, {
  FilterablePaginatedTableProps
} from "../filterablePaginatedTable/FilterablePaginatedTable";
import { azureBAReportTypes } from "dashboard/constants/enums/azure-ba-reports.enum";
import DoraChart from "shared-resources/charts/dora-chart/DoraChart";
import DoraCombinedStatStageChart, {
  DoraCombinedStatStageChartProps
} from "shared-resources/charts/dora-chart/dora-stage-stat-chart/DoraStatStageChartWrapper";
import TableChartComponent from "shared-resources/charts/propelo-table-chart/TableChartComponent";
import CompositeBarChart from "shared-resources/charts/composite-chart/CompositeBarChart";
import { ChartType } from "./ChartType";

type ChartContainerProps =
  | {
      chartType: ChartType.LINE;
      chartProps: LineChartProps;
    }
  | {
      chartType: ChartType.RADAR;
      chartProps: RadarChartProps;
    }
  | {
      chartType: ChartType.BAR;
      chartProps: BarChartProps;
    }
  | {
      chartType: ChartType.CIRCLE;
      chartProps: CircleChartProps;
    }
  | {
      chartType: ChartType.DONUT;
      chartProps: DonutChartProps;
    }
  | {
      chartType: ChartType.AREA;
      chartProps: AreaChartProps;
    }
  | {
      chartType: ChartType.SCATTER;
      chartProps: ScatterChartProps;
    }
  | {
      chartType: ChartType.SCORE;
      chartProps: ScoreChartProps;
    }
  | {
      chartType: ChartType.STATS;
      chartProps: StatsChartProps;
    }
  | {
      chartType: ChartType.COMPOSITE;
      chartProps: BarChartProps;
    }
  | {
      chartType: ChartType.TABLE;
      chartProps: TableChartProps;
    }
  | {
      chartType: ChartType.TREEMAP;
      chartProps: TreeMapChartProps;
    }
  | {
      chartType: ChartType.SANKEY;
      chartProps: SankeyChartProps;
    }
  | {
      chartType: ChartType.BUBBLE;
      chartProps: BubbleChartProps;
    }
  | {
      chartType: ChartType.GRID_VIEW;
      chartProps: TreeMapChartProps;
    }
  | {
      chartType: ChartType.JIRA_PROGRESS_CHART;
      chartProps: JiraProgressChartProps;
    }
  | {
      chartType: ChartType.GRAPH_STAT;
      chartProps: GraphStatChartProps;
    }
  | {
      chartType: ChartType.JIRA_EFFORT_ALLOCATION_CHART;
      chartProps: JiraEffortAllocationChartProps;
    }
  | {
      chartType: ChartType.JIRA_PRIORITY_CHART;
      chartProps: JiraPriorityChartProps;
    }
  | {
      chartType: ChartType.JIRA_BURNDOWN_CHART;
      chartProps: JiraBurndownChartProps;
    }
  | {
      chartType: ChartType.EFFORT_INVESTMENT_TEAM_CHART;
      chartProps: EffortInvestmentTeamChartProps;
    }
  | {
      chartType: ChartType.LEAD_TIME_PHASE;
      chartProps: PhaseChartProps;
    }
  | {
      chartType: ChartType.LEAD_TIME_TYPE;
      chartProps: LeadTimeTypeChartProps;
    }
  | {
      chartType: ChartType.AZURE_RESOLUTION_TIME;
      chartProps: BarChartProps;
    }
  | {
      chartType: ChartType.MULTI_TIME_SERIES;
      chartProps: BarChartProps;
    }
  | {
      chartType: ChartType.EFFORT_INVESTMENT_STAT;
      chartProps: any;
    }
  | {
      chartType: ChartType.REVIEW_SCM_SANKEY;
      chartProps: ScmReviewSankeyChartProps;
    }
  | {
      chartType: ChartType.AZURE_EFFORT_ALLOCATION_CHART;
      chartProps: JiraEffortAllocationChartProps;
    }
  | {
      chartType: ChartType.ENGINEER_TABLE;
      chartProps: EngineerTableProps;
    }
  | {
      chartType: ChartType.ALIGNMENT_TABLE;
      chartProps: AlignmentTableProps;
    }
  | {
      chartType: ChartType.STAGE_BOUNCE_CHART;
      chartProps: BarChartProps;
    }
  | {
      chartType: ChartType.HYGIENE_AREA_CHART;
      chartProps: AreaChartProps;
    }
  | {
      chartType: ChartType.HYGIENE_BAR_CHART;
      chartProps: BarChartProps;
    }
  | {
      chartType: ChartType.DEV_PROD_TABLE_CHART;
      chartProps: any;
    }
  | {
      chartType: ChartType.DEV_PROD_ACTIVE_TABLE_CHART;
      chartProps: any;
    }
  | {
      chartType: ChartType.FILTERABLE_RAW_STATS_TABLE;
      chartProps: FilterablePaginatedTableProps;
    }
  | {
      chartType: ChartType.DORA_COMBINED_BAR_CHART;
      chartProps: any;
    }
  | {
      chartType: ChartType.DORA_COMBINED_STAGE_CHART;
      chartProps: DoraCombinedStatStageChartProps;
    }
  | {
      chartType: ChartType.LEVELOPS_TABLE_CHART;
      chartProps: PropeloTableChartProps;
    }
  | {
      chartType: ChartType.COMPOSITE_TABLE_CHART;
      chartProps: BarChartProps;
    };

const ChartContainer: React.FC<ChartContainerProps> = (props: ChartContainerProps) => {
  const { chartType, chartProps } = props;
  const cacheWidgetPreview = useContext(CacheWidgetPreview);
  const { colorSchema } = useContext(DashboardColorSchemaContext);
  let isIssuesLeadTime = false;
  const reportType = get(chartProps, ["reportType"], undefined);
  if (
    reportType === LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT ||
    reportType === AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_Time_BY_STAGE_REPORT ||
    reportType === LEAD_MTTR_DORA_REPORTS.LEAD_TIME_FOR_CHANGE ||
    reportType === LEAD_MTTR_DORA_REPORTS.MEAN_TIME_TO_RESTORE
  )
    isIssuesLeadTime = true;

  const containerRef = React.createRef<HTMLDivElement>();
  const [containerHeight, setContainerHeight] = useState(0);

  useEffect(() => {
    if (containerRef && containerRef.current) {
      setContainerHeight(containerRef.current.clientHeight);
    }
  }, [containerRef]); // eslint-disable-line react-hooks/exhaustive-deps

  function renderChart() {
    if (cacheWidgetPreview) {
      (chartProps as any)["config"] = {
        disable_tooltip: true
      };
    }
    switch (chartType) {
      case ChartType.AREA:
        return <AreaChart {...(chartProps as AreaChartProps)} colorSchema={colorSchema} />;
      case ChartType.RADAR:
        return <RadarChart {...(chartProps as RadarChartProps)} />;
      case ChartType.BAR:
        return <BarChart {...(chartProps as BarChartProps)} colorSchema={colorSchema} />;
      case ChartType.CIRCLE:
        return <CircleChart {...(chartProps as CircleChartProps)} colorSchema={colorSchema} />;
      case ChartType.LINE:
        return <LineChart {...(chartProps as LineChartProps)} colorSchema={colorSchema} />;
      case ChartType.DONUT:
        return <DonutChart {...(chartProps as DonutChartProps)} colorSchema={colorSchema} />;
      case ChartType.SCATTER:
        return <ScatterChart {...(chartProps as ScatterChartProps)} />;
      case ChartType.SCORE:
        return <ScoreChart {...(chartProps as ScoreChartProps)} />;
      case ChartType.STATS:
        return <StatsChart {...(chartProps as StatsChartProps)} />;
      case ChartType.COMPOSITE:
        return <CompositeChart {...(chartProps as BarChartProps)} colorSchema={colorSchema} />;
      case ChartType.TABLE:
        return <TableChart {...(chartProps as TableChartProps)} />;
      case ChartType.TREEMAP:
        return <TreeMapChart {...(chartProps as TreeMapChartProps)} />;
      case ChartType.SANKEY:
        return <SankeyChart {...(chartProps as SankeyChartProps)} />;
      case ChartType.BUBBLE:
        return <BubbleChart {...(chartProps as BubbleChartProps)} />;
      case ChartType.GRID_VIEW:
        return <GridViewChart {...(chartProps as GridViewChartProps)} />;
      case ChartType.JIRA_PROGRESS_CHART:
        return <JiraProgressChart {...(chartProps as JiraProgressChartProps)} />;
      case ChartType.GRAPH_STAT:
        return <GraphStatChart {...(chartProps as GraphStatChartProps)} />;
      case ChartType.JIRA_EFFORT_ALLOCATION_CHART:
        return <JiraEffortAllocationChart {...(chartProps as JiraEffortAllocationChartProps)} />;
      case ChartType.AZURE_EFFORT_ALLOCATION_CHART:
        return <AzureEffortAllocationChart {...(chartProps as JiraEffortAllocationChartProps)} />;
      case ChartType.JIRA_PRIORITY_CHART:
        return <JiraPriorityChart {...(chartProps as JiraPriorityChartProps)} />;
      case ChartType.JIRA_BURNDOWN_CHART:
        return <JiraBurndownChartContainer {...(chartProps as JiraBurndownChartProps)} />;
      case ChartType.EFFORT_INVESTMENT_TEAM_CHART:
        return <EffortInvestmentTeamChartContainer {...(chartProps as EffortInvestmentTeamChartProps)} />;
      case ChartType.LEAD_TIME_PHASE: {
        if (isIssuesLeadTime) return <PhaseChartIssues {...(chartProps as PhaseChartProps)} />;
        else return <PhaseChart {...(chartProps as PhaseChartProps)} />;
      }

      case ChartType.LEAD_TIME_TYPE:
        return <TypeChart {...(chartProps as LeadTimeTypeChartProps)} />;
      case ChartType.AZURE_RESOLUTION_TIME:
        return <AzureResolutionChartComponent {...(chartProps as BarChartProps)} />;
      case ChartType.MULTI_TIME_SERIES:
        return <MultiTimeSeriesChartComponent {...(chartProps as BarChartProps)} />;
      case ChartType.EFFORT_INVESTMENT_STAT:
        return <EffortInvestmentStat {...(chartProps as any)} />;
      case ChartType.REVIEW_SCM_SANKEY:
        return <ScmReviewSankeyChartComponent {...(chartProps as ScmReviewSankeyChartProps)} />;
      case ChartType.ENGINEER_TABLE:
        return <EngineerTable {...(chartProps as EngineerTableProps)} />;
      case ChartType.ALIGNMENT_TABLE:
        return <AlignmentTable {...(chartProps as AlignmentTableProps)} />;
      //TODO : this is of no use , the case can be handled by Bar Chart
      case ChartType.STAGE_BOUNCE_CHART:
        return <StageBounceChart {...(chartProps as BarChartProps)} />;
      case ChartType.HYGIENE_AREA_CHART:
        return <HygieneAreaChart {...(chartProps as AreaChartProps)} />;
      case ChartType.HYGIENE_BAR_CHART:
        return <HygieneBarChart {...(chartProps as BarChartProps)} />;
      case ChartType.DEV_PROD_TABLE_CHART:
        return <DevProdTableChartComponent {...(chartProps as any)} />;
      case ChartType.DEV_PROD_ACTIVE_TABLE_CHART:
        return <ActivityTable {...(chartProps as any)} />;
      case ChartType.FILTERABLE_RAW_STATS_TABLE:
        return <FilterablePaginatedTable {...(chartProps as any)} isDemo={false} />;
      case ChartType.DORA_COMBINED_BAR_CHART:
        return <DoraChart {...(chartProps as any)} colorSchema={colorSchema} />;
      case ChartType.DORA_COMBINED_STAGE_CHART:
        return <DoraCombinedStatStageChart {...(chartProps as DoraCombinedStatStageChartProps)} />;
      case ChartType.LEVELOPS_TABLE_CHART:
        return <TableChartComponent {...(chartProps as PropeloTableChartProps)} />;
      case ChartType.COMPOSITE_TABLE_CHART:
        return <CompositeBarChart {...(chartProps as BarChartProps)} />;
    }
  }

  const height = useMemo(() => {
    const reportType = get(chartProps, ["reportType"], undefined);
    if (
      [
        LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_TYPE_REPORT,
        LEAD_TIME_REPORTS.JIRA_LEAD_TIME_BY_STAGE_REPORT,
        LEAD_TIME_REPORTS.SCM_PR_LEAD_TIME_BY_STAGE_REPORT,
        LEAD_TIME_REPORTS.LEAD_TIME_BY_TIME_SPENT_IN_STAGES_REPORT,
        LEAD_TIME_REPORTS.JIRA_LEAD_TIME_TREND_REPORT,
        AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_TIME_TREND_REPORT,
        AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_TIME_BY_TYPE_REPORT,
        AZURE_LEAD_TIME_ISSUE_REPORT.ISSUE_LEAD_Time_BY_STAGE_REPORT,
        jiraBAReportTypes.JIRA_EFFORT_INVESTMENT_ENGINEER_REPORT,
        ISSUE_MANAGEMENT_REPORTS.EFFORT_INVESTMENT_ENGINEER_REPORT,
        "effort_investment_single_stat",
        "azure_effort_investment_single_stat",
        "review_collaboration_report",
        DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_PR_ACTIVITY_REPORT,
        DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT,
        DEV_PRODUCTIVITY_REPORTS.INDIVIDUAL_RAW_STATS,
        DEV_PRODUCTIVITY_REPORTS.ORG_RAW_STATS,
        DEV_PRODUCTIVITY_REPORTS.DEV_PRODUCTIVITY_SCORE_REPORT,
        DEV_PRODUCTIVITY_REPORTS.PRODUCTIVITY_SCORE_BY_ORG_UNIT,
        JIRA_MANAGEMENT_TICKET_REPORT.PROGRESS_SINGLE_REPORT,
        JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT,
        azureBAReportTypes.AZURE_PROGRAM_PROGRESS_REPORT,
        ISSUE_MANAGEMENT_REPORTS.AZURE_ISSUES_PROGRESS_REPORT,
        DORA_REPORTS.LEADTIME_CHANGES,
        LEAD_MTTR_DORA_REPORTS.LEAD_TIME_FOR_CHANGE,
        LEAD_MTTR_DORA_REPORTS.MEAN_TIME_TO_RESTORE
      ].includes(reportType)
    ) {
      return "100%";
    }
    return containerHeight;
  }, [containerHeight, chartProps]);

  return (
    <div ref={containerRef} className="chart-container-parent">
      <ResponsiveContainer height={height} className="chart-container-component">
        {renderChart()}
      </ResponsiveContainer>
    </div>
  );
};

export default ChartContainer;
