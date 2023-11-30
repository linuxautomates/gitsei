import { getPrefixLabel } from "dashboard/components/dashboard-header/dashboard-actions/DashboardOUInfoModal/helper";
import { toTitleCase } from "utils/stringUtils";
import { FilterType, InfoDataType, IntegrationFilterType } from "./typing";
import { WORKFLOW_PROFILE_MENU } from "configurations/pages/lead-time-profiles/containers/workflowDetails/components/constant";
import { get } from "lodash";
import { HARNESS_CICD_ID_TOLABEL_MAPPINGS } from "configurations/pages/Organization/Filters/harnessng/harnessng-job-filter.config";
import { JENKINS_CICD_ID_TOLABEL_MAPPINGS } from "dashboard/reports/jenkins/constants";

export const getUserOrFilterValue = ({ value, type, label }: FilterType) => {
  const labelsMap = {...JENKINS_CICD_ID_TOLABEL_MAPPINGS, ...HARNESS_CICD_ID_TOLABEL_MAPPINGS};
  // @ts-ignore
  const newlabel = labelsMap[label] || toTitleCase(label);
  const newType = getPrefixLabel(type);
  let newValue = value;
  if (Array.isArray(value)) {
    newValue = value.join(",");
  }
  return `${newlabel} ${newType} ${newValue}`;
};

export const transformOrgIntegrations = (integrationFilters: IntegrationFilterType[]) => {
  const dataArray: InfoDataType[] = [];

  integrationFilters?.forEach(item => {
    dataArray.push({ key: "INTEGRATION", value: item?.name, className: "indent-top" });

    item?.filters?.forEach(filter => {
      const value = getUserOrFilterValue(filter);
      dataArray.push({ key: "FILTER", value, className: "indent-left" });
    });
  });

  return dataArray;
};

export const tabMap = {
  change_failure_rate : WORKFLOW_PROFILE_MENU.CHANGE_FAILURE_RATE,
  deployment_frequency_report: WORKFLOW_PROFILE_MENU.DEPLOYMENT_FREQUENCY,
  leadTime_changes:WORKFLOW_PROFILE_MENU.LEAD_TIME_FOR_CHANGES,
  meanTime_restore:WORKFLOW_PROFILE_MENU.MEAN_TIME_TO_RESTORE
}

export const getURL = (report:string, id:string) => {
  const tab = get(tabMap,report);
  return `configId=${id}&tabComponent=${tab}`;
}