import React, { useCallback } from "react";
import { useDispatch } from "react-redux";
import { Icon, Switch } from "antd";
import cx from "classnames";

import "./rearrange-widget.style.scss";
import { RestWidget } from "../../../classes/RestDashboards";
import { CacheWidgetPreview } from "../../pages/context";
import { widgetUpdate } from "reduxConfigs/actions/restapi";
import WidgetPreviewWrapperComponent from "../../../configurable-dashboard/components/widget-preview/widget-preview-wrapper.component";
import { AntTag, AntText } from "../../../shared-resources/components";

interface RearrangeWidgetProps {
  layoutInfo: any;
  className: string;
}

const RearrangeWidgetComponent: React.FC<RearrangeWidgetProps> = ({ layoutInfo, className }) => {
  const dispatch = useDispatch();

  const widget: RestWidget = layoutInfo.data;

  const updateWidth = useCallback(
    (checked: boolean) => {
      const _width = checked ? "full" : "half";
      dispatch(widgetUpdate(widget.id, { ...widget.json, metadata: { width: _width } }));
    },
    [widget]
  );

  const renderOptionToSetWidth = () => {
    if (!widget.isWidthConfigurable) {
      return null;
    }
    return (
      <div className="pt-10 d-flex justify-content-between">
        <AntText>Full-width</AntText>
        <Switch checked={widget.width === "full"} onChange={updateWidth} />
      </div>
    );
  };

  return (
    <div className={cx("rearrange-widget", { "draft-widget": widget?.draft }, className)} key={layoutInfo.i}>
      <div className="rearrange-widget__header">
        <div className="rearrange-widget__header--count">
          {widget?.name}
          {widget?.draft && <AntTag color="#fcb25f">New</AntTag>}
        </div>
        <div>
          <Icon type="drag" className="rearrange-widget__header--icon" />
        </div>
      </div>
      <CacheWidgetPreview.Provider value>
        <WidgetPreviewWrapperComponent widgetId={widget?.id} dashboardId={widget?.dashboard_id} previewOnly />
      </CacheWidgetPreview.Provider>
      {renderOptionToSetWidth()}
    </div>
  );
};

export default RearrangeWidgetComponent;
