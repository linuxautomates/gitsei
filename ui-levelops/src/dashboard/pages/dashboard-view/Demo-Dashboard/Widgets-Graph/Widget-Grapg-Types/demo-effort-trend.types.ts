import { chartPropsType } from "dashboard/dashboard-types/common-types";
import { BarChartProps, ChartConfigType } from "shared-resources/charts/chart-types";

export type DemoEffortTrendChartProps = {
  data: BarChartProps & ChartConfigType & { records: any[] } & { donutData: any[] } & any;
  chartProps: chartPropsType;
  stacked: boolean;
  reportType?: string;
  transformFn?: (data: any) => any;
  totalCountTransformFn?: (data: any) => any;
  onClick: (x: any) => void;
  hideLegend?: boolean;
  previewOnly?: boolean;
  id: string;
};
