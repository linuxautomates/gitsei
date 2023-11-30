import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { snykSupportedFilters } from "dashboard/constants/supported-filters.constant";
import APIFilterContainer from "dashboard/graph-filters/components/GenericFilterComponents/APIFilter.container";
import { extractFilterAPIData } from "dashboard/report-filters/helper";
import { get } from "lodash";
import { ApiDropDownData, baseFilterConfig, LevelOpsFilter, LevelOpsFilterTypes } from "model/filters/levelopsFilters";
import OrganisationFilterSelect from "../organisationFilterSelect";

const synkCommonFilters = snykSupportedFilters.values.map((item: string) => ({
  label: item.replace(/_/g, " ")?.toUpperCase(),
  key: item
}));

const SYNK_FILTER_MAPPING = {
  type: "types",
  severity: "severities",
  project: "project"
};
export const OUSynkCommonFiltersConfig: LevelOpsFilter[] = [
  ...synkCommonFilters.map((item: { key: string; label: string }) =>
    baseFilterConfig((SYNK_FILTER_MAPPING as any)[item.key], {
      renderComponent: OrganisationFilterSelect,
      apiContainer: APIFilterContainer,
      label: item.label,
      type: LevelOpsFilterTypes.API_DROPDOWN,
      labelCase: "title_case",
      deleteSupport: true,
      partialSupport: true,
      excludeSupport: true,
      partialKey: item.key,
      filterMetaData: {
        selectMode: "multiple",
        uri: "snyk_issues_values",
        method: "list",
        payload: (args: Record<string, any>) => {
          return {
            integration_ids: get(args, "integrationIds", []),
            fields: [item.key],
            filter: { integration_ids: get(args, "integrationIds", []) }
          };
        },
        specialKey: item.key,
        options: (args: any) => {
          const data = extractFilterAPIData(args, item.key);
          return data
            ?.map((item: any) => ({
              label: item.additional_key ?? item.key,
              value: item.key
            }))
            .filter((item: { label: string; value: string }) => !!item.value);
        },
        sortOptions: true,
        createOption: true
      } as ApiDropDownData
    })
  )
];
