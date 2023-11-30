import { TICKET_CATEGORIZATION_UNIT_FILTER_KEY } from "dashboard/constants/bussiness-alignment-applications/constants";
import { EffortUnitType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { get } from "lodash";
import { AzureCommitEIByEngineerFiltersConfig, AzureEIByEngineerFiltersConfig } from "./filters.config";

export const reportFilterConfig = (args: any) => {
  const { filters } = args;
  const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
  if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
    return AzureCommitEIByEngineerFiltersConfig;
  }
  return AzureEIByEngineerFiltersConfig;
};

export const hideCustomFields = (args: any) => {
  const { filters } = args;
  const uriUnit = get(filters, [TICKET_CATEGORIZATION_UNIT_FILTER_KEY], undefined);
  if ([EffortUnitType.AZURE_COMMIT_COUNT, EffortUnitType.COMMIT_COUNT].includes(uriUnit)) {
    return true;
  }
  return false;
};
