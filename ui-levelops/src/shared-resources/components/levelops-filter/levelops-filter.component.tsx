import * as React from "react";
import { LevelOpsFilter, LevelOpsFilterTypes } from "../../../model/filters/levelopsFilters";

interface LevelOpsFilterProps {
  filterProps: LevelOpsFilter;
  className?: string;
  style?: string;
  handlePartialValueChange: (key: string, value: any) => void;
}

// combines filter object with all the changeHandlers
export const LevelOpsFilterComponent: React.FC<LevelOpsFilterProps> = ({ filterProps, className, style, ...props }) => {
  if (filterProps.renderComponent) {
    return React.createElement(filterProps.renderComponent, { ...filterProps, ...props } as any);
  }

  return null;
};
