import React from "react";
import { StatusWithIcon } from "../constants";
import { default as SuccessStatus } from "../success-status/success-status.component";
import { default as FailedStatus } from "../failed-status/failed-status.component";
import { default as WarningStatus } from "../warning-status/warning-status.component";
import { default as PendingStatus } from "../pending-status/pending-status.component";

interface StatusWithIconProps {
  text?: {
    success?: string;
    pending?: string;
    failed?: string;
    scheduled?: string;
    warning?: string;
    failure?: string;
  };
  status: "success" | "failed" | "pending" | "warning" | "scheduled" | "failure";
}

const StatusWithIconComponent: React.FC<StatusWithIconProps> = ({ text, status }) => {
  switch (status) {
    case StatusWithIcon.SUCCESS:
      return <SuccessStatus text={text?.success} />;
    case StatusWithIcon.FAILED:
    case StatusWithIcon.FAILURE:
      return <FailedStatus text={text?.failed || text?.failure} />;
    case StatusWithIcon.PENDING:
      return <PendingStatus text={text?.pending} />;
    case StatusWithIcon.WARNING:
    case StatusWithIcon.SCHEDULED:
      return <WarningStatus text={text?.warning || text?.scheduled} />;
    default:
      return null;
  }
};

export default React.memo(StatusWithIconComponent);
