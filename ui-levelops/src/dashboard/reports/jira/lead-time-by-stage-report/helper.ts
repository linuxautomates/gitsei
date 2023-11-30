import React from "react";
import DrilldownViewMissingCheckbox from "./DrilldownViewMissingCheckbox";

export const getFilters = (props: any) => {
  const { finalFilters, contextFilter } = props;
  return { ...finalFilters, filter: { ...finalFilters.filter, ...contextFilter } };
};

export const getDrilldownCheckBox = () => {
  return DrilldownViewMissingCheckbox;
};
