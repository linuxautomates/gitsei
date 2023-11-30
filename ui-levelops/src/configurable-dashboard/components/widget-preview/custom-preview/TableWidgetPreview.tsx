import React from "react";

import "./EmptyWidgetPreview.scss";
import WidgetTableIcon from "assets/svg/widget-table-preview.svg";

interface TableWidgetPreviewProps {}

const TableWidgetPreview: React.StatelessComponent<TableWidgetPreviewProps> = () => (
  <div className="empty-preview-state">
    <WidgetTableIcon />
  </div>
);

export default TableWidgetPreview;
