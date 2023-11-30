import React from "react";
import { Empty } from "antd";

interface EmptyDashboardProps {
  message: string;
}
const EmptyDashboard: React.FC<EmptyDashboardProps> = (props: EmptyDashboardProps) => {
  return (
    <div className="empty-dashboard">
      <Empty description={props.message} />
    </div>
  );
};

export default EmptyDashboard;
