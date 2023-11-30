import {
  CATEGORY,
  CSV_DRILLDOWN_TRANSFORMER,
  GET_CUSTOMIZE_TITLE,
  IS_FRONTEND_REPORT,
  SHOW_SETTINGS_TAB
} from "dashboard/constants/filter-key.mapping";
import { ChartContainerType } from "dashboard/helpers/helper";
import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { DoraLeadTimeChangesReportType } from "model/report/dora/leadtime-changes/leadtimeChanges.constants";
import { FILTER_WARNING_LABEL, LEAD_TIME_CHANGES_DESCRIPTION } from "../constants";
import { WIDGET_MIN_HEIGHT } from "dashboard/constants/helper";
import { GET_GRAPH_FILTERS, REPORT_FILTERS_CONFIG } from "dashboard/constants/applications/names";
import {
  csvTransformer,
  getCheckboxValue,
  getDoraProfileIntegrationId,
  getDoraProfileIntegrationType,
  getDrilldownCheckBox,
  getDrilldownFooter,
  getDrilldownTitle,
  getDrillDownType,
  getFilterConfig,
  getFilterKeysToHide,
  getGraphFilters,
  getShowTitle,
  handleRatingChange,
  mapFiltersBeforeCall
} from "./helper";
import { chartProps, CHECKBOX_TITLE, WIDGET_CONFIG_FILTERS } from "./constants";
import { leadTimeForChangeDrilldown } from "dashboard/constants/drilldown.constants";
import { STORE_ACTION } from "dashboard/constants/bussiness-alignment-applications/constants";
import { getDoraLeadTimeWidgetDataAction } from "reduxConfigs/actions/restapi/widgetAPIActions";
import { getDoraReportsTitle } from "../doraReportTitle";
import { getDoraSingleStateValue, getHideFilterButton } from "../helper";

const leadTimeForChangesReport: { leadTime_changes: DoraLeadTimeChangesReportType } = {
  leadTime_changes: {
    name: "Lead Time for Changes",
    application: "any",
    chart_type: ChartType?.DORA_COMBINED_STAGE_CHART,
    chart_container: ChartContainerType.DORA_API_WRAPPER,
    chart_props: chartProps,
    drilldown: leadTimeForChangeDrilldown,
    uri: "dora_lead_time_for_change",
    method: "list",
    [IS_FRONTEND_REPORT]: true,
    [CATEGORY]: "dora",
    description: LEAD_TIME_CHANGES_DESCRIPTION,
    isAdvancedFilterSetting: true,
    [SHOW_SETTINGS_TAB]: true,
    [WIDGET_MIN_HEIGHT]: "260px",
    widgetActionSelectFilter: WIDGET_CONFIG_FILTERS,
    [REPORT_FILTERS_CONFIG]: getFilterConfig,
    getDoraProfileIntegrationType: getDoraProfileIntegrationType,
    mapFiltersBeforeCall: mapFiltersBeforeCall,
    drilldownFooter: getDrilldownFooter,
    drilldownCheckbox: getDrilldownCheckBox,
    getDrilldownTitle: getDrilldownTitle,
    getDrillDownType: getDrillDownType,
    [STORE_ACTION]: getDoraLeadTimeWidgetDataAction,
    [GET_GRAPH_FILTERS]: getGraphFilters,
    filterWarningLabel: FILTER_WARNING_LABEL,
    [GET_CUSTOMIZE_TITLE]: getDoraReportsTitle,
    drilldownCheckBoxhandler: {
      getCheckBoxValue: getCheckboxValue,
      handleRatingChange: handleRatingChange,
      title: CHECKBOX_TITLE
    },
    getShowTitle: getShowTitle,
    hideFilterButton: getHideFilterButton,
    [CSV_DRILLDOWN_TRANSFORMER]: csvTransformer,
    getFilterKeysToHide: getFilterKeysToHide,
    getDoraProfileIntegrationId: getDoraProfileIntegrationId,
    getDoraSingleStateValue: getDoraSingleStateValue
  }
};

export default leadTimeForChangesReport;
