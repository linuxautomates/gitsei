import { optionType } from "dashboard/dashboard-types/common-types";

export type StatTimeBasedFilterConfig = {
  defaultValue: string;
  options: Array<optionType>;
  getFilterLabel: (data: any) => string;
  getFilterKey: (data: any) => any;
};
