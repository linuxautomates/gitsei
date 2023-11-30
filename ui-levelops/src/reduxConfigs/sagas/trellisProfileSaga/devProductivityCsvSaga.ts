import { call, put, select, takeLatest } from "redux-saga/effects";
import { DEV_PRODUCTIVITY_CSV_DOWNLOAD } from "reduxConfigs/actions/actionTypes";
import { RestDashboard, RestWidget } from "../../../classes/RestDashboards";
import { getWidget } from "../../selectors/widgetSelector";
import { getDashboard } from "../../selectors/dashboardSelector";
import { get, uniqBy } from "lodash";
import { notification } from "antd";
import { paginationEffectSaga } from "../paginationSaga";
import { restapiState } from "../../selectors/restapiSelector";
import { saveAs } from "file-saver";
import { getWidgetConstant } from "../../../dashboard/constants/widgetConstants";
import { csvDrilldownDataTransformer } from "../../../dashboard/helpers/csv-transformers/csvDrilldownDataTransformer";
import { CSV_DRILLDOWN_TRANSFORMER } from "../../../dashboard/constants/filter-key.mapping";
import { getError } from "../../../utils/loadingUtils";
import { DEV_PRODUCTIVITY_INTERVAL_OPTIONS } from "dashboard/graph-filters/components/DevProductivityFilters/constants";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";

export type devProductivityCSVDownloadActionType = {
  type: string;
  dashboardId: string;
  widgetId: string;
  queryparams?: any;
};

export function* devProductivityCSVDownloadEffectSaga(action: devProductivityCSVDownloadActionType): any {
  const { dashboardId, widgetId, queryparams } = action;

  try {
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    const dashboard: RestDashboard = yield select(getDashboard, { dashboard_id: dashboardId });
    const reportType = widget?.type;
    const dashboardMetadata = dashboard?.metadata || {};
    const ou_ids = queryparams?.ou_ids ? [queryparams?.ou_ids] : get(dashboardMetadata, "ou_ids", []);
    const query = widget?.query || {};

    const interval = query?.interval || DEV_PRODUCTIVITY_INTERVAL_OPTIONS[1].value;

    let filters: any = {
      filter: {
        interval,
        ou_ref_ids: ou_ids.length ? ou_ids : undefined
      }
    };

    let csvText = "";
    let page = 0;
    let totalPages = 999;

    const uri = getWidgetConstant(reportType, "uri", "");
    const method = getWidgetConstant(reportType, "method", "");
    const columns = getWidgetConstant(reportType, ["chart_props", "columns"], []);
    const transformer = getWidgetConstant(reportType, [CSV_DRILLDOWN_TRANSFORMER], csvDrilldownDataTransformer);

    const mappedID = `${uri}_csv;`;
    let mappedData: any[] = [];
    filters.page_size = 1000;

    while (totalPages > page) {
      filters.page = page;
      yield call(paginationEffectSaga, {
        uri,
        method,
        filters,
        id: mappedID,
        derive: false,
        deriveOnly: [],
        payload: { queryParam: action?.queryparams ?? {} }
      });
      // @ts-ignore
      const csvState = yield select(restapiState);

      if (getError(csvState, uri, method, mappedID)) {
        const errorMessage = get(csvState, [uri, method, mappedID, "data", "message"], undefined);
        if (errorMessage) {
          notification.error({ message: errorMessage });
        }
        return;
      }

      const tableData = get(csvState, [uri, "list", mappedID, "data", "records"], []);
      mappedData = [...mappedData, ...tableData];
      if (page === 0) {
        // this is the first page, so set the metadata stuff correctly here
        const metadata: any = get(csvState, [uri, "list", mappedID, "data", "_metadata"], {});
        totalPages = Math.ceil(metadata.total_count / filters.page_size);
      }
      page += 1;
    }

    const sectionColumns: any[] = [];
    const modifiedData = (mappedData || []).map((record: any) => {
      const { org_name, score, section_responses, full_name } = record;

      const sectionData = (section_responses || []).reduce((acc: any, next: any) => {
        sectionColumns.push({
          key: next.name,
          titleForCSV: next.name
        });
        return {
          ...acc,
          [next.name]: next.score
        };
      }, {});

      if (full_name) {
        return {
          full_name,
          score,
          ...sectionData
        };
      }

      return {
        org_name,
        score,
        ...sectionData
      };
    });
    const allColumns = uniqBy([...columns, ...sectionColumns], "key");
    const headers = allColumns.map((col: any) => get(TRELLIS_SECTION_MAPPING, [col.titleForCSV], col.titleForCSV));
    csvText = headers.join(",").concat("\n");
    const csvData = transformer?.({ apiData: modifiedData, columns: allColumns, filters });
    csvText = csvText.concat(csvData.join("\n")).concat("\n");
    // rename dev_productivity to trellis_profile
    const fileName = uri.replace("dev_productivity_", "").replace("score", "trellis_score").concat("-drilldown.csv");
    const file = new File([csvText], fileName, { type: "text/csv;charset=utf-8" });
    saveAs(file);
  } catch (e) {
    handleError({
      bugsnag: {
        message: (e as any)?.message,
        severity: severityTypes.ERROR,
        context: issueContextTypes.TRELLIS,
        data: { e, action }
      }
    });
  }
}

export function* devProductivityCSVDownloadWatcherSaga() {
  yield takeLatest([DEV_PRODUCTIVITY_CSV_DOWNLOAD], devProductivityCSVDownloadEffectSaga);
}
