import { LEADTIME_SCM_OU_DESIGNATION_EXCLUDE_APPLICATIONS } from "./constants";
import { uniq } from "lodash";
import { Integration } from "model/widget/Integration";
import {
  LEAD_TIME_OU_DESIGNATION_REPORTS,
  SCM_LEAD_TIME_OU_DESIGNATION_REPORTS
} from "shared-resources/containers/widget-api-wrapper/helper-constant";

// this change is related to LFE-2202, updating ou_designation so that BE can classify between jira and github
export const leadtimeOUDesignationQueryBuilder = (
  filters: any,
  reportType: string,
  availableIntegrations?: Integration[]
) => {
  let allApplications: string[] = availableIntegrations?.map(integration => integration.application) || [];
  allApplications = uniq(allApplications);

  let scmApplications = LEADTIME_SCM_OU_DESIGNATION_EXCLUDE_APPLICATIONS.filter(application =>
    allApplications.includes(application)
  );
  const issueApplications = allApplications.filter(application => !scmApplications.includes(application));

  // if no integrations available, sending both jira and azure_devops
  if (!scmApplications.length) {
    scmApplications = LEADTIME_SCM_OU_DESIGNATION_EXCLUDE_APPLICATIONS;
  }

  // all Lead time report
  if (LEAD_TIME_OU_DESIGNATION_REPORTS.includes(reportType as any)) {
    filters = {
      ...filters,
      ou_user_filter_designation: {
        ...(filters?.ou_user_filter_designation || {}),
        ...issueApplications.reduce((acc, application) => ({ ...acc, [application]: ["none"] }), {})
      }
    };
  }

  // all scm lead time reports
  if (SCM_LEAD_TIME_OU_DESIGNATION_REPORTS.includes(reportType as any)) {
    filters = {
      ...filters,
      ou_user_filter_designation: {
        ...(filters?.ou_user_filter_designation || {}),
        ...scmApplications.reduce((acc, application) => ({ ...acc, [application]: ["none"] }), {})
      }
    };
  }

  return filters;
};
