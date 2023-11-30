import React, { useState, useCallback } from "react";
import { StatusWithIcon } from "shared-resources/components";
import { Tooltip } from "antd";
import ErrorDetailsModalContainer from "./error-details-modal.container";

interface ErrorDetailsContainerProps {
  integrationObj: Object;
  status: "success" | "failed" | "pending" | "warning" | "scheduled" | "failure";
  text: {
    success?: string;
    pending?: string;
    failed?: string;
    scheduled?: string;
    warning?: string;
    failure?: string;
  };
}

const ErrorDetailsContainer: React.FC<ErrorDetailsContainerProps> = ({ integrationObj, status, text }) => {
  const [showErrorModal, setShowErrorModal] = useState<boolean>(false);

  const triggerErrorModal = useCallback(() => {
    setShowErrorModal(true);
  }, []);

  const onModalClose = useCallback(() => {
    setShowErrorModal(false);
  }, []);

  let errorMsg = (integrationObj as any).error?.message || "Not available";
  let modalLinkTrigger =
    ((integrationObj as any).error?.stacktrace || []).length > 0 ? (
      <a onClick={triggerErrorModal} style={{ fontWeight: "500", fontSize: "13px" }}>
        Click to view error details
      </a>
    ) : null;

  let errorContainerComponent = (
    <div>
      <div>Message: {errorMsg}</div>
      <div style={{ marginTop: "20px" }}>{modalLinkTrigger}</div>
    </div>
  );

  return (
    <Tooltip title={errorContainerComponent} overlayStyle={{ zIndex: 1, minWidth: "450px", borderRadius: "8px" }}>
      <span>
        <StatusWithIcon status={status} text={text} />
        <ErrorDetailsModalContainer
          visible={showErrorModal}
          onClose={onModalClose}
          message={errorMsg}
          stacktrace={(integrationObj as any).error?.stacktrace || []}
        />
      </span>
    </Tooltip>
  );
};

export default React.memo(ErrorDetailsContainer);
