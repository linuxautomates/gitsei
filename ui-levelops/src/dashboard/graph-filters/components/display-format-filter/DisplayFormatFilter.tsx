import React, { useCallback } from "react";
import CustomSelectWrapper from "shared-resources/components/custom-select/CustomSelectWrapper";
import { displayFormatOptionsConfig, DISPLAY_FORMAT_FILTER_KEY, DISPLAY_FORMAT_TITLE } from "./helper";
import { ITEM_TEST_ID } from "../Constants";
import { Form } from "antd";
import { AntSelect } from "shared-resources/components";

interface DisplayFormatFilterProps {
  onFilterChange?: (value: any, type: any) => void;
  currentValue: string;
}

const DisplayFormatFilter: React.FC<DisplayFormatFilterProps> = ({ onFilterChange, currentValue }) => {
  const handleFilterValueChange = useCallback(
    (value: any) => {
      onFilterChange && onFilterChange(value, DISPLAY_FORMAT_FILTER_KEY);
    },
    [onFilterChange]
  );

  return (
    <Form.Item
      key={DISPLAY_FORMAT_FILTER_KEY}
      label={DISPLAY_FORMAT_TITLE}
      data-filterselectornamekey={`${ITEM_TEST_ID}-display-format`}
      data-filtervaluesnamekey={`${ITEM_TEST_ID}-display-format`}>
      <AntSelect
        dropdownTestingKey={`${ITEM_TEST_ID}-display-format_dropdown`}
        showArrow={true}
        value={currentValue}
        options={displayFormatOptionsConfig}
        mode={"single"}
        onSelect={handleFilterValueChange}
      />
    </Form.Item>
  );
};

export default React.memo(DisplayFormatFilter);
