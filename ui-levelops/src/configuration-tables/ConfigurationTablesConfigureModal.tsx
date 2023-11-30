import React, { useState } from "react";
import { Form, Input, Modal, Select, Tabs } from "antd";
import { AntCol, AntRow } from "../shared-resources/components";
import { v1 as uuid } from "uuid";
import "./ConfigurationTablesConfigureModal.scss";

const { TabPane } = Tabs;
const { Option } = Select;

interface ConfigurationTablesConfigureModalProps {
  visible: boolean;
  onOk: () => void;
  onCancel: () => void;
}

const ConfigurationTablesConfigureModal: React.FC<ConfigurationTablesConfigureModalProps> = (
  props: ConfigurationTablesConfigureModalProps
) => {
  const [tableName, setTableName] = useState<string>("");
  const [columns, setColumns] = useState<Array<any>>([]);

  const addColumns = () => {
    setColumns([...columns, { id: uuid() }]);
  };

  const removeColumns = (targetKey: any) => {
    let data = columns.filter((item: any) => item.id !== targetKey);
    setColumns(data);
  };

  const manageColumns = (targetKey: any, action: "add" | "remove") => {
    if (action === "add") {
      addColumns();
    } else removeColumns(targetKey);
  };

  const onColumnFieldChange = (id: string, field: string, value: string) => {
    let otherColumns = columns.filter((item: any) => item.id !== id);
    let column = columns.find((item: any) => item.id === id);
    column = {
      ...column,
      [field]: value
    };

    setColumns([...otherColumns, { ...column }]);
  };

  return (
    <Modal
      title={"Configure Table"}
      wrapClassName={"configuration-tables"}
      visible={props.visible}
      onOk={props.onOk}
      onCancel={props.onCancel}>
      <AntRow gutter={[10, 10]}>
        <AntCol span={24}>
          <Form layout={"vertical"}>
            <Form.Item label="Name" required>
              <Input placeholder="Name" value={tableName} onChange={e => setTableName(e.target.value)} />
            </Form.Item>
            <Form.Item label="Add Columns" required>
              <Tabs size={"small"} onEdit={manageColumns} type="editable-card">
                {columns.map((child: any, index: number) => (
                  <TabPane tab={`Column ${index + 1}`} key={child.id} closable={true}>
                    <Form layout={"vertical"}>
                      <Form.Item label="Key" required>
                        <Input
                          placeholder="Key"
                          value={child.key || ""}
                          onChange={e => onColumnFieldChange(child.id, "key", e.target.value)}
                        />
                      </Form.Item>
                      <Form.Item label="Display Name" required>
                        <Input
                          placeholder="Display Name"
                          value={child.name || ""}
                          onChange={e => onColumnFieldChange(child.id, "name", e.target.value)}
                        />
                      </Form.Item>
                      <Form.Item label="Type" required>
                        <Select
                          value={child.type || "string"}
                          onChange={(value: any) => onColumnFieldChange(child.id, "type", value)}>
                          <Option value={"string"}>String</Option>
                          <Option value={"boolean"}>Boolean</Option>
                          <Option value={"select"}>Select</Option>
                          <Option value={"multi-select"}>Multi-Select</Option>
                        </Select>
                      </Form.Item>
                    </Form>
                  </TabPane>
                ))}
              </Tabs>
            </Form.Item>
          </Form>
        </AntCol>
      </AntRow>
    </Modal>
  );
};

export default ConfigurationTablesConfigureModal;
