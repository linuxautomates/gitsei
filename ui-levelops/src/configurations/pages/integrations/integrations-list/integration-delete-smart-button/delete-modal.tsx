import React from "react";
import { Modal } from "antd";
import { AntIcon } from "shared-resources/components";

interface DeleteModalProps {
  showConfirmModal: boolean;
  handleOk: () => void;
  clearState: () => void;
  ouLoading: boolean;
  confirmPopupContent: JSX.Element | null;
}

const DeleteModal: React.FC<DeleteModalProps> = ({
  showConfirmModal,
  handleOk,
  clearState,
  ouLoading,
  confirmPopupContent
}) => (
  <Modal
    wrapClassName="integration-delete-confirm-popup"
    visible={showConfirmModal}
    onOk={handleOk}
    onCancel={clearState}
    okText="Yes"
    okButtonProps={{
      disabled: ouLoading
    }}
    cancelText="No">
    <div className="header-wrap">
      <AntIcon type="delete" />
      <span className="confirmation-heading">Do you want to delete this integration?</span>
    </div>
    <div className="content-wrap">{confirmPopupContent}</div>
  </Modal>
);

export default DeleteModal;
