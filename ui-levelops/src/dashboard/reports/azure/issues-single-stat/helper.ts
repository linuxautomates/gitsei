import { azureIssuesSingleStatReportTransformer } from "custom-hooks/helpers/issuesSingleStat.helper";
import { get } from "lodash";

export const transformFunction = (data: any) => {
  const { apiData, widgetFilters } = data;
  const { across } = widgetFilters;
  const newApiData = get(apiData, ["0", across, "records"], []);
  return azureIssuesSingleStatReportTransformer({ ...data, apiData: newApiData });
};
