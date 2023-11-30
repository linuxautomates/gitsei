import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { WEEK_DATE_FORMAT, WEEK_FORMAT_CONFIG_OPTIONS } from "constants/time.constants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { get } from "lodash";
import { TIME_FILTERS_KEYS } from "constants/filters";

export const WeekDateFormatConfig: LevelOpsFilter = {
  id: "week_date_format",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Week Date Format",
  beKey: "weekdate_format",
  defaultValue: WEEK_DATE_FORMAT.DATE,
  labelCase: "title_case",
  hideFilter: (args: any) => {
    const interval = get(args, ["filters", "interval"], "");
    const across = get(args, ["filters", "across"]);
    return !TIME_FILTERS_KEYS.includes(across) || interval !== "week";
  },
  updateInWidgetMetadata: true,
  filterMetaData: {
    selectMode: "default",
    options: (args: any) => {
      return WEEK_FORMAT_CONFIG_OPTIONS;
    }
  },
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
