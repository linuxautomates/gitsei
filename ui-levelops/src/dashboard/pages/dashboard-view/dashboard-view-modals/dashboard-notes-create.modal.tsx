import React, { useCallback, useState, useMemo } from "react";
import { AntModal, RichEditor } from "../../../../shared-resources/components/index";
import { useDispatch, useSelector } from "react-redux";
import { dashboardWidgetsSelector, selectedDashboard } from "reduxConfigs/selectors/dashboardSelector";
import { _selectedDashboardIntegrations } from "reduxConfigs/selectors/integrationSelector";
import Widget from "../../../../model/widget/Widget";
import { WebRoutes } from "../../../../routes/WebRoutes";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { dashboardWidgetAdd } from "reduxConfigs/actions/restapi/widgetActions";
import { DashboardNotesReport } from "../constant";

import "./dashboard-view-modals.style.scss";
import { ProjectPathProps } from "classes/routeInterface";

interface DashboardNotesProps {
  visible: boolean;
  onClose: () => void;
}

export const DashboardNotesCreateModal: React.FC<DashboardNotesProps> = ({ visible, onClose }) => {
  const dispatch = useDispatch();
  const history = useHistory();
  const location = useLocation();

  const dashboard = useSelector(selectedDashboard);
  const dashboardIntegrations = useSelector(_selectedDashboardIntegrations);
  const widgets = useParamSelector(dashboardWidgetsSelector, {
    dashboard_id: dashboard?.id
  });
  const projectParams = useParams<ProjectPathProps>();

  const [content, setContent] = useState<string>("");

  const onChange = (value: any) => {
    setContent(value);
  };

  const handleSave = useCallback(() => {
    // used when creating a widget
    if (dashboard) {
      const widget = Widget.newInstance(dashboard, DashboardNotesReport, widgets);
      if (!widget) {
        console.error("Error: Failed to create widget");
        return;
      }

      widget.name = "Documentation";
      widget.description = content;

      dispatch(dashboardWidgetAdd(dashboard.id, widget.json));

      history.push(WebRoutes.dashboard.widgets.widgetsRearrange(projectParams, dashboard.id, location?.search));
    } else {
      console.error("Error: No insight selected");
    }
    onClose();
  }, [content, dashboardIntegrations]);

  const _onClose = useCallback(() => {
    setContent("");
    onClose();
  }, []);

  const title = useMemo(
    () => (
      <>
        <div className="notes-modal-title">Add Notes</div>
        <div className="notes-modal-sub-title">Enrich the insight by adding descriptive notes about the reports</div>
      </>
    ),
    []
  );

  return (
    <AntModal
      closable={false}
      width={850}
      visible={visible}
      title={title}
      onOk={handleSave}
      onCancel={_onClose}
      okText={"Save"}>
      <RichEditor initialValue={""} onChange={onChange} />
    </AntModal>
  );
};
