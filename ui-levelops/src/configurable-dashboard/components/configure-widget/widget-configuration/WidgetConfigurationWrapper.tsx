import React, { useCallback, useMemo, useState, useContext } from "react";

import "./WidgetConfigurationWrapper.scss";
import { AntCol, AntIcon, AntRow, AntText } from "../../../../shared-resources/components";
import WidgetPreviewWrapperComponent from "../../widget-preview/widget-preview-wrapper.component";
import { RestWidget } from "../../../../classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import EditWidgetModal from "../modal/EditWidgetModal";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { opacity } from "html2canvas/dist/types/css/property-descriptors/opacity";

interface WidgetConfigurationWrapperProps {
  dashboardId: string;
  widgetId: string;
  children: any;
}

const WidgetConfigurationWrapper: React.StatelessComponent<WidgetConfigurationWrapperProps> = ({
  dashboardId,
  widgetId,
  children
}) => {
  const [showEditWidgetModal, setEditWidgetModalVisibility] = useState(false);

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  const closeEditWidgetModal = useCallback(() => {
    setEditWidgetModalVisibility(false);
  }, []);

  const editWidgetModal = useMemo(() => {
    if (!showEditWidgetModal) {
      return null;
    }
    return <EditWidgetModal widgetId={widgetId} onClose={closeEditWidgetModal} />;
  }, [showEditWidgetModal]);

  const handleSettingClick = useCallback(() => setEditWidgetModalVisibility(true), []);

  return (
    <AntRow className="configure-widget-page h-100">
      <AntCol span={14} className="left-container h-100">
        <div className="d-flex justify-start align-center">
          <AntText className="widget-name">{widget?.name || ""}</AntText>
          <div className="edit-name-container" onClick={handleSettingClick}>
            <AntText className="name-discription"> Edit Name and Description</AntText>
            <AntIcon className="edit-icon" type="edit" theme="outlined" />
          </div>
        </div>
        <div>
          <WidgetPreviewWrapperComponent widgetId={widgetId} dashboardId={dashboardId} configurePreview={true} />
        </div>
      </AntCol>
      <AntCol span={10} className="right-container h-100">
        {children}
      </AntCol>
      {editWidgetModal}
    </AntRow>
  );
};

export default WidgetConfigurationWrapper;
