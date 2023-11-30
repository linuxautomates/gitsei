import LeadTimeByStageFooter from "shared-resources/containers/server-paginated-table/components/Table-Footer/Lead-time-stage-footer";
import DrilldownViewMissingCheckbox from "dashboard/reports/jira/lead-time-by-stage-report/DrilldownViewMissingCheckbox";
export const getDrilldownFooter = () => {
  return LeadTimeByStageFooter;
};

export const getDrilldownCheckBox = () => {
  return DrilldownViewMissingCheckbox;
};

export const getExcludeWithPartialMatchKey = (keyName: string) => {
  if (keyName === 'source_branches') {
    return true;
  }
  return false
}