import React, { useEffect } from "react";

import "./EmptyWidgetPreview.scss";
import EmptyWidgetIcon from "assets/svg/widget-no-data.svg";
import { AntText } from "../../../../shared-resources/components";
import cx from "classnames";
import { notification } from "antd";

interface EmptyWidgetPreviewProps {
  message?: string;
  notificationMessage?: string;
  isStat?: boolean;
}

const EmptyWidgetPreview: React.StatelessComponent<EmptyWidgetPreviewProps> = ({
  message,
  isStat,
  notificationMessage
}) => {
  useEffect(() => {
    if (notificationMessage) {
      notification.error({
        message: notificationMessage
      });
    }
  }, []);
  return (
    <div className="empty-preview-state">
      <EmptyWidgetIcon className={cx("mb-15", { "preview-icon-stat": isStat })} />
      <AntText className={cx({ "preview-icon-info-title": isStat })}>No data to display.</AntText>
      <AntText className={cx({ "preview-icon-info-sub-title": isStat })}>
        {message ?? "Add a report to see preview."}
      </AntText>
    </div>
  );
};

export default EmptyWidgetPreview;
