import { Radio } from "antd";
import { RadioChangeEvent } from "antd/lib/radio";
import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import React from "react";
import { AntText } from "shared-resources/components";
import { getParameterOptions } from "./helper";
import "./calculationFieldsComponent.scss";

interface CalculationFieldsProps {
  calculationType: string;
  integrationType: WorkflowIntegrationType | undefined;
  applicationName: string;
  onChange: (calculationValue: string) => void;
  value: string;
}

const CalculationFieldsComponent: React.FC<CalculationFieldsProps> = ({
  calculationType,
  integrationType,
  applicationName,
  onChange,
  value
}) => (
  <div className="calculation-fields">
    <AntText
      strong>{`How do you want to calculate the ${calculationType} for the timeline present in the insight?`}</AntText>
    <Radio.Group onChange={(e: RadioChangeEvent) => onChange(e.target.value)} value={value}>
      {getParameterOptions(integrationType, applicationName).map((option: { label: string; value: string }) => (
        <Radio value={option.value} key={option.value} className="radio-style">
          {option.label}
        </Radio>
      ))}
    </Radio.Group>
  </div>
);

export default CalculationFieldsComponent;
