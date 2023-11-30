import { Form } from "antd";
import { getFilterValue } from "helper/widgetFilter.helper";
import { isArray } from "lodash";
import { DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { CustomFormItemLabel, CustomSelect } from "shared-resources/components";

interface UniversalIssueManagementSystemFilterProps {
  filterProps: LevelOpsFilter;
  handleMetadataChange?: (key: any, value: any) => void;
}

const UniversalIssueManagementSystemFilter: React.FC<UniversalIssueManagementSystemFilterProps> = ({
  handleMetadataChange,
  filterProps
}) => {
  const { label, beKey, filterMetaData, allFilters, disabled, defaultValue, getMappedValue } = filterProps;
  const { options, selectMode, valueKey, labelKey } = filterMetaData as DropDownData;

  const isDisabled = useMemo(() => {
    if (typeof disabled === "boolean") return disabled;
    if (disabled instanceof Function) return disabled({ filters: allFilters });
    return false;
  }, [disabled, allFilters]);

  const value = getMappedValue?.({ allFilters }) ?? getFilterValue(allFilters, "default_value")?.value ?? defaultValue;

  const getOptions = useMemo(() => {
    if (isArray(options)) return options;
    if (options instanceof Function) return options({ ...filterProps });
    return [];
  }, [filterProps, options]);

  return (
    <Form.Item key={beKey} label={<CustomFormItemLabel label={label} />}>
      <CustomSelect
        valueKey={valueKey ?? "value"}
        labelKey={labelKey ?? "label"}
        createOption={false}
        labelCase={"title_case"}
        showArrow={true}
        disabled={isDisabled}
        value={value}
        options={getOptions}
        mode={selectMode}
        onChange={(value: any) => handleMetadataChange?.(value, beKey)}
      />
    </Form.Item>
  );
};

export default UniversalIssueManagementSystemFilter;
