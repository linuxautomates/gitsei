import moment from "moment";
import { isvalidTimeStamp } from "../../../../utils/dateUtils";
import { GROUP_BY_TIME_FILTERS } from "../../../../constants/filters";
import { CustomTimeBasedTypes } from "../../../graph-filters/components/helper";
import { idFilters } from "../commonJiraReports.constants";

export const xAxisLabelTransform = (params: any) => {
  const { across, item = {}, CustomFieldType } = params;
  const { key, additional_key } = item;
  let newLabel = key;
  const isValidDate = isvalidTimeStamp(newLabel);
  if (idFilters.includes(across)) {
    newLabel = additional_key;
    return newLabel;
  }
  if (
    (CustomFieldType && CustomTimeBasedTypes.includes(CustomFieldType) && isValidDate) ||
    (isValidDate && !GROUP_BY_TIME_FILTERS.includes(across))
  ) {
    newLabel = moment(parseInt(key)).format("DD-MM-YYYY HH:mm:ss");
    return newLabel;
  }
  if (!newLabel) {
    newLabel = "UNRESOLVED";
  }
  return newLabel;
};
