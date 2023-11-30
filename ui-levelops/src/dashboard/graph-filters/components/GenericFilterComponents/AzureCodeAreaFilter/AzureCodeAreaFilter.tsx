import React from "react";
import { Form } from "antd";
import { AntSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { ITEM_TEST_ID } from "../../Constants";
import { optionType } from "dashboard/dashboard-types/common-types";
import CustomTreeSelectComponent from "shared-resources/components/custom-tree-select-component/custom-tree-select-component";

interface AzureCodeAreaFilterProps {
  apiFilters: any;
  azureAreaOptions: optionType[] | undefined;
  label: string;
  onCodeAreaValueChange: (event: any) => void;
  selectedValueForCustomTree: any[];
  handleFilterValueChange: (value: any, key: string) => void;
}

const AzureCodeAreaFilter: React.FC<AzureCodeAreaFilterProps> = (props: AzureCodeAreaFilterProps) => {
  const {
    apiFilters,
    label,
    azureAreaOptions,
    handleFilterValueChange,
    onCodeAreaValueChange,
    selectedValueForCustomTree
  } = props;

  const preventDefault = (e: any) => {
    e.preventDefault();
    e.stopPropagation();
  };

  return (
    <>
      {
        <Form.Item
          label={
            <div style={{ display: "flex", width: "100%" }}>
              <NewCustomFormItemLabel label={label} {...apiFilters} />
            </div>
          }
          key={`${ITEM_TEST_ID}-azure-areas`}
          data-filterselectornamekey={`${ITEM_TEST_ID}-azure-areas`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-azure-areas`}>
          <AntSelect
            mode="multiple"
            dropdownClassName="azure-areas-select"
            value={selectedValueForCustomTree}
            onChange={handleFilterValueChange}
            dropdownRender={(noData: any) => (
              <div className="customTree-component-wrapper">
                <div onMouseDown={preventDefault}>
                  {azureAreaOptions?.length === 0 && noData}
                  <CustomTreeSelectComponent
                    data={azureAreaOptions}
                    selected={selectedValueForCustomTree}
                    onCheckboxValueChange={onCodeAreaValueChange}
                  />
                </div>
              </div>
            )}
          />
        </Form.Item>
      }
    </>
  );
};

export default AzureCodeAreaFilter;
