import * as actions from "../actionTypes";
import {
  DASHBOARD_WIDGET_ADD,
  DASHBOARD_WIDGET_CLONE,
  DASHBOARD_WIDGET_REMOVE,
  DASHBOARD_WIDGET_REORDER,
  DASHBOARD_WIDGET_RESET,
  DASHBOARD_WIDGET_SET_ORDER,
  DASHBOARD_WIDGET_UPDATE,
  DASHBOARD_WIDGET_BULK_UPDATE,
  CLEAR_WIDGET_HISTORY,
  REVERT_WIDGET_CHANGES,
  MAKE_WIDGETS_REVERSIBLE,
  MULTI_REPORT_WIDGET_REPORT_ADD,
  MULTI_REPORT_WIDGET_REPORT_DELETE,
  COPY_WIDGET_TO_DASHBOARD,
  DASHBOARD_TIME_UPDATE,
  DASHBOARD_TIME_RANGE_TOGGLE
} from "../actionTypes";
import { RestWidget } from "../../../classes/RestDashboards";
import { WidgetOrderConfig } from "../../../dashboard/components/rearrange-grid/helper";
import { MULTI_REPORT_WIDGET_REPORT_NAME_UPDATE } from "../actionTypes";
import { COPY_DESTINATION_DASHBOARD_NODE } from "dashboard/constants/filter-key.mapping";
import { reportCSVDownloadActionType } from "reduxConfigs/sagas/saga-types/genericReportCSVDownloadSaga.types";
import { string } from "prop-types";

export const dashboardWidgetAdd = (dashboardId: string, widget: any) => ({
  type: DASHBOARD_WIDGET_ADD,
  dashboardId,
  widget
});

export const widgetDelete = (widgetId: string) => ({
  type: DASHBOARD_WIDGET_REMOVE,
  widgetId
});

export const widgetsReOrder = (dashboardId: string, widgetType: string, widgetOrderList: WidgetOrderConfig[]) => ({
  type: DASHBOARD_WIDGET_REORDER,
  dashboardId,
  widgetType,
  widgetOrderList
});

export const resetWidgetsOrder = (dashboardId: string) => ({
  type: DASHBOARD_WIDGET_RESET,
  dashboardId
});

export const setWidgetOrder = (dashboardId: string, widgetId: string, order: number) => ({
  type: DASHBOARD_WIDGET_SET_ORDER,
  dashboardId,
  widgetId,
  order
});

export const widgetAddCall = (dashboardId: string, widget: RestWidget, complete: string | null = null) => ({
  type: actions.RESTAPI_WRITE,
  payload: {
    dashboard_id: dashboardId,
    widget
  },
  setState: {
    creating: true
  },
  method: "generic",
  uri: "widgets",
  id: widget.id,
  function: "_create",
  complete
});

export const _widgetUpdateCall = (dashboardId: string, widget: RestWidget, complete: string | null = null) => ({
  type: actions.RESTAPI_WRITE,
  payload: {
    dashboard_id: dashboardId,
    widget
  },
  setState: {
    updating: true
  },
  method: "generic",
  uri: "widgets",
  id: widget.id,
  function: "_update",
  complete
});

export const widgetDeleteCall = (dashboardId: string, widgetId: string, complete: string | null = null) => ({
  type: actions.RESTAPI_WRITE,
  payload: {
    dashboard_id: dashboardId,
    widget_id: widgetId
  },
  setState: {
    deleting: true
  },
  method: "generic",
  uri: "widgets",
  id: widgetId,
  function: "_delete",
  complete
});

export const widgetBulkDeleteCall = (
  dashboardId: string,
  widgetId: string,
  widgetIds: string[],
  complete: string | null = null
) => ({
  type: actions.RESTAPI_WRITE,
  payload: {
    dashboard_id: dashboardId,
    widget_ids: widgetIds
  },
  setState: {
    deleting: true
  },
  method: "generic",
  uri: "widgets",
  function: "deleteBulk",
  id: widgetId,
  complete
});

//form will be the object , where object keys will be the fields
//to update in the widget and the object values will be value
//of that updating field

