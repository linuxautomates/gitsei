import React, { useCallback, useMemo, useState } from "react";
import { AntModal, RichEditor } from "../../../../shared-resources/components/index";
import { useDispatch } from "react-redux";
import { getDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { widgetUpdate } from "reduxConfigs/actions/restapi/widgetActions";
import { RestWidget } from "../../../../classes/RestDashboards";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { newDashboardUpdate } from "reduxConfigs/actions/restapi/index";

import "./dashboard-view-modals.style.scss";

interface DashboardNotesProps {
  visible: boolean;
  onClose: () => void;
  widgetId: string;
  dashboardId: string;
}

export const DashboardNotesEditModal: React.FC<DashboardNotesProps> = ({ visible, onClose, widgetId, dashboardId }) => {
  const dispatch = useDispatch();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const dashboard = useParamSelector(getDashboard, { dashboard_id: dashboardId });

  const [content, setContent] = useState(widget.description || "");

  const onChange = (value: any) => {
    setContent(value);
  };

  const handleSave = useCallback(() => {
    widget.description = content;
    dispatch(widgetUpdate(widgetId, widget.json));
    dispatch(newDashboardUpdate(dashboardId, dashboard.json));
    onClose();
  }, [widget, content]);

  const title = useMemo(
    () => (
      <>
        <div className="notes-modal-title">Edit Notes</div>
        <div className="notes-modal-sub-title">Enrich the insight by adding descriptive notes about the reports</div>
      </>
    ),
    []
  );

  return (
    <AntModal
      width={850}
      closable={false}
      visible={visible}
      title={title}
      onOk={handleSave}
      onCancel={onClose}
      okText={"Save"}>
      <RichEditor initialValue={widget.description || ""} value={content} onChange={onChange} />
    </AntModal>
  );
};
