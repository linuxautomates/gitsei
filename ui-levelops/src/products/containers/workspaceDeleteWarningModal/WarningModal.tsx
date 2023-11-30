import React from "react";
import { Modal } from "antd";
import { AntButton, AntIcon } from "shared-resources/components";

interface DeleteWarningModalProps {
  showWarning: boolean;
  handleOk: () => void;
}

const DeleteWarningModal: React.FC<DeleteWarningModalProps> = ({ showWarning, handleOk }) => (
  <Modal
    wrapClassName="integration-delete-confirm-popup"
    footer={[
      <AntButton key="ok" type="primary" onClick={handleOk}>
        OK
      </AntButton>
    ]}
    visible={showWarning}
    onOk={handleOk}
    okText="Ok">
    <div className="header-wrap">
      <AntIcon type="delete" />
      <span className="confirmation-heading">
        Deleting a project is a restricted action. Contact SEI Customer Support for assistance.
      </span>
    </div>
  </Modal>
);

export default DeleteWarningModal;
