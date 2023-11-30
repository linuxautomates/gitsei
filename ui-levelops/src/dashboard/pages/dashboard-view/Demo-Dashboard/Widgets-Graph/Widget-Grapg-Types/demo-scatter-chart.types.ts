import { ChartConfigType } from "shared-resources/charts/chart-types";

export type DemoScatterChartProps = {
  data: Array<Object>;
  yDataKey: string;
  unit: string;
  xUnit: string;
  rangeY: Array<string>;
  hasClickEvents?: boolean;
  onClick?: (x: string) => void;
  id?: string;
  config?: ChartConfigType;
  previewOnly?: boolean;
  hasTrendLikeData?: boolean;
  reportType?: string;
};
