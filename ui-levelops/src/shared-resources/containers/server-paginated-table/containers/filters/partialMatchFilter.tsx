import { Radio } from "antd";
import React, { useState } from "react";
import { AntCol, AntInput } from "shared-resources/components";
interface partialMatchFilterProps {
  filterConfig: any;
  index: number;
  onChangehandler: any;
  renderLabel: (label: string) => string;
  onPartialRadioChange: any;
}
const PartialMatchFilter: React.FC<partialMatchFilterProps> = (props: partialMatchFilterProps) => {
  const { filterConfig, index, onChangehandler, renderLabel, onPartialRadioChange } = props;
  const radioValue = Object.keys(filterConfig?.selected || {})?.[0] || "$begins";
  const [startWith, setStartWith] = useState<string>(radioValue);
  const onButtonChange = (e: any) => {
    if (filterConfig?.selected?.[startWith]?.length) {
      onPartialRadioChange(filterConfig.field, e.target.value, filterConfig?.selected?.[startWith]);
    }
    setStartWith(e.target.value);
  };
  return (
    <AntCol key={index} className="gutter-row" span={12}>
      {renderLabel(filterConfig.label)}
      <Radio.Group onChange={onButtonChange} value={startWith}>
        <div style={{ display: "flex" }}>
          <Radio value={"$begins"}>Start with</Radio>
          <Radio value={"$contains"}>Contains</Radio>
        </div>
      </Radio.Group>
      <AntInput
        id={`search-${filterConfig.id}`}
        placeholder={"Case Sensitive"}
        onChange={onChangehandler(filterConfig.field, startWith)}
        value={filterConfig.selected?.[startWith] || ""}
        name={filterConfig.id}
      />
    </AntCol>
  );
};

export default PartialMatchFilter;
