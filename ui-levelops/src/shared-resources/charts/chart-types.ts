import { RestWorkflowProfile } from "classes/RestWorkflowProfile";
import { ProjectPathProps } from "classes/routeInterface";
import { aligmentDataType } from "dashboard/dashboard-types/BAReports.types";
import { basicMappingType, chartPropsType } from "dashboard/dashboard-types/common-types";
import { CSSProperties } from "react";
import { LegendProps } from "recharts";

export type SingleAreaProps = {
  dataKey: string;
  theme: keyof ChartThemeOptions;
  unit?: string;
  transformer?: Function;
};

export type AreaChartProps = {
  customColors?: string[];
  id: string;
  unit?: string;
  data: Array<Object>;
  areaProps: Array<SingleAreaProps>;
  hasClickEvents?: boolean;
  onClick?: (x: any) => void;
  stackedArea?: boolean;
  chartProps: any;
  fillOpacity?: number;
  showGrid?: boolean;
  showDots?: boolean;
  legendType?: string;
  areaType?: string;
  config?: ChartConfigType;
  reportType?: string;
  previewOnly?: boolean;
  hideLegend?: boolean;
  hideKeys?: string[];
  dataKey?: string;
  showTotalOnTooltip?: boolean;
  xUnit?: string;
  hasTrendLikeData?: boolean;
  colorSchema?: Record<string, string>;
  hygieneMapping?: { [key: string]: string };
  xAxisProps?: any;
};

export type RadarChartProps = {
  data: Array<Object>;
  radarProps: Array<SingleAreaProps>;
  hasClickEvents?: boolean;
  onClick?: (x: string) => void;
  config?: ChartConfigType;
};

export type ScoreChartProps = {
  id: string;
  score: number;
  breakdown: Array<Object>;
  data: Array<Object>;
  hasClickEvents?: boolean;
  onClick?: (x: any) => void;
  chartProps?: {
    barGap?: number;
    margin?: {
      top: number;
      bottom: number;
      right: number;
      left: number;
    };
  };
  config?: ChartConfigType;
  previewOnly?: boolean;
  hideScore?: boolean;
  width: number;
  hideLegend?: boolean;
};

export type StatsChartProps = {
  id?: string;
  stat: number;
  statTrend: number;
  unit: string;
  hasClickEvents?: boolean;
  onClick?: (x: string) => void;
  config?: ChartConfigType;
  reportType?: string;
  idealRange?: any;
  metric?: string;
  showRoundedValue?: boolean;
  precision?: number;
  band?: string;
  count?: number;
  unitSymbol?: string;
  simplifyValue?: boolean;
};

export type ChartConfigType = {
  disable_tooltip?: boolean;
  showXAxisTooltip?: boolean;
};

export type ScatterChartProps = {
  data: Array<Object>;
  yDataKey: string;
  unit: string;
  xUnit: string;
  rangeY: Array<string>;
  hasClickEvents?: boolean;
  onClick?: (x: any) => void;
  id?: string;
  config?: ChartConfigType;
  previewOnly?: boolean;
  hasTrendLikeData?: boolean;
  reportType?: string;
  isDemo?: boolean;
};

export type SingleBarProps = {
  name: string;
  dataKey: string;
  fill: string;
  unit: string;
  xUnit: string;
  stackId?: string;
};

