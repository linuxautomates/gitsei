import React from "react";

import "./EmptyWidgetPreview.scss";
import WidgetNotAvailable from "assets/svg/widget-preview-not-available.svg";
import { AntText } from "../../../../shared-resources/components";

interface CompositeWidgetPreviewProps {}

const CompositeWidgetPreview: React.StatelessComponent<CompositeWidgetPreviewProps> = () => (
  <div className="empty-preview-state">
    <WidgetNotAvailable className="composite-preview-icon mb-15" />
    <AntText>Preview not available</AntText>
  </div>
);

export default CompositeWidgetPreview;
