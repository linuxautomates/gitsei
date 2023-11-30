import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { FEBasedFilterMap } from "dashboard/dashboard-types/FEBasedFilterConfig.type";
import { WidgetFilterType } from "../enums/WidgetFilterType.enum";

const acrossValues = ["author", "committer"];

export const scmCodingDaysReportFEBased: FEBasedFilterMap = {
  days_count: {
    type: WidgetFilterType.RANGE_BASED_FILTERS,
    label: "",
    BE_key: "days_count",
    configTab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS,
    isVisible: (filters: any) => {
      const { across } = filters;
      return acrossValues.includes(across);
    },
    getLabel: (filters: any) => {
      const { across } = filters;
      if (acrossValues.includes(across)) {
        return `SHOW ${across.toUpperCase()}S WITH`;
      }
      return "";
    }
  }
};
