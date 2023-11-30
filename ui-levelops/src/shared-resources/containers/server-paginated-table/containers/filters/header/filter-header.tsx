import React from "react";

import "./filter-header.scss";

interface FilterHeaderProps {
  label: string;
}

export const FilterHeader: React.FC<FilterHeaderProps> = (props: FilterHeaderProps) => (
  <div className="filter-label">
    <span>{props.label}</span>
  </div>
);
