import { EffortType, EffortUnitType, IntervalType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { basicActionType, basicMappingType, basicRangeType } from "./common-types";

export type BARangeConfigType = {
  start_date: number;
  end_date: number;
  id?: string;
  name: string;
};

export type RangeType = {
  $lt: string;
  $gt: string;
};

export type effortInvestmentTrendInitialDataType = {
  id?: string;
  name?: string;
  description?: string;
  index?: number;
  filter?: any;
  color?: string;
  completed_points: { [x: string]: any }[];
};

export type BASubRangeModifcationType = { offset: string; factor: number };

export type basicActionFunctionType = (...args: any) => basicActionType<any>;

export type BAEffortTypeSwitchConfig<T> = {
  [x in EffortType]: {
    [y in EffortUnitType]: T;
  };
};

export type EICategoryGoalsTypes = {
  enabled: boolean;
  ideal_range: { min: number; max: number };
  acceptable_range: basicRangeType;
};

export type EICategoryTypes = {
  id?: string;
  index?: number;
  name: string;
  description?: string;
  color?: string;
  filter?: any;
  goals?: EICategoryGoalsTypes;
};

export type completedEffortRecord = { key: string; fte: number; total: number };
export type completedEffortRecordType = {
  engineer: string;
  allocation_summary: basicMappingType<number>;
};

export type categoryAllocationType = {
  alignment_score: number;
  allocation: number;
  effort: number;
  total_effort: number;
};

export type alignmentCategoryConfig = {
  ideal_range?: { min: number; max: number };
  color?: string;
  alignment_score: number;
  allocation: number;
};

export type categoryAlignmentConfig = { name: string; config: alignmentCategoryConfig; id: string };

export type aligmentDataType = {
  total_alignment_score: number;
  categories: categoryAlignmentConfig[];
  profileId: string;
};

export type alignmentActiveConfig = {
  alignment_score: number;
  category_allocations: basicMappingType<categoryAllocationType>;
};
export type alignmentCategoryConfigType = "alignment_score" | "category_allocations";

export type EITrendReportTimeRangeListFuncType = (
  categoryWiseRecords: effortInvestmentTrendInitialDataType[],
  interval: IntervalType.WEEK | IntervalType.BI_WEEK | IntervalType.MONTH,
  timeRangeDisplayFormat: string
) => Array<BARangeConfigType>;

export enum UncategorizedNames {
  OTHER = "Other",
  UNCATEGORIZED = "Uncategorized"
}
