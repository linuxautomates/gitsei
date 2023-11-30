import { Empty, notification, Row } from "antd";
import React, { useEffect } from "react";

interface EmptyWidgetProps {
  minHeight?: string;
  className?: string;
  notificationMessage?: string;
  description?: string;
}

const EmptyWidgetComponent: React.FC<EmptyWidgetProps> = ({ minHeight, className, notificationMessage, ...props }) => {
  useEffect(() => {
    if (notificationMessage) {
      notification.error({
        message: notificationMessage
      });
    }
  }, []);
  return (
    <Row align="middle" type="flex" justify="center" className={className} style={{ height: minHeight || "100%" }}>
      <Empty {...props} />
    </Row>
  );
};

export default EmptyWidgetComponent;
