import { LegendProps } from "recharts";

export interface ChartLegendProps extends LegendProps {
    filters: any;
    allowLabelTransform?: boolean;
    setFilters: (filters: any) => void;
    labelMapping?: { [x: string]: string };
    readOnlyLegend?: boolean;
    report?: string;
    legendsProps?: { [x: string]: any };
    trendLineKey?: string;
  }
  
export enum FilterActionType {
    SELECT_ALL = "selectAll",
    CLEAR = "clearAll"
  }