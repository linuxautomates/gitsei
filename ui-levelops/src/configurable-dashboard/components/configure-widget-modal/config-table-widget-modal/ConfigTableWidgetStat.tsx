import { Form } from "antd";
import React from "react";
import { CustomSelect } from "shared-resources/components";
import "./Style.scss";

interface ConfigTableWidgetStatProps {
  setAxis: (key: string, value: any, yIndex: number) => void;
  options: Array<any>;
  value: { key: string };
}

const ConfigTableWidgetStat: React.FC<ConfigTableWidgetStatProps> = (props: ConfigTableWidgetStatProps) => {
  return (
    <div className="axis-container">
      <Form.Item label="Column" style={{ marginTop: "1.5rem" }}>
        <CustomSelect
          valueKey="key"
          labelKey="label"
          labelCase="none"
          mode="default"
          createOption={false}
          value={props.value.key}
          onChange={value => props.setAxis("key", value, 0)}
          options={props.options}
        />
      </Form.Item>
    </div>
  );
};

export default ConfigTableWidgetStat;
