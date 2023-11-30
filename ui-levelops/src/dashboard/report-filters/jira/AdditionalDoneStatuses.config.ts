import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import ExcludeStatusAPIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/ExcludeStatusApiContainer";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get } from "lodash";
import { ApiDropDownData, LevelOpsFilter } from "./../../../model/filters/levelopsFilters";

export const AdditionalDoneStatusFilterConfig: LevelOpsFilter = {
  id: "additional_done_statuses",
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: ExcludeStatusAPIFilterContainer,
  label: "Additional Done Statuses",
  beKey: "additional_done_statuses",
  labelCase: "title_case",
  filterMetaData: {
    selectMode: "multiple",
    uri: "jira_filter_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["status"],
        filter: { integration_ids: get(args, "integrationIds", []), status_categories: ["Done", "DONE"] }
      };
    },
    specialKey: "additional_done_statuses",
    options: (args: any) => {
      const data = args?.filterMetaData?.apiConfig?.status_data ?? [];
      const excludeStatusState = args?.filterMetaData?.apiConfig?.data ?? [];
      const excludeStatus = get(excludeStatusState, [0, "status"], []).map((item: any) => item.key);
      const statusObject = data
        ? data.filter(
            (item: { [filterType: string]: { [key: string]: string }[] }) => Object.keys(item)[0] === "status"
          )[0]
        : [];
      if (statusObject && Object.keys(statusObject).length > 0) {
        let list = statusObject.status;
        if (list) {
          list = list.filter((item: { [key: string]: string }) => !excludeStatus.includes(item.key));
        } else {
          list = [];
        }
        return (list || []).map((item: any) => ({ label: item.key, value: item.key }));
      }
      return [];
    },
    sortOptions: true
  } as ApiDropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.SETTINGS
};

export const generateAdditionalDoneStatusFilterConfig = (beKey?: string) => ({
  ...AdditionalDoneStatusFilterConfig,
  beKey: beKey ?? AdditionalDoneStatusFilterConfig.beKey
});
