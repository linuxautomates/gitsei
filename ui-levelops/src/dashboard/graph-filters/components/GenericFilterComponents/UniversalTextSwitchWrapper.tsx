import { getFilterValue } from "helper/widgetFilter.helper";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import React from "react";
import { NewCustomFormItemLabel } from "shared-resources/components";
import { AntSwitchComponent } from "shared-resources/components/ant-switch/ant-switch.component";

interface UniversalTextSwitchWrapperProps {
  onFilterValueChange: (value: any, type?: any) => void;
  filterProps: LevelOpsFilter;
}

const UniversalTextSwitchWrapper: React.FC<UniversalTextSwitchWrapperProps> = (
  props: UniversalTextSwitchWrapperProps
) => {
  const { onFilterValueChange, filterProps } = props;
  const { label, beKey, allFilters, getMappedValue } = filterProps;

  const value = getMappedValue?.({ allFilters }) ?? getFilterValue(allFilters, beKey, true)?.value;

  return (
    <div className={"flex justify-space-between my-10"}>
      <NewCustomFormItemLabel label={label} />
      <AntSwitchComponent checked={value} onChange={(value: boolean) => onFilterValueChange(value, beKey)} />
    </div>
  );
};

export default UniversalTextSwitchWrapper;
