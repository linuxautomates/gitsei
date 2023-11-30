import { ChartType } from "shared-resources/containers/chart-container/ChartType";
import { ChartContainerType } from "dashboard/helpers/helper";
import { DevProductivityOrgUnitScoreReportType } from "model/report/dev-productivity/dev-productivity-org-unit-score-report/devProductivityOrgUnitScoreReport.constants";
import { chartPropsOrgUnit, defaultQuery, defaultSort, filters } from "./../constants";
import { GET_WIDGET_CHART_PROPS } from "../../../constants/filter-name.mapping";
import {
  CSV_DRILLDOWN_TRANSFORMER,
  SHOW_AGGREGATIONS_TAB,
  SHOW_METRICS_TAB,
  IS_FRONTEND_REPORT
} from "../../../constants/filter-key.mapping";
import { csvTransformerFn, getGraphFilters, transformerFn } from "./helper";
import { GET_GRAPH_FILTERS, GET_WIDGET_TITLE_INTERVAL } from "dashboard/constants/applications/names";
import { get } from "lodash";
import { emptyWidgetPreviewFunc } from "../helper";
import { getDynamicColumns } from "./tableConfig";

const devOrgUnitScoreReport: { dev_productivity_org_unit_score_report: DevProductivityOrgUnitScoreReportType } = {
  dev_productivity_org_unit_score_report: {
    name: "Trellis Scores by Collections",
    application: "dev_productivity",
    chart_type: ChartType?.DEV_PROD_TABLE_CHART,
    chart_container: ChartContainerType.WIDGET_API_WRAPPER,
    uri: "dev_productivity_org_unit_score_report",
    method: "list",
    xaxis: false,
    chart_props: chartPropsOrgUnit,
    filters: filters,
    default_query: defaultQuery,
    defaultSort: {},
    widget_height: "12rem",
    transformFunction: transformerFn,
    [CSV_DRILLDOWN_TRANSFORMER]: csvTransformerFn,
    getDynamicColumns: getDynamicColumns,
    show_notification_on_error: false,
    render_empty_widget_preview_func: emptyWidgetPreviewFunc,
    [IS_FRONTEND_REPORT]: true,
    [SHOW_METRICS_TAB]: false,
    [SHOW_AGGREGATIONS_TAB]: false,
    [GET_GRAPH_FILTERS]: getGraphFilters,
    [GET_WIDGET_CHART_PROPS]: data => {
      const { filters = {} } = data;
      const interval = get(filters, ["filter", "interval"], "");
      const ou_id = get(filters, ["filter", "ou_ref_ids"], "");
      return {
        interval,
        ou_id
      };
    },
    [GET_WIDGET_TITLE_INTERVAL]: true
  }
};

export default devOrgUnitScoreReport;
