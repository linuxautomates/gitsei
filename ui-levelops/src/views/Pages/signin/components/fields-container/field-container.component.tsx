import React, { useMemo } from "react";

import cx from "classnames";

import "./field-container.style.scss";

interface FieldsContainerProps {
  steps: string[];
  step: string;
  className?: string;
}

const FieldsContainer: React.FC<FieldsContainerProps> = ({ steps, step, children, className }) => {
  const includedStep = useMemo(() => steps.includes(step), [steps]);

  return <div className={cx("input-field-component", { "current-step": includedStep }, className)}>{children}</div>;
};

export default FieldsContainer;
