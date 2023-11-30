import { Form, Modal, Button } from "antd";
import React from "react";
import { SelectRestapi } from "shared-resources/helpers";

interface RecepientsModalProps {
  visible: boolean;
  onCancel: () => void;
  onOk: () => void;
  selectedRecipients: any;
  onUpdateRecipients: (newRecipients: any) => void;
}

const RecepientsModal: React.FC<RecepientsModalProps> = (props: RecepientsModalProps) => {
  return (
    <Modal
      visible={props.visible}
      width={700}
      destroyOnClose={true}
      onCancel={props.onCancel}
      footer={[
        <Button key="back" onClick={props.onCancel}>
          Cancel
        </Button>,
        <Button
          key="submit"
          type="primary"
          disabled={!props.selectedRecipients || props.selectedRecipients.length === 0}
          onClick={props.onOk}>
          Send
        </Button>
      ]}>
      <Form.Item label="Select Recipients" colon={false}>
        <SelectRestapi
          style={{ width: "100%" }}
          value={props.selectedRecipients}
          mode="multiple"
          labelInValue
          createOption={true}
          uri="users"
          searchField="email"
          onChange={(recepients: any) => props.onUpdateRecipients(recepients)}
        />
      </Form.Item>
    </Modal>
  );
};

export default RecepientsModal;
