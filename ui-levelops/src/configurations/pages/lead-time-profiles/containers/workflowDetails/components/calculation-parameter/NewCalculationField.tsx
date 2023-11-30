import { WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import React from "react";
import { AntSelect } from "shared-resources/components";
import { getParameterOptions } from "./helper";
import "./CalculationFieldsComponent.scss";

interface NewCalculationFieldsProps {
  calculationRoute: "pr" | "commit";
  integrationType: WorkflowIntegrationType | undefined;
  applicationName: string;
  onChange: (calculationValue: string) => void;
  value: string;
}

const NewCalculationFieldsComponent: React.FC<NewCalculationFieldsProps> = ({
  integrationType,
  applicationName,
  onChange,
  value,
  calculationRoute
}) => (
    <AntSelect
      className="calculation-fields"
      value={value}
      options={getParameterOptions(integrationType, applicationName, calculationRoute)}
      onChange={onChange}
    />
);

export default NewCalculationFieldsComponent;