export type BarChartProps = {
  id?: string;
  data: Array<Object>;
  barProps: Array<SingleBarProps>;
  orderedBarProps?: any[];
  isDemo?: boolean;
  barPropsMap?: { [key: string]: any };
  useOrderedStacks?: boolean;
  legendPosition?: "bottom" | "middle" | "top";
  stacked: boolean;
  showDefaultLegend?: boolean;
  unit?: string;
  units?: Array<string>;
  xUnit?: string;
  hasClickEvents?: boolean;
  onClick?: (x: any, filters?: string[]) => void;
  widgetName?: any;
  reportType?: string;
  tooltipTitle?: string;
  dataMin?: number;
  dataMax?: number;
  hideTotalInTooltip?: boolean;
  showNegativeValuesAsPositive?: boolean;
  customColors?: string[];
  hideGrid?: boolean;
  legendProps?: LegendProps;
  chartProps?: {
    barGap?: number;
    margin?: {
      top: number;
      bottom: number;
      right: number;
      left: number;
    };
    className?: string;
  };
  transformFn?: (data: any) => void;
  totalCountTransformFn?: (data: any, meta_data?: any) => any;
  config?: ChartConfigType;
  previewOnly?: boolean;
  hideLegend?: boolean;
  digitsAfterDecimal?: number;
  hasTrendLikeData?: boolean;
  colorSchema?: Record<string, string>;
  xAxisProps?: any;
  hygieneMapping?: { [key: string]: string };
  bypassTitleTransform?: boolean;
  widgetMetaData?: any;
  displayValueOnBar?: boolean;
  display_colors?: { [x: string]: string };
  trendLineData?: Record<string, string>;
  barTopValueFormater?: (value: number | string) => number | string;
  baseLinesDataPoints?: number[];
  readOnlyLegend?: boolean;
  showValueOnBarStacks?: boolean;
  baseLineMap?: any;
  trendLineKey?: any;
  apiData?: { [key: string]: any };
};

export interface PropeloTableChartProps<T extends Object = Object> extends BarChartProps {
  data: Array<T>;
  tableId: string;
  columns?: string[];
  tableFilters?: Record<string, any>;
  validOUIds?: string[];
  showOUSpecificData?: boolean;
}

export type EffortInvestmentBarChartProps = {
  id: string;
  data: Array<Object>;
  barProps: Array<SingleBarProps>;
  unit?: string;
  units?: Array<string>;
  xUnit?: string;
  stacked?: any;
  hasClickEvents?: boolean;
  onClick?: (x: any) => void;
  reportType?: string;
  tooltipTitle?: string;
  hideTotalInTooltip?: boolean;
  hideGrid?: boolean;
  chartProps?: {
    barGap?: number;
    margin?: {
      top: number;
      bottom: number;
      right: number;
      left: number;
    };
    className?: string;
  };
  showNegativeValuesAsPositive?: boolean;
  config?: ChartConfigType;
  previewOnly?: boolean;
  hideLegend?: boolean;
  filters: any;
  colorsData: any;
  activeKey?: any;
  setActiveKey?: any;
};

export type CircleChartProps = BarChartProps;

export type LineChartProps = {
  id: string;
  data: Array<Object>;
  xUnit: string;
  unit: string;
  units?: Array<string>;
  hasClickEvents?: boolean;
  onClick?: (x: any) => void;
  reportType?: string;
  tooltipTitle?: string;
  chartProps?: {
    barGap?: number;
    margin?: {
      top: number;
      bottom: number;
      right: number;
      left: number;
    };
  };
  showTotalOnTooltip?: boolean;
  lineProps?: Array<SingleBarProps>;
  config?: ChartConfigType;
  hideLegend?: boolean;
  showDefaultLegend?: boolean;
  hasTrendLikeData?: boolean;
  colorSchema?: Record<string, string>;
  alwaysShowDot?: boolean;
  showYaxis?: boolean;
  percentIncludeFn?: any;
  all_data?: any;
  totalCountTransformFn?: any;
};

export type DonutDataItem = {
  name: string;
  value: number;
  percentage?: number;
};

export type DonutChartProps = {
  data: Array<DonutDataItem>;
  pieProps?: Object;
  labelTitle?: string;
  showLegend?: boolean;
  hasClickEvents?: boolean;
  onClick?: (x: string) => void;
  config?: ChartConfigType;
  height?: number;
  width?: number;
  unit?: string;
  colorSchema?: Record<string, string>;
  reportType?: String;
  xUnit?: string;
};

export type EffortInvestmentDonutChartProps = {
  data: Array<DonutDataItem>;
  pieProps?: Object;
  labelTitle?: string;
  showLegend?: boolean;
  hasClickEvents?: boolean;
  onClick?: (x: string) => void;
  config?: ChartConfigType;
  height?: number;
  width?: number;
  unit?: string;
  showTooltip: boolean;
  colorsData?: any;
  activeKey: any;
  setActiveKey: any;
  donutUnit?: string;
  averageEngineers?: number;
};

