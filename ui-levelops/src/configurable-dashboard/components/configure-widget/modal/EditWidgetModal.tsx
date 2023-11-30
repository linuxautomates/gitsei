import React, { useCallback, useMemo } from "react";
import { useDispatch } from "react-redux";

import "./EditWidgetModal.scss";
import WidgetDetailsModal from "./WidgetDetailsModal";
import { RestWidget } from "../../../../classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { widgetUpdate } from "reduxConfigs/actions/restapi";
import { dashboardWidgetAdd, multiReportWidgetReportAdd } from "reduxConfigs/actions/restapi";

interface EditWidgetModalProps {
  widgetId: string;
  onClose: () => void;
}

const EditWidgetModal: React.FC<EditWidgetModalProps> = ({ widgetId, onClose }) => {
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const dispatch = useDispatch();

  const handleSave = useCallback(
    (name: string, description: string) => {
      widget.name = name;
      widget.description = description;
      dispatch(widgetUpdate(widgetId, widget.json));
      onClose();
    },
    [widget]
  );

  const title = useMemo(() => {
    return widgetId ? "Edit Widget" : "Create Widget";
  }, []);

  const widgetDetail: any = useMemo(() => {
    return widgetId ? widget : { name: "", description: "" };
  }, []);
  return (
    <WidgetDetailsModal
      widget={widgetDetail}
      title={title}
      onSave={handleSave}
      onCancel={onClose}
      hideGraphConfiguration
    />
  );
};

export default React.memo(EditWidgetModal);
