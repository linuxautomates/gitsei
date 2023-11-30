import React, { useEffect, useState } from "react";
import { DatePicker, Input, Modal, Select } from "antd";
import { AntButton, AntText } from "../shared-resources/components";
import "./DefaultValueModal.style.scss";

const { Option } = Select;

interface DefaultValueModalProps {
  visible: boolean;
  onOk: (defaultValue: string) => void;
  onCancel: () => void;
  itemKey: string;
  presetValue?: string;
  defaultRequired: boolean;
}

const DefaultValueModal: React.FC<DefaultValueModalProps> = (props: DefaultValueModalProps) => {
  const { visible, onCancel, onOk, itemKey, defaultRequired } = props;
  const [defaultValue, setDefaultValue] = useState<string>("");

  useEffect(() => setDefaultValue(props.presetValue || ""), [props]);

  const getInputType = () => {
    return itemKey && itemKey.split("_")[2];
  };

  const onClose = () => {
    setDefaultValue("");
    onCancel();
  };

  const onSet = () => {
    onOk(defaultValue);
  };

  const renderField = () => {
    const type = getInputType();

    switch (type) {
      case "boolean":
        return (
          <Select
            className={defaultRequired && !defaultValue ? "selectError" : ""}
            style={{ width: "100%" }}
            value={defaultValue}
            showArrow={false}
            onBlur={(value: any) => setDefaultValue(value)}
            onChange={(value: any) => setDefaultValue(value)}>
            <Option value={"False"}>False</Option>
            <Option value={"True"}>True</Option>
          </Select>
        );
      case "date":
        return (
          <DatePicker
            className={defaultRequired && !defaultValue ? "dateError" : ""}
            allowClear={false}
            style={{ width: "100%" }}
            defaultValue={null}
            onChange={(date, dateString) => setDefaultValue(dateString)}
            format={"MM/DD/YYYY"}
          />
        );
      default:
        return (
          <Input
            value={defaultValue}
            style={{ width: "100%", borderColor: defaultRequired && !defaultValue ? "red" : "" }}
            onChange={e => setDefaultValue(e.target.value)}
            required={defaultRequired}
          />
        );
    }
  };

  return (
    <Modal
      title={"Set Default Value"}
      visible={visible}
      closable={!defaultRequired}
      onCancel={onClose}
      footer={[
        <AntButton key="back" type="secondary" disabled={defaultRequired} onClick={onClose}>
          Cancel
        </AntButton>,
        <AntButton key="submit" type="primary" disabled={defaultRequired && !defaultValue} onClick={onSet}>
          Set
        </AntButton>
      ]}
      className={"defaultValueModal"}
      onOk={onSet}>
      <div style={{ margin: "1rem 0 0" }}>{renderField()}</div>
      {defaultRequired && !defaultValue && (
        <AntText type="danger">Default value is required when column is Required and Read-Only</AntText>
      )}
    </Modal>
  );
};

export default DefaultValueModal;
