import React from 'react';
import { AntSelect } from 'shared-resources/components';
import './CalculationCriteria.scss';
import { getCalculationCriteriaOptions } from './helper';

interface CalculationCriteriaProps {
  calculationRoute: "pr" | "commit" | string;
  onChange: (criteria: "pr_merged" | "pr_closed"| "pr_merged_closed"|"commit_merged_to_branch"|"commit_with_tag"|"commit_merged_to_branch_with_tag") => void;
  value?: "pr_merged" | "pr_closed"| "pr_merged_closed"|"commit_merged_to_branch"|"commit_with_tag"|"commit_merged_to_branch_with_tag";
}

const CalculationCriteria: React.FC<CalculationCriteriaProps> = ({
  calculationRoute,
  onChange,
  value
}) => (
  <AntSelect
    className="calculation-criteria"
    value={value}
    options={getCalculationCriteriaOptions(calculationRoute)}
    onChange={onChange}
  />
);

export default CalculationCriteria;
