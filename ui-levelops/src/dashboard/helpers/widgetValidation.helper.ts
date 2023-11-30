import { RestWidget } from "classes/RestDashboards";
import { LEVELOPS_TABLE_MULTI_STACK_ERROR } from "dashboard/constants/applications/levelops-constant";
import { WIDGET_VALIDATION_FUNCTION } from "dashboard/constants/filter-name.mapping";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { get } from "lodash";

// this helps in disabling save/place widget button if some filter/condition is required
export const widgetValidationHelper = (widget: RestWidget) => {
  if (!widget || !widget?.type) return false;
  const report = widget.type || "";
  const widgetValidationFunction = getWidgetConstant(report, WIDGET_VALIDATION_FUNCTION);

  if (widgetValidationFunction) {
    return widgetValidationFunction(widget);
  }
  return true;
};

export const hygieneWeightValidationHelper = (payload: any) => {
  const weights = get(payload, ["metadata", "weights"], {});
  if (Object.keys(weights).length) {
    let total_weight = 0;
    Object.keys(weights).forEach(category => {
      total_weight += weights[category];
    });
    if (total_weight > 100) {
      return false;
    }
  }
  return true;
};

export const issuesSingleStatValidationHelper = (payload: any) => {
  const across = get(payload, ["query", "across"], undefined);

  return !!(
    ["trend", ""].includes(across) ||
    [
      "issue_created",
      "issue_resolved",
      "issue_due",
      "issue_updated",
      "issue_due_relative",
      "workitem_resolved_at",
      "workitem_created_at",
      "workitem_due_at",
      "workitem_updated_at"
    ].includes(across)
  );
};

export const jiraBaValidationHelper = (payload: any) => {
  const ticketCategorization = get(payload, ["query", "ticket_categorization_scheme"], undefined);
  return !!ticketCategorization;
};

export const levelOpsTableReportValidatorFunction = (payload: RestWidget) => {
  const tableId = payload?.tableId;
  const { xAxis, yAxis, groupBy } = payload?.metadata;
  const validYAxis = (yAxis || []).filter(
    (item: { value: any; key: any; label?: string }) => !!item?.value && !!item?.key && !!item?.label
  );
  const isAddedMultiStack = get(payload, ["metadata", "stackBy"], [])?.length > 1;
  const saveWidget = !!tableId && !!xAxis && (groupBy || validYAxis?.length > 0) && !isAddedMultiStack;

  return { saveWidget, errorMessage: LEVELOPS_TABLE_MULTI_STACK_ERROR };
};
