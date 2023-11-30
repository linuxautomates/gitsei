import { put, select, takeLatest } from "redux-saga/effects";
import { get } from "lodash";

import { SET_REPORTS, WIDGET_REPORTS_LIST } from "reduxConfigs/actions/actionTypes";
import { listReportDocs } from "../../actions/restapi/reportDocs.action";
import CompactReport from "../../../model/report/CompactReport";
import { restReportList } from "../../actions/widgetLibraryActions";
import {
  getAllCompactReports,
  mapWidgetLibraryApiList,
  removeFEMiscReportPresentInAPIData
} from "../../../dashboard/pages/explore-widget/reportHelper";
import { reportDocsListSelector } from "../../selectors/widgetReportDocsSelector";
import { IS_FRONTEND_REPORT } from "../../../dashboard/constants/filter-key.mapping";

function* appendReportDocsIntoReports(action: { type: string }): any {
  const reportDocsState = yield select(reportDocsListSelector);

  const loading = get(reportDocsState, ["loading"], true);
  const error = get(reportDocsState, ["error"], true);
  if (!loading && !error) {
    const data = get(reportDocsState, ["data", "records"], []);
    const allFrontendReports = getAllCompactReports();
    let additionalMiscReports: any = allFrontendReports
      .filter(report => !!report?.[IS_FRONTEND_REPORT])
      .map(report => ({
        ...report,
        id: report?.key,
        "report-categories": report.categories ? report.categories : ["Miscellaneous"]
      }));

    // FRISKING POINT: TO MAKE SURE THAT THE FRONT END REPORTS ARE NOT ALREADY UPDATED AT BACKEND
    additionalMiscReports = removeFEMiscReportPresentInAPIData(additionalMiscReports, data);

    const mappedData: any[] = mapWidgetLibraryApiList([...data, ...additionalMiscReports]);

    yield put(restReportList(mappedData));
  }
}

function* listReportSaga(action: { type: string }) {
  try {
    yield put(listReportDocs({ filter: {} }, SET_REPORTS));
  } catch (e) {
    console.error("Failed to list reports.", e);
  }
}

export function* reportListSagaWatcher() {
  yield takeLatest(WIDGET_REPORTS_LIST, listReportSaga);
  yield takeLatest(SET_REPORTS, appendReportDocsIntoReports);
}
