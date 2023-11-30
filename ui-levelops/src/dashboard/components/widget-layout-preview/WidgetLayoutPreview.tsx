import React, { useCallback, useState, useEffect } from "react";
import { Row } from "antd";
import { AntButton, AntInput, AntText } from "shared-resources/components";
import { useDispatch } from "react-redux";

import "./WidgetLayoutPreview.scss";
import WidgetPreviewWrapperComponent from "../../../configurable-dashboard/components/widget-preview/widget-preview-wrapper.component";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { RestWidget } from "../../../classes/RestDashboards";
import { setWidgetOrder } from "reduxConfigs/actions/restapi/widgetActions";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";

interface WidgetLayoutPreviewProps {
  widgetId: string;
  dashboardId: string;
}

const WidgetLayoutPreview: React.FC<WidgetLayoutPreviewProps> = ({ widgetId, dashboardId }) => {
  const [widgetOrder, _setWidgetOrder] = useState<string>("1");
  const dispatch = useDispatch();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });

  useEffect(() => {
    _setWidgetOrder(widget?.order);
  }, [widget]);

  const handlePlaceClick = useCallback(() => {
    dispatch(setWidgetOrder(dashboardId, widgetId, parseInt(widgetOrder)));
  }, [dashboardId, widgetId, widgetOrder]);

  const handleWidgetOrderChange = useCallback((e: any) => {
    let order = e.target.value;
    order = order.replace("#", "");
    if (isNaN(order)) {
      return;
    }
    //TODO: show error for out of bound values.
    _setWidgetOrder(order);
  }, []);

  if (!widget) {
    return null;
  }

  return (
    <>
      <Row className="row-margin note">
        <AntText className="notice-heading">Note: Drag & Drop</AntText>
        <AntText className="notice-message">
          Please drag and drop graph to make meaningful layout. Or, you can enter a number in which selected graph will
          be placed.
        </AntText>
      </Row>
      {widgetId && (
        <>
          <Row className="row-margin flex direction-column">
            <AntText className="preview-heading">Preview - {widget.name}</AntText>
            <WidgetPreviewWrapperComponent widgetId={widgetId} dashboardId={dashboardId} />
          </Row>
          <Row className="flex direction-column">
            <AntText className="place-heading">Place widget (#{widget.order}) at</AntText>
            <AntInput value={`#${widgetOrder}`} onChange={handleWidgetOrderChange} />
            <AntButton className="place-button" type="primary" onClick={handlePlaceClick}>
              Place
            </AntButton>
          </Row>
        </>
      )}
    </>
  );
};

export default WidgetLayoutPreview;
