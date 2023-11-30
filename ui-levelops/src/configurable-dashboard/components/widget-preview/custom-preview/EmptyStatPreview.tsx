import React from "react";

import "./EmptyWidgetPreview.scss";
import emptyWidgetIcon from "../../../../assets/svg/widget-no-data.svg";
import { AntText } from "../../../../shared-resources/components";

interface EmptyStatPreviewProps {
  message?: string;
}

const EmptyStatPreview: React.StatelessComponent<EmptyStatPreviewProps> = ({ message }) => (
  <div className="empty-preview-state"></div>
);

export default EmptyStatPreview;
