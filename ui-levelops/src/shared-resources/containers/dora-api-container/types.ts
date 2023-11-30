import { type } from "os";

type barValue = { key: number; count: number; additional_key: string };

export type FailureRate = {
  failure_rate?: number;
  band?: "HIGH" | "LOW" | "ELITE" | "MEDIUM";
  total_deployment: number;
  is_absolute: boolean;
};
export type Deployments = {
  count_per_day: number;
  band: "HIGH" | "LOW" | "ELITE" | "MEDIUM";
  total_deployment: number;
  count_per_week?: number;
  count_per_month?: number;
};

export type DoraLeadTimeForChangeStatProps = {
  simplifyValue: {
    simplifiedValue: number;
    unit: string;
  };
  unitSymbol: string;
};

export interface DoraBarChartResponseType {
  time_series: {
    day: Array<barValue>;
    week: Array<barValue>;
    month: Array<barValue>;
  };
  stats: FailureRate | Deployments;
}
