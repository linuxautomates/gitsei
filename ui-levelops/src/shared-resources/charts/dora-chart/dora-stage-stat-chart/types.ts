import { number } from "prop-types";

export interface BucketType {
  rating: string;
  count: number;
}

export interface StageType {
  id: string;
  key: string;
  duration: number;
  lower_limit: number;
  upper_limit: number;
}

export interface DoraStageComponentProps {
  stage: StageType;
  onClick?: (v: any) => void;
  isActivePhase?: boolean;
  isEndEvent?: boolean;
  buckets: BucketType[];
  dataKey: string;
  metrics: string;
}

export interface DoraStatData {
  lead_time_mean: number;
  lead_time_median: number;
  lead_time_p90: number;
  lead_time_p95: number;
  lead_time_band_mean: string;
  lead_time_band_median: string;
  lead_time_band_p90: string;
  lead_time_band_p95: string;
  total: number;
}

export interface DoraStageData {
  key: string;
  additional_key: string;
  median: number;
  count: number;
  mean: number;
  p90: number;
  p95: number;
  good: number;
  acceptable: number;
  slow: number;
  good_count: number;
  missing_count: number;
  acceptable_count: number;
  velocity_stage_result: {
    lower_limit_value: number;
    lower_limit_unit: string;
    upper_limit_value: number;
    upper_limit_unit: string;
    rating: string;
  };
}
