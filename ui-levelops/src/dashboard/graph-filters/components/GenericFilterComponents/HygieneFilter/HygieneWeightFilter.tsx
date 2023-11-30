import { Form, Slider } from "antd";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React from "react";
import { NewCustomFormItemLabel } from "shared-resources/components";

interface HygieneWeightFilterProps {
  filterProps: LevelOpsFilter;
  onWeightChange: (value: any, type?: any) => void;
  widgetWeights: any;
  handleRemoveFilter: (key: string) => void;
}

const HygieneWeightFilterComponent: React.FC<HygieneWeightFilterProps> = (props: HygieneWeightFilterProps) => {
  const { widgetWeights, onWeightChange, filterProps } = props;
  const { label, beKey, filterMetaData } = filterProps;
  const { customHygienes } = filterMetaData as DropDownData;

  const customHygieneIds = (customHygienes ?? []).map((cHygiene: any) => cHygiene.id);

  const handleChange = (value: any, key: string) => {
    const name = key.replace(/_/g, " ");
    if (customHygieneIds.includes(name)) {
      onWeightChange(value, { id: key, type: key });
    } else {
      onWeightChange(value, key);
    }
  };

  return (
    <>
      <div style={{ paddingLeft: !widgetWeights[beKey] ? "0.35rem" : "0.25rem" }}>
        <Form.Item key={beKey} label={<NewCustomFormItemLabel label={label.replace(/_/g, " ") || ""} />} colon={false}>
          <Slider
            value={widgetWeights[beKey]}
            onChange={(value: any) => handleChange(value, beKey)}
            min={0}
            max={100}
          />
        </Form.Item>
      </div>
    </>
  );
};

export default HygieneWeightFilterComponent;
