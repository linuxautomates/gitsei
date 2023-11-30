import { CATEGORY, IS_FRONTEND_REPORT, SHOW_SETTINGS_TAB } from "dashboard/constants/filter-key.mapping";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { DoraMeanTimeRestoreReportType } from "model/report/dora/meantime-restore/meantimeRestore.constants";
import { TIME_TO_RECOVER_DESCRIPTION } from "../constants";
import { getHideFilterButton } from "../helper";
import { IntegrationTypes } from "constants/IntegrationTypes";

const meanTimeToRestoreReport: { meanTime_restore: DoraMeanTimeRestoreReportType } = {
  meanTime_restore: {
    name: "Mean Time to Restore",
    application: IntegrationTypes.JIRA,
    chart_type: ChartType?.DORA_COMBINED_STAGE_CHART,
    chart_container: ChartContainerType.DORA_API_WRAPPER,
    drilldown: {},
    uri: "",
    method: "list",
    [IS_FRONTEND_REPORT]: true,
    [CATEGORY]: "dora",
    description: TIME_TO_RECOVER_DESCRIPTION,
    isAdvancedFilterSetting: true,
    hideFilterButton: getHideFilterButton
  }
};

export default meanTimeToRestoreReport;