export type DonutLabelProps = {
  bulletColor: string;
  key: string;
};

export type ChartThemeProps = {
  fill: string;
  stroke: string;
};

export type ChartThemeOptions = {
  blue: ChartThemeProps;
  red: ChartThemeProps;
  yellow: ChartThemeProps;
};

export type TableChartProps = {
  data: Array<Object>;
  columns: Array<Object>;
  size: "small" | "middle" | "default";
  sort_value: string;
  hasClickEvents?: boolean;
  reportType: string;
  onClick: (data: any) => void;
  xUnit: string;
  chartProps: any;
  config?: ChartConfigType;
  previewOnly?: boolean;
  id?: string;
  metaData?: any;
  otherKeyData?: any;
};

export type TreeMapChartProps = {
  id: string;
  data: Array<any>;
  dataKey: string;
  total?: number;
  hasClickEvents?: boolean;
  onClick?: (x: string) => void;
  reportType?: string;
  config?: ChartConfigType;
  previewOnly?: boolean;
};

export type GridViewChartProps = {
  id: string;
  data: Array<any>;
  dataKey: string;
  height: number;
  width: number;
  total?: number;
  hasClickEvents?: boolean;
  onClick?: (x: any) => void;
  defaultPath?: string;
  reportType?: string;
  config?: ChartConfigType;
  previewOnly?: boolean;
};

export type SankeyChartProps = {
  id: string;
  data: Array<any>;
  dataKey: string;
  nodePadding?: number;
  nodeWidth?: number;
  linkWidth?: number;
  linkCurvature?: number;
  iterations?: number;
  node?: any;
  link?: any;
  margin?: CSSProperties;
  onClick: (data: any) => void;
  config?: ChartConfigType;
  previewOnly?: boolean;
};

export type BubbleChartProps = {
  id: string;
  data: Array<Object>;
  bubbleProps: Array<SingleBarProps>;
  stacked: boolean;
  yunit?: string;
  xunit?: string;
  zunit?: string;
  xunitLabel?: string;
  yunitLabel?: string;
  units?: Array<string>;
  xUnit?: string;
  hasClickEvents?: boolean;
  onClick?: (x: any) => void;
  widgetName?: any;
  reportType?: string;
  chartProps?: {
    barGap?: number;
    margin?: {
      top: number;
      bottom: number;
      right: number;
      left: number;
    };
  };
  transformFn: (data: any) => void;
  config?: ChartConfigType;
  hideLegend?: boolean;
};

type jiraProgressDataType = {
  key: string;
  story_points: number;
  unestimated_tickets: number;
  team_allocation: { name: string }[];
  priority: string;
  completed_percent_story_point?: number;
};
export type JiraProgressChartProps = {
  data: jiraProgressDataType[];
  across: string;
  reportType?: string;
  onClick: (x: any) => void;
  headerData?: any[];
  widgetId: string;
};

export type JiraEffortAllocationChartProps = {
  data: BarChartProps & ChartConfigType & { records: any[] } & { donutData: any[] } & any;
  chartProps: chartPropsType;
  stacked: boolean;
  reportType?: string;
  transformFn?: (data: any) => any;
  totalCountTransformFn?: (data: any) => any;
  onClick: (x: any) => void;
  hideLegend?: boolean;
  previewOnly?: boolean;
};

export type JiraPriorityChartProps = {
  data: any[];
  reportType?: string;
  onClick: (x: any) => void;
};

export type JiraBurndownChartProps = {
  data: any;
  reportType?: string;
  onClick: (x: any) => void;
};

export type EffortInvestmentTeamChartProps = {
  data: any;
  reportType?: string;
  unit?: string;
  onClick: (x: any) => void;
};

