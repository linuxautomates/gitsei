import React from "react";
import "./FilterLabel.style.scss";

interface FilterLabelProps {
  label: string;
}

const FilterLabel: React.FC<FilterLabelProps> = props => {
  const { label } = props;

  return <div className="FilterLabel">{label || ""}</div>;
};

export default FilterLabel;
