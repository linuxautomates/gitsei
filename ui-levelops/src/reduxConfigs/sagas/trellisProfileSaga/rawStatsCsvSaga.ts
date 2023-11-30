import { select, takeLatest } from "redux-saga/effects";
import { RAW_STATS_CSV_DOWNLOAD } from "reduxConfigs/actions/actionTypes";
import { RestWidget } from "../../../classes/RestDashboards";
import { getWidget } from "../../selectors/widgetSelector";
import { get } from "lodash";
import { saveAs } from "file-saver";
import { csvDrilldownDataTransformer } from "../../../dashboard/helpers/csv-transformers/csvDrilldownDataTransformer";
import widgetConstants from "dashboard/constants/widgetConstants";
import { getWidgetDataSelector } from "reduxConfigs/selectors/widgetAPISelector";
import { ColumnPropsWithType } from "dashboard/reports/dev-productivity/rawStatsTable.config";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";

export type rawStatsCSVDownloadActionType = {
  type: string;
  dashboardId: string;
  widgetId: string;
  queryparams?: any;
};

export function* rawStatsCSVDownloadEffectSaga(action: rawStatsCSVDownloadActionType) {
  const { dashboardId, widgetId, queryparams } = action;

  try {
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    const reportType = widget?.type;
    const widgetConstant = get(widgetConstants, [reportType], []);
    const chartProps = widgetConstant.getChartProps(widget);
    const reportName = widgetConstant.name;
    const tableColumns = chartProps.columns;

    let columns: Array<{ titleForCSV: string; key: string }> = [];
    tableColumns.forEach((tabColumn: ColumnPropsWithType<any>) => {
      if (tabColumn.children) {
        tabColumn.children.forEach((child: ColumnPropsWithType<any>) => {
          columns.push({
            titleForCSV: typeof child.title === "string" ? child.title : child.titleForCSV || "",
            key: child.dataIndex || ""
          });
        });
      } else {
        columns.push({
          titleForCSV: typeof tabColumn.title === "string" ? tabColumn.title : tabColumn.titleForCSV || "",
          key: tabColumn.dataIndex || ""
        });
      }
    });

    // @ts-ignore
    const widgetDataState = yield select(getWidgetDataSelector, { widgetId });

    const headers = columns.map((col: any) => col.titleForCSV);
    let csvText = headers.join(",").concat("\n");
    const csvData = csvDrilldownDataTransformer({ apiData: widgetDataState.data, columns });
    csvText = csvText.concat(csvData.join("\n")).concat("\n");
    // rename dev_productivity to trellis_profile
    const fileName = reportName.replace(" ", "_").toLowerCase();
    const file = new File([csvText], fileName, { type: "text/csv;charset=utf-8" });
    saveAs(file);
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.FILES,
        data: { e, action }
      }
    });
  }
}

export function* rawStatsCSVDownloadWatcherSaga() {
  yield takeLatest([RAW_STATS_CSV_DOWNLOAD], rawStatsCSVDownloadEffectSaga);
}
