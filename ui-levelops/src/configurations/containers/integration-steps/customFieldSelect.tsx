import * as React from "react";
import TagSelect from "../../../dashboard/graph-filters/components/tag-select/TagSelect";
import { baseColumnConfig } from "../../../utils/base-table-config";
import { useState } from "react";

interface CustomFieldSelectProps {
  selectedValues: any[];
  dataSource: any;
  onChange: any;
}

const CustomFieldSelect: React.FC<CustomFieldSelectProps> = ({ selectedValues, dataSource, onChange }) => {
  const [isVisible, setIsVisible] = useState<boolean>(false);

  const onVisibleChange = (data?: any) => {
    setIsVisible(!!data);
  };

  const onCancel = (...args: any) => {
    setIsVisible(false);
  };

  return (
    <TagSelect
      isVisible={isVisible}
      isCustom={false}
      onVisibleChange={onVisibleChange}
      dataKey={"key"}
      onChange={onChange}
      switchWithDropdown={false}
      selectMode={"full"}
      onCancel={onCancel}
      selectedValues={selectedValues}
      partialValue={[]}
      switchValue={false}
      onFilterValueChange={onChange}
      columns={[baseColumnConfig("Value", "key")]}
      valueKey={"key"}
      labelKey={"key"}
      filterValueLoading={false}
      dataSource={dataSource}
      tableHeader={"Select Values"}
      onPartialValueChange={() => {}}
    />
  );
};

export default React.memo(CustomFieldSelect);
