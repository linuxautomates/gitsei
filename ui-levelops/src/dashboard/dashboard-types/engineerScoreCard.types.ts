import { ColumnProps } from "antd/lib/table";
import { scoreWidgetType } from "dashboard/pages/scorecard/constants";
import { engineerRatingType } from "../constants/devProductivity.constant";

export type ratingType = "GOOD" | "NEED IMPROVEMENT" | "ACCEPTABLE";

export type engineerManagerType = {
  full_name: string;
  email: string;
};

export type engineerFeatureResponsesType = {
  name: string;
  description: string;
  order: number;
  mean: any;
  score: number;
  result: any;
  rating: ratingType;
  feature_unit: string;
};

export type engineerSectionType = {
  name: string;
  description: string;
  order: number;
  score: number;
  feature_responses: engineerFeatureResponsesType[];
};

export type profileExtraInfoType = {
  path: string[];
};

export type scoreWidgetConfig = {
  name: string;
  widget_type: scoreWidgetType;
  width: "half" | "full";
  height: string;
  score_transformer: (data: any) => any;
};

export type scoreOverviewUtilType = {
  finalScore: number;
  rating: engineerRatingType;
  color: string;
  sectionMapping: any;
};

// ou user response object type for dev productivity
export type OUUserDevProdConfig = {
  [x: string]: number | string;
};

export type ouScoreWidgetsConfig = {
  name: string;
  widget_type: scoreWidgetType;
  width: string;
  height: string;
  columns?: ColumnProps<any>[];
  uri?: string;
  method?: string;
  csvTransformer?: (data: any) => any;
};

export type ouOverviewFieldsType = {
  label: string;
  value: any;
  className: string;
};
