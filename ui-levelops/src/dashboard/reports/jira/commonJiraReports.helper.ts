import { CustomTimeBasedTypes } from "dashboard/graph-filters/components/helper";
import { get } from "lodash";
import moment from "moment";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { isvalidTimeStamp } from "../../../utils/dateUtils";
import { idFilters } from "./commonJiraReports.constants";
import { CUSTOM_FIELD_PREFIX } from "../../constants/constants";
import widgetConstants from '../../constants/widgetConstants'
import { REQUIRED_ONE_FILTER_KEYS } from "dashboard/constants/filter-key.mapping";

export const jiraXAxisLabelTransformForCustomFields = (params: any) => {
  const { item = {}, CustomFieldType, xAxisLabelKey, across } = params;
  const { key, additional_key } = item;
  let newLabel = key;

  if (idFilters.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }

  const isValidDate = isvalidTimeStamp(key);
  if (CustomFieldType && ((CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) || isValidDate)) {
    newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
    return newLabel;
  }
  if (!newLabel) {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};

export const jiraOnChartClickPayload = (params: any) => {
  const { data, across, visualization } = params;
  const { activeLabel, activePayload } = data;
  let keyValue = activeLabel;

  if (visualization && visualization === ChartType.DONUT) {
    keyValue = get(data, ["tooltipPayload", 0, "name"], "_UNASSIGNED_");

    if (idFilters.includes(across) || across.includes(CUSTOM_FIELD_PREFIX)) {
      keyValue = {
        id: get(data, ["key"], "_UNASSIGNED_"),
        name: get(data, ["name"], "_UNASSIGNED_")
      };
    }
    return keyValue;
  } else {
    if (idFilters.includes(across) || across.includes(CUSTOM_FIELD_PREFIX)) {
      keyValue = {
        id: get(activePayload, [0, "payload", "key"], "_UNASSIGNED_"),
        name: get(activePayload, [0, "payload", "name"], "_UNASSIGNED_")
      };
    }
  }
  return keyValue;
};

export const handleRequiredForFilters = (config: any, filters: any, report: any, dashboardTimeRangeKey?: any) => {
  const filterKeysArr = get(widgetConstants, [report, REQUIRED_ONE_FILTER_KEYS], []);
  let newConfig = [...config];
  let filtersCount = 0;
  let requiredElement: any = undefined;
  filterKeysArr.forEach((element: any) => {
    if (filters[element]) {
      filtersCount++;
      requiredElement = element;
    } else if (dashboardTimeRangeKey && Object.keys(dashboardTimeRangeKey).length > 0 && dashboardTimeRangeKey[element]){
      filtersCount++;
      requiredElement = element;
    }
  });
  if (filtersCount === 1) {
    const index = newConfig.findIndex((cnf: any) => cnf.beKey === requiredElement);
    newConfig[index].deleteSupport = false;
    newConfig[index].required = true;
  } else {
    newConfig = newConfig.map((cnf: any) => {
      if (filterKeysArr.indexOf(cnf.beKey) !== -1) {
        cnf.deleteSupport = true;
        cnf.required = false;
      }
      return cnf;
    });
  }
  return config;
};
