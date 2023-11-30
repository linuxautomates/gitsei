import { WIDGET_CONFIGURATION_KEYS } from "constants/widgets";
import { CUSTOM_FIELD_PREFIX } from "dashboard/constants/constants";
import UniversalSelectFilterWrapper from "dashboard/graph-filters/components/GenericFilterComponents/UniversalSelectFilterWrapper";
import { get, uniqBy } from "lodash";
import { LevelOpsFilter, DropDownData } from "model/filters/levelopsFilters";
import { ACROSS_OPTIONS } from "../commonZendeskReports.constants";
import { ZENDESK_TICKET_REPORT_ACROSS_OPTIONS } from "./constants";
import { getAcrossValue } from "./helper";

export const StackFilterConfig: LevelOpsFilter = {
  id: "stacks",
  renderComponent: UniversalSelectFilterWrapper,
  label: "Stacks",
  beKey: "stacks",
  labelCase: "title_case",
  filterMetaData: {
    clearSupport: true,
    options: (args: any) => {
      const commonOptions = ACROSS_OPTIONS;
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData.map((item: any) => ({ label: item.name, value: item.key }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.METRICS
};

export const AcrossFilterConfig: LevelOpsFilter = {
  id: "across",
  renderComponent: UniversalSelectFilterWrapper,
  label: "X-Axis",
  beKey: "across",
  labelCase: "title_case",
  required: true,
  getMappedValue: getAcrossValue,
  filterMetaData: {
    selectMode: "default",
    options: (args: any) => {
      const commonOptions = ZENDESK_TICKET_REPORT_ACROSS_OPTIONS;
      const filterMetaData = get(args, ["filterMetaData"], {});
      const customFieldsData = get(filterMetaData, ["customFieldsRecords"], []);
      const customFieldsOptions = customFieldsData.map((item: any) => ({
        label: item.name,
        value: `${CUSTOM_FIELD_PREFIX}${item.key}`
      }));
      return uniqBy([...commonOptions, ...customFieldsOptions], "value");
    },
    sortOptions: true
  } as DropDownData,
  tab: WIDGET_CONFIGURATION_KEYS.AGGREGATIONS
};
