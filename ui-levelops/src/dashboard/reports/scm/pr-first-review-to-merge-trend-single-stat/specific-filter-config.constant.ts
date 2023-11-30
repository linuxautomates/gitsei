import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import genericApiFilterProps from "dashboard/report-filters/common/common-api-filter-props";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";

export const LabelFilterConfig: LevelOpsFilter = baseFilterConfig("labels", {
  renderComponent: UniversalSelectFilterWrapper,
  apiContainer: APIFilterContainer,
  label: "SCM Label",
  tab: WIDGET_CONFIGURATION_KEYS.FILTERS,
  type: LevelOpsFilterTypes.API_DROPDOWN,
  labelCase: "none",
  deleteSupport: true,
  partialSupport: true,
  excludeSupport: true,
  partialKey: "label",
  supportPaginatedSelect: true,
  apiFilterProps: genericApiFilterProps,
  filterMetaData: {
    selectMode: "multiple",
    uri: "github_prs_filter_values",
    method: "list",
    payload: (args: Record<string, any>) => {
      return {
        integration_ids: get(args, "integrationIds", []),
        fields: ["label"],
        filter: { integration_ids: get(args, "integrationIds", []) }
      };
    },
    options: (args: any) => {
      const filterMetaData = get(args, ["filterMetaData"], {});
      const filterApiData = get(filterMetaData, ["apiConfig", "data"], []);
      const currData = filterApiData.find((fData: any) => Object.keys(fData)[0] === "label");
      if (currData) {
        return (Object.values(currData)[0] as Array<any>)?.map((item: any) => ({
          label: item.value,
          value: item.key
        }));
      }
      return [];
    },
    sortOptions: true,
    createOption: true,
    specialKey: "label"
  } as ApiDropDownData
});
