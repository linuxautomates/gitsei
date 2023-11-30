import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";

export const HideStatusFilterConfig: LevelOpsFilter = {
  id: "stages",
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: "Hide Status",
  beKey: "stages",
  labelCase: "title_case",
  filterInfo:
    "Hide selected status values from the graph. It is recommended to hide terminal statuses like Done, Won't Do.",
  filterMetaData: {
    alwaysExclude: true,
    selectMode: "multiple",
    uri: "issue_management_workitem_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["status"],
        filter: { integration_ids: get(args, "integrationIds", []) }
      };
    },
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
      const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "status");
      const data = get(currData, ["status", "records"], []);
      if (data) {
        return (data as Array<any>)?.map((item: any) => ({
          label: item.key,
          value: item.key
        }));
      }
      return [];
    },
    sortOptions: true,
    createOption: true
  } as ApiDropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};
