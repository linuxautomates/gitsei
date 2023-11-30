import { Button, Modal } from "antd";
import React from "react";
import { AntText } from "shared-resources/components";

interface FilterRequiredModalProps {
  showModal: boolean;
  onOK: () => void;
  calculationType: string;
}

const FilterRequiredModal: React.FC<FilterRequiredModalProps> = ({ showModal, onOK, calculationType }) => {
  return (
    <Modal
      visible={showModal}
      closable
      onCancel={onOK}
      title={`${calculationType.toUpperCase()} REQUIRED`}
      footer={[
        <Button type="primary" onClick={onOK}>
          OK
        </Button>
      ]}>
      <div>
        <AntText>{`At least one parameter is required to define ${calculationType}.`}</AntText>
      </div>
      <AntText>This filter can be changed but not deleted.</AntText>
    </Modal>
  );
};

export default FilterRequiredModal;
