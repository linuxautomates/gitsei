import React, { useState } from "react";
import { Button, Divider, Form, Icon, Input, Modal, Typography } from "antd";

interface PresetColumnTypeModalProps {
  visible: boolean;
  onOk: (options: Array<string>) => void;
  onCancel: () => void;
}

const PresetColumnTypeModal: React.FC<PresetColumnTypeModalProps> = (props: PresetColumnTypeModalProps) => {
  const { visible, onOk, onCancel } = props;
  const [options, setOptions] = useState<Array<string>>([]);
  const [newOption, setNewOption] = useState<string>("");

  const addOption = (value: string) => {
    if (!options.includes(value)) {
      setOptions([...options, value]);
    }
    setNewOption("");
  };

  const removeOption = (value: string) => {
    setOptions(options.filter(option => option !== value));
  };

  const cancel = () => {
    setOptions([]);
    onCancel();
  };

  const save = () => {
    setOptions([]);
    onOk(options);
  };

  return (
    <Modal
      title={"Preset"}
      visible={visible}
      onCancel={cancel}
      footer={
        <>
          <Button onClick={cancel}>Cancel</Button>
          <Button type={"primary"} onClick={save} disabled={options.length === 0}>
            Save
          </Button>
        </>
      }>
      <Form layout={"vertical"}>
        <Form.Item required label={"Add Options"}>
          <Input
            style={{ width: "80%", marginRight: "0.5em" }}
            value={newOption}
            onPressEnter={() => addOption(newOption)}
            onChange={e => setNewOption(e.target.value)}
          />
          <Button icon={"plus"} onClick={() => addOption(newOption)}>
            Add
          </Button>
        </Form.Item>
      </Form>
      <div style={{ padding: "1em" }}>
        {options.map((option: any, index: number) => {
          return (
            <React.Fragment key={index}>
              <div
                style={{
                  display: "flex",
                  justifyContent: "space-between",
                  alignItems: "center",
                  padding: "0.3em"
                }}>
                <Typography.Text>{option}</Typography.Text>
                <Icon type={"close"} onClick={() => removeOption(option)} />
              </div>
              <Divider style={{ margin: "0.5em 0" }} />
            </React.Fragment>
          );
        })}
      </div>
    </Modal>
  );
};

export default PresetColumnTypeModal;
