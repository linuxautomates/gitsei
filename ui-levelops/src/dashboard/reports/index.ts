import jiraReports from "./jira";
import bullseyeReports from "./bullseye";
import coverityReports from "./coverity";
import MicrosoftReports from "./microsoft";
import nccGroupReports from "./ncc-group";
import pagerdutyReports from "./pagerduty";
import snykReports from "./snyk";
import testRailsReports from "./testRails";
import jiraZendeskReports from "./jiraZendesk";
import jiraSalesforceReports from "./jiraSalesforce";
import praetorianReport from "./praetorian";
import DevProductivityReports from "./dev-productivity";
import scmReports from "./scm";
import azureReports from "./azure";
import miscellaneousReports from "./miscellaneous";

const allReports = {
  ...jiraReports,
  ...jiraSalesforceReports,
  ...jiraZendeskReports,
  ...pagerdutyReports,
  ...testRailsReports,
  ...bullseyeReports,
  ...coverityReports,
  ...snykReports,
  ...nccGroupReports,
  ...MicrosoftReports,
  ...praetorianReport,
  ...coverityReports,
  ...DevProductivityReports,
  ...scmReports,
  ...azureReports,
  ...miscellaneousReports
};

export default allReports;
