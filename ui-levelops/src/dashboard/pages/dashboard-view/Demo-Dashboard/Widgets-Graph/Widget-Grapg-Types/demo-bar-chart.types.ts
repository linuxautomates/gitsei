export type DemoBarChartProps = {
  id?: string;
  data: Array<Object>;
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
  barProps: any;
  stacked: boolean;
  hideLegend?: boolean;
  unit: string;
  colorSchema?: Record<string, string>;
  reportType?: string;
  customColors: string[];
  onClick?: (x: any) => void;
  hasClickEvents?: boolean;
};
