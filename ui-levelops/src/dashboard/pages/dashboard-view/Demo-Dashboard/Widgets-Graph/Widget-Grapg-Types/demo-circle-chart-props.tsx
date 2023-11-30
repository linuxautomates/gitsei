export type DemoCircleChartProps = {
  id?: string;
  data: Array<Object>;
  hideLegend?: boolean;
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
  colorSchema?: Record<string, string>;
  reportType?: string;
  onClick?: (x: any) => void;
};
