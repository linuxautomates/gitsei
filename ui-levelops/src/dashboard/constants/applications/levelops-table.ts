import { ChartContainerType, WidgetType } from "../../helpers/helper";
import { WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE } from "../filter-name.mapping";
import { levelOpsTableReportValidatorFunction } from "../../helpers/widgetValidation.helper";
import { get } from "lodash";
import { levelOpsTableReportDrilldown } from "../drilldown.constants";
import { ChartType } from "../../../shared-resources/containers/chart-container/ChartType";
import { XAXIS_TRUNCATE_LENGTH } from "./constant";
import { PREV_REPORT_TRANSFORMER } from "./names";
import { levelopsTableReportPrevQueryTransformer } from "dashboard/helpers/previous-query-transformers/levelops/levelopsTableReportPrevQueryTransformer";
import { overrideFilterWithStackFilter } from "./helper";
import { MAX_STACK_ENTRIES } from "dashboard/reports/jira/issues-report/constants";
import { generateBarColors } from "dashboard/reports/jira/issues-report/helper";

export const levelopsTable = {
  levelops_table_single_stat: {
    name: "Table Single Stat",
    chart_type: ChartType?.STATS,
    chart_container: ChartContainerType.CONFIG_TABLE_API_WRAPPER,
    supported_widget_types: [WidgetType.CONFIGURE_WIDGET_STATS]
  },
  levelops_table_report: {
    name: "Custom Table Report",
    supported_widget_types: [WidgetType.CONFIGURE_WIDGET],
    chart_container: ChartContainerType.CONFIG_TABLE_API_WRAPPER,
    chart_props: {
      xAxisProps: {
        interval: 0,
        [XAXIS_TRUNCATE_LENGTH]: 20
      }
    },
    drilldown: levelOpsTableReportDrilldown,
    overrideFilterWithStackFilter: overrideFilterWithStackFilter,
    onChartClickPayload: (args: any) => {
      const { data } = args;
      return get(data, ["activeLabel"], "");
    },
    [PREV_REPORT_TRANSFORMER]: levelopsTableReportPrevQueryTransformer,
    [WIDGET_VALIDATION_FUNCTION_WITH_ERROR_MESSAGE]: levelOpsTableReportValidatorFunction,
    maxStackEntries: MAX_STACK_ENTRIES,
    stackByFilterMode: "default",
    isAllowMultipleStackSelection: false,
    isAllowRealTimeDrilldownDataUpdate: true,
    generateBarColors: generateBarColors
  }
};
