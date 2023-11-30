import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import SprintReportFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/SprintReportFilter/SprintReportFilter";
import { get } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import genericApiFilterProps from "../common/common-api-filter-props";

export const SprintReportFilterConfig: LevelOpsFilter = {
  id: "sprint_report",
  renderComponent: SprintReportFilterWrapper,
  label: "Sprint Report",
  beKey: "sprint_report",
  apiFilterProps: genericApiFilterProps,
  deleteSupport: true,
  labelCase: "none",
  filterMetaData: {
    selectMode: "multiple",
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const sprintApiData = get(filterMetaData, ["sprintApiData"], []);
      return (sprintApiData || []).map((item: any) => ({ label: item.name, value: item.name }));
    },
    sortOptions: true,
    createOption: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS
};
