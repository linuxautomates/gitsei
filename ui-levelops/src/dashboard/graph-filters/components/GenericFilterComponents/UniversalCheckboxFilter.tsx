import { Checkbox, Form } from "antd";
import { CheckboxChangeEvent } from "antd/lib/checkbox";
import { getFilterValue } from "helper/widgetFilter.helper";
import { CheckboxData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { AntText, NewCustomFormItemLabel } from "shared-resources/components";
import { showInfoProps } from "shared-resources/components/custom-form-item-label/CustomFormItemLabel";

interface UniversalCheckboxFilterProps {
  filterProps: LevelOpsFilter;
  handleMetadataChange?: (key: any, value: any) => void;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
}

const UniversalCheckboxFilter: React.FC<UniversalCheckboxFilterProps> = ({
  filterProps,
  handleMetadataChange,
  onFilterValueChange
}) => {
  const {
    filterMetaData,
    metadata,
    beKey,
    label,
    allFilters,
    disabled,
    required,
    getMappedValue,
    defaultValue,
    updateInWidgetMetadata,
    filterInfo,
    modifyFilterValue,
    hideFilter
  } = filterProps;
  const { checkboxLabel } = filterMetaData as CheckboxData;

  const isDisabled = useMemo(() => {
    if (typeof disabled === "boolean") return disabled;
    if (disabled instanceof Function) return disabled({ filters: allFilters });
    return false;
  }, [disabled, allFilters]);

  const value = isDisabled
    ? false
    : getMappedValue?.({ allFilters }) ?? getFilterValue(allFilters, beKey, true)?.value ?? defaultValue;

  const isRequired = useMemo(() => {
    if (typeof required === "boolean") return required;
    if (required instanceof Function) return required({ filters: allFilters });
    return false;
  }, [required, allFilters]);

  const onChange = (value: any) => {
    if (updateInWidgetMetadata) {
      handleMetadataChange?.(value, beKey);
      return;
    }
    if (!!modifyFilterValue) {
      modifyFilterValue({ onFilterValueChange, value, beKey });
      return;
    }
    onFilterValueChange(value, beKey);
  };

  const filterInfoConfig: showInfoProps = useMemo(() => {
    if (typeof filterInfo === "function")
      return {
        showInfo: !!filterInfo({ filters: allFilters }),
        description: filterInfo({ filters: allFilters }) || ""
      };
    return { showInfo: !!filterInfo, description: filterInfo || "" };
  }, [filterInfo, allFilters]);

  const isFilterHidden = useMemo(() => {
    if (typeof hideFilter === "boolean") return hideFilter;
    if (hideFilter instanceof Function) return hideFilter({ filters: allFilters });
    return false;
  }, [hideFilter, allFilters]);

  if (isFilterHidden) return null;

  return (
    <Form.Item label={<NewCustomFormItemLabel label={label} required={isRequired} withInfo={filterInfoConfig} />}>
      <Checkbox checked={value} disabled={isDisabled} onChange={(e: CheckboxChangeEvent) => onChange(e.target.checked)}>
        {checkboxLabel && (
          <AntText style={{ color: isDisabled ? "#BFBFBF" : "initial" }} className="mr-1">
            {checkboxLabel}
          </AntText>
        )}
      </Checkbox>
    </Form.Item>
  );
};

export default UniversalCheckboxFilter;