export type GraphStatChartProps = {
  unit: string;
  stat: number | "NaN";
  statTrend?: number;
  reportType?: string; // will remove after testing
  onClick: (x: any) => void;
  hasClickEvents?: boolean;
  categoryName: string;
  idealRange?: { min: number; max: number };
  isDemo?: boolean;
};

export type PhaseChartProps = {
  id: string;
  data: Array<Object>;
  dataKey: string;
  height: number;
  width: number;
  total?: number;
  unit?: string;
  defaultPath?: string;
  reportType?: string;
  config?: ChartConfigType;
  onClick?: (x: any) => void;
  hasClickEvents?: boolean;
  hideLegend?: boolean;
  hideKeys?: string[];
  showStaticLegends?: boolean;
  widgetMetaData?: basicMappingType<string>;
  isSummaryNeeded?: boolean;
  onDoraClick?: (data: any, filters?: any) => void;
  isDemo?: boolean;
};

export type DoraLeadTimeStageChartProps = {
  id: string;
  data: Array<Object>;
  onDoraClick?: (data: any, filters?: any) => void;
  dataKey: string;
  widgetMetaData?: basicMappingType<string>;
  metrics: string;
  dateRangeValue?: { $gt: number; $lt: number };
  workflowProfile?: RestWorkflowProfile;
  activePhase?: string;
};

export type LeadTimeTypeChartProps = {
  id: string;
  data: Array<Object>;
  dataKey: string;
  height: number;
  width: number;
  total?: number;
  unit?: string;
  reportType?: string;
  config?: ChartConfigType;
  onClick?: (x: any) => void;
  hasClickEvents?: boolean;
  hideLegend?: boolean;
  hideKeys?: string[];
};

export type chartTooltipListitemType = {
  label?: string;
  value?: any;
  color?: string;
};

export type ScmReviewSankeyChartProps = {
  data: {
    apiData: any[];
    submittersList: any[];
    reviewersList: any[];
    totalNotReviewed: number;
    totalReviewed: number;
    totalBreakdowndata: basicMappingType<number>;
  };
  onClick?: (x: any) => void;
  id: string;
};

export type EngineerTableProps = {
  data: any;
  widgetId: string;
  onClick?: (x: any) => void;
  reportType?: string;
};

export type AlignmentTableProps = {
  data: aligmentDataType;
  widgetId: string;
  profileId: string;
  onClick?: (x: any) => void;
  reload: () => void;
};

export type DevProductivityTableChartProps = {
  data: Array<Object>;
  columns: Array<Object>;
  size: "small" | "middle" | "default";
  sort_value: string;
  hasClickEvents?: boolean;
  reportType: string;
  onClick: (data: any) => void;
  xUnit: string;
  chartProps: any;
  id: string;
  apisMetaData: any;
  config?: ChartConfigType;
  previewOnly?: boolean;
  modifiedColumnInfo?: any;
  showNameButton?: boolean;
  onRowClick?: (
    params: ProjectPathProps,
    record: any,
    index: number,
    event: Event,
    interval?: string,
    ou_id?: Array<string>,
    locationPathName?: string
  ) => any;
  onRowDemoTableClick?: (
    id: any,
    record: any,
    dashboardId: string,
    index: number,
    event: Event,
    interval?: string,
    ou_id?: Array<string>
  ) => any;
  interval?: string;
  ou_id?: Array<string>;
  ou_uuid?: Array<string>;
  isDemo?: boolean;
  dashboardId?: string;
};

export type ActivityTableProps = {
  data: any; // TODO: update with actual shape of response
  widgetId: string;
};

export type LineGraphDataSourceType = {
  key: string;
  value: string;
  additional_key: string;
};

export type LineChartDrillDownProps = {
  data: Array<LineGraphDataSourceType>;
  chartProps?: {
    barGap?: number;
    margin?: {
      top: number;
      bottom: number;
      right: number;
      left: number;
    };
  };
  lines: Array<string>;
  lineType?: string;
  xLabelValue: string;
  xAxisDataKey: string;
  yLabelValue: string;
  customizedDotLabel?: boolean;
  onClick?: (data: any) => void;
  additionalKey?: string;
};
