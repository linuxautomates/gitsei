import { cloneDeep, get } from "lodash";
import { RestDashboard, RestWidget } from "classes/RestDashboards";
import {
  EXCLUDE_SUB_COLUMNS_FOR,
  REPORT_CSV_DOWNLOAD_CONFIG,
  STORE_ACTION,
  SUB_COLUMNS_TITLE
} from "dashboard/constants/bussiness-alignment-applications/constants";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { ReportCSVDownloadConfig } from "dashboard/dashboard-types/Dashboard.types";
import { put, select, takeLatest } from "redux-saga/effects";
import { REPORT_CSV_DOWNLOAD } from "reduxConfigs/actions/actionTypes";
import { selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { combineAllFilters } from "shared-resources/containers/widget-api-wrapper/helper";
import { reportCSVDownloadActionType } from "../saga-types/genericReportCSVDownloadSaga.types";
import { restapiState } from "reduxConfigs/selectors/restapiSelector";
import { EffortType } from "dashboard/constants/enums/jira-ba-reports.enum";
import {
  activeEffortEIEngineerReport,
  AZURE_CUSTOM_FIELDS_LIST,
  completedEffortEIEngineerReport,
  JIRA_CUSTOM_FIELDS_LIST,
  jiraReleaseTableCsvReport
} from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { issueContextTypes, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { JIRA_MANAGEMENT_TICKET_REPORT } from "dashboard/constants/applications/names";

export function* genericReportCSVDownloadEffectSaga(action: reportCSVDownloadActionType): any {
  const { widgetId, queryParam } = action;
  try {
    const widget: RestWidget = yield select(getWidget, { widget_id: widgetId });
    const dashboard: RestDashboard = yield select(selectedDashboard);
    const report = widget?.type;
    const integrationIds = get(dashboard, ["query", "integration_ids"], []);

    const integrationKey = integrationIds.length ? integrationIds.sort().join("_") : "0";

    const reportCSVConfig: ReportCSVDownloadConfig = getWidgetConstant(report, REPORT_CSV_DOWNLOAD_CONFIG);

    const azureFieldsSelector = yield select(getGenericRestAPISelector, {
      uri: AZURE_CUSTOM_FIELDS_LIST,
      method: "list",
      uuid: integrationKey
    });

    const jiraFieldsSelector = yield select(getGenericRestAPISelector, {
      uri: JIRA_CUSTOM_FIELDS_LIST,
      method: "list",
      uuid: integrationKey
    });

    /**
     * handling widget filters through combining all
     * types of filters then transforming specific to report needs
     */
    const widgetFilters = getWidgetConstant(report, ["filter"], {});
    const queryFilters = widget?.query;
    const hiddenFilters = getWidgetConstant(report, ["hidden_filters"], {});
    const globalFilters = dashboard?.query ?? {};
    const combinedFilters = combineAllFilters(widgetFilters, cloneDeep(queryFilters), hiddenFilters || {});
    const application = getWidgetConstant(report, ["application"], "");
    let finalFilters: { [x: string]: any } = {
      filter: {
        ...combinedFilters,
        ...globalFilters
      }
    };
    const widgetFinalFilters = reportCSVConfig?.widgetFiltersTransformer?.(finalFilters, {
      widgetMetadata: widget?.metadata ?? {},
      dashboardMetadata: dashboard?.metadata ?? {},
      uri: widget?.uri,
      report,
      application,
      supportedCustomFields:
        application === IntegrationTypes.JIRA
          ? get(jiraFieldsSelector, "data", [])
          : get(azureFieldsSelector, "data", []),
      queryParam,
      widgetId
    });

    /** handling dynamic uri */
    let apiUrl = widget?.uri;
    if (reportCSVConfig?.widgetDynamicURIGetFunc) {
      apiUrl = reportCSVConfig.widgetDynamicURIGetFunc(widgetFinalFilters ?? {}, widget?.metadata ?? {}, report);
    }

    /** handling apiCall */

    /** @todo replace logic of this code peace with more generic logic */
    const storeAction = getWidgetConstant(report, [STORE_ACTION]);

    const effortType = get(widget?.metadata ?? {}, ["effort_type"], undefined);

    if (effortType) {
      if (effortType === EffortType.COMPLETED_EFFORT) {
        yield put(completedEffortEIEngineerReport(widgetId, widgetFinalFilters, apiUrl, { application }));
      } else {
        yield put(activeEffortEIEngineerReport(widgetId, widgetFinalFilters, apiUrl, { application }));
      }
    } else if (widget?.type === JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT) {
      yield put(jiraReleaseTableCsvReport(widgetId, widgetFinalFilters, apiUrl, { application }));
    } else {
      yield put(
        storeAction(apiUrl, widget?.method, widgetFinalFilters, widgetId, {
          dashboardMetaData: dashboard?.metadata ?? {},
          widgetMetaData: widget?.metadata ?? {},
          application
        })
      );
    }

    const restState = yield select(restapiState);
    const apiData = get(restState, [apiUrl, widget?.method, widgetId, "data", "records"], []);

    /** handling CSV columns */
    const columns = reportCSVConfig.widgetCSVColumnsGetFunc?.(apiData) ?? [];

    /** handling CSV formation */
    let csvText = columns
      .map(column => column.title)
      .join(",")
      .concat("\n");
    const subColumns = reportCSVConfig?.[SUB_COLUMNS_TITLE] || [];

    if (subColumns?.length) {
      const excludeSubColumns = reportCSVConfig?.[EXCLUDE_SUB_COLUMNS_FOR] || [];
      csvText = columns
        .map((column: any) => {
          return excludeSubColumns.includes(column.title)
            ? column.title
            : subColumns.map(_col => `${column.title} ${_col}`);
        })
        .join(",")
        .concat("\n");
    }
    const csvData = reportCSVConfig.widgetCSVDataTransform?.(apiData, columns);
    csvText = csvText.concat((csvData ?? []).join("\n")).concat("\n");
    const file = new File([csvText], `${widget?.name ?? "widget"}.csv`, { type: "text/csv;charset=utf-8" });
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

export function* genericReportCSVTransformWatcher() {
  yield takeLatest([REPORT_CSV_DOWNLOAD], genericReportCSVDownloadEffectSaga);
}