export const multiReportWidgetReportAdd = (widgetId: string, value: string) => ({
  type: MULTI_REPORT_WIDGET_REPORT_ADD,
  widgetId,
  selected_report: value
});

export const multiReportWidgetReportDelete = (widgetId: string, value: string) => ({
  type: MULTI_REPORT_WIDGET_REPORT_DELETE,
  widgetId,
  deleted_report: value
});

export const multiReportWidgetReportNameUpdate = (widgetId: string, value: string) => ({
  type: MULTI_REPORT_WIDGET_REPORT_NAME_UPDATE,
  widgetId,
  report_name: value
});

/**
 * @deprecated
 * @param dashboardId
 * @param widgetId
 * @param form
 */
export const _widgetUpdate = (dashboardId: string, widgetId: string, form: any) => ({
  type: DASHBOARD_WIDGET_UPDATE,
  widgetId,
  form
});

/**
 * @param widgetId
 * @param form
 */
export const widgetUpdate = (widgetId: string, form: any) => ({
  type: DASHBOARD_WIDGET_UPDATE,
  widgetId,
  form
});

export const widgetDrilldownColumnsUpdate = (widgetId: string, drilldownColumn: string[] | undefined) => ({
  type: actions.WIDGET_DRILLDOWN_COLUMNS_UPDATE,
  widgetId,
  drilldownColumn
});

export const widgetSelectedColumnsUpdate = (widgetId: string, selectedColumns: string[] | undefined) => ({
  type: actions.WIDGET_SELECTED_COLUMNS_UPDATE,
  widgetId,
  selectedColumns
});

export const widgetTableFiltersUpdate = (widgetId: string, filters: any) => ({
  type: actions.WIDGET_TABLE_FILTERS_UPDATE,
  widgetId,
  filters
});

export const dashboardWidgetClone = (dashboardId: string, widgetId: string, cloneWidgetId: string) => ({
  type: DASHBOARD_WIDGET_CLONE,
  dashboardId,
  widgetId,
  cloneWidgetId
});

export const widgetBulkUpdate = (dashboardId: string, filters: any) => ({
  type: DASHBOARD_WIDGET_BULK_UPDATE,
  dashboardId,
  filters
});

export const makeWidgetsReversible = (entityIds: string[]) => ({
  type: MAKE_WIDGETS_REVERSIBLE,
  data: entityIds
});

export const revertWidgets = (dashboardId: string) => ({
  type: REVERT_WIDGET_CHANGES,
  data: { dashboard_id: dashboardId }
});

export const clearWidgetsHistory = () => ({
  type: CLEAR_WIDGET_HISTORY
});

// this actions can be used when specific widget is to be updated remotely
export const remoteWidgetUpdate = (widgetId: string, widgetForm: any) => ({
  type: actions.DASHBOARD_REMOTE_WIDGET_UPDATE,
  widgetId: widgetId,
  form: widgetForm
});

export const copyWidgetToDashboard = (dashboardId: string, widgetId: string) => ({
  type: COPY_WIDGET_TO_DASHBOARD,
  payload: { dashboard_id: dashboardId, widget_id: widgetId, uri: COPY_DESTINATION_DASHBOARD_NODE }
});

export const dashboardTimeUpdate = (dashboardId: string, filters: any) => ({
  type: DASHBOARD_TIME_UPDATE,
  dashboardId,
  filters
});

/**
 * Action to be dispatched for report CSV download Saga
 */

export const widgetCSVDownloadAction: (
  dashboardId: string,
  widgetId: string,
  queryParam?: any
) => reportCSVDownloadActionType = (dashboardId: string, widgetId: string, queryParam?: any) => ({
  type: actions.REPORT_CSV_DOWNLOAD,
  widgetId,
  dashboardId,
  queryParam
});

export const filterWidgetsCustomFields = () => ({
  type: actions.FILTER_WIDGETS_CUSTOM_FIELDS
});

export const widgetMetaDataUpdate = (widgetId: string, value: any) => ({
  type: actions.WIDGET_METADATA_FIELD_UPDATE,
  widgetId,
  value
});
