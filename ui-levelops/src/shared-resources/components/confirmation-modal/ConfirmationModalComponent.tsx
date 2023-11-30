import { Button, Modal } from "antd";
import React from "react";
import WarningTriangle from "shared-resources/assets/svg-icons/warningTriangle.svg";

interface ConfirmationModalComponentProps {
  onOk: () => void;
  onReject?: () => void;
  onCancel: (event: any) => void;
  text: string;
  visiblity: boolean;

  // This modal lets users stay on page.
  // but with this flag, it will save.
  saveMode?: boolean;
}

const ConfirmationModalComponent: React.FC<ConfirmationModalComponentProps> = props => {
  const { saveMode } = props;

  return (
    <Modal
      visible={props.visiblity}
      onOk={props.onOk}
      onCancel={props.onCancel}
      closable={false}
      bodyStyle={{ paddingBottom: 0 }}
      footer={[
        <Button key="back" onClick={props.onReject || props.onCancel}>
          {saveMode ? "Discard Changes" : "No"}
        </Button>,
        <Button key="submit" type={!saveMode ? "primary" : undefined} onClick={props.onOk}>
          {saveMode ? "Save Changes" : "Yes"}
        </Button>
      ]}>
      <div style={{ display: "flex", padding: "2rem", alignItems: "center" }}>
        <WarningTriangle style={{ width: "24px", height: "24px" }} />
        <span style={{ marginLeft: "1.5rem", fontSize: "0.9rem" }}>{props.text}</span>
      </div>
    </Modal>
  );
};

ConfirmationModalComponent.defaultProps = {
  saveMode: false
};

export default ConfirmationModalComponent;
