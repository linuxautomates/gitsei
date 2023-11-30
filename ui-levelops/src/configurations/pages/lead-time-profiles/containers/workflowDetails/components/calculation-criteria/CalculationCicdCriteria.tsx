import React from "react";
import { AntSelect } from "shared-resources/components";
import "./CalculationCriteria.scss";
import { getCalculationCriteriaOptions } from "./helper";

interface CalculationCicdCriteriaProps {
  calculationRoute: "jobs" | "pipelines" | string;
  onChange: (
    criteria:
      | "start_time"
      | "end_time"
      | "issue_resolved_at"
      | "workitem_resolved_at"
      | "issue_updated_at"
      | "workitem_updated_at"
  ) => void;
  value?:
    | "start_time"
    | "end_time"
    | "issue_resolved_at"
    | "workitem_resolved_at"
    | "issue_updated_at"
    | "workitem_updated_at";
  application?: string;
}

const CalculationCicdCriteria: React.FC<CalculationCicdCriteriaProps> = ({
  calculationRoute,
  onChange,
  value,
  application
}) => (
  <AntSelect
    className="calculation-cicd-criteria"
    value={value}
    options={getCalculationCriteriaOptions(calculationRoute, application)}
    onChange={onChange}
  />
);

export default CalculationCicdCriteria;
