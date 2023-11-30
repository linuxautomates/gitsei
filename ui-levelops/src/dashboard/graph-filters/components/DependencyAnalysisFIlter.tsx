import React from "react";
import { Form } from "antd";
import { AntSelect, CustomFormItemLabel } from "shared-resources/components";
import { dependencyAnalysisFilterOptions, ITEM_TEST_ID } from "./Constants";
import { getFilterValue, isExcludeVal } from "configurable-dashboard/helpers/helper";

interface DependencyAnalysisFilterProps {
  filters: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  onSwitchValueChange: (key: string, value: boolean) => void | undefined;
}

const DependencyAnalysisFilter: React.FC<DependencyAnalysisFilterProps> = ({
  filters,
  onFilterValueChange,
  onSwitchValueChange
}) => {
  return (
    <Form.Item
      key={`${ITEM_TEST_ID}-dependency-analysis`}
      data-filterselectornamekey={`${ITEM_TEST_ID}-dependency-analysis`}
      data-filtervaluesnamekey={`${ITEM_TEST_ID}-dependency-analysis`}
      className={"custom-form-item"}
      label={
        <CustomFormItemLabel
          label={"Dependency Analysis"}
          withSwitch={{
            showSwitchText: true,
            showSwitch: true,
            switchValue: isExcludeVal(filters, "links"),
            onSwitchValueChange: value => onSwitchValueChange("links", value)
          }}
        />
      }>
      <AntSelect
        allowClear
        showArrow
        value={getFilterValue(filters, "links")[0]}
        options={dependencyAnalysisFilterOptions}
        dropdownTestingKey={`${ITEM_TEST_ID}-dependency-analysis_dropdown`}
        mode="default"
        onChange={(value: any) => onFilterValueChange(value ? [value] : "", "links", isExcludeVal(filters, "links"))}
      />
    </Form.Item>
  );
};

export default DependencyAnalysisFilter;
