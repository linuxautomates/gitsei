import React from "react";
import { Form } from "antd";
import { get } from "lodash";
import { AntSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { optionType } from "dashboard/dashboard-types/common-types";
import { ITEM_TEST_ID } from "../../Constants";
import TagSelect from "../../tag-select/TagSelect";
import { LevelOpsFilter } from "model/filters/levelopsFilters";

interface AzureTeamFilterProps {
  label: string;
  apiFilters: any;
  handleFilterValueChange: (value: any, key: string) => void;
  filterProps: LevelOpsFilter;
  handleRemoveFilter: any;
  azureTeamsOptions: optionType[] | undefined;
  selectedValue: string[];
  activePopupKey: any;
  columnConfig: any;
  setActivePopupKey: (value: boolean) => void;
  beKey: string;
  dataKey: string;
  tableHeader?: string;
}
const AzureTeamFilter: React.FC<AzureTeamFilterProps> = props => {
  const {
    label,
    azureTeamsOptions,
    activePopupKey,
    selectedValue,
    handleFilterValueChange,
    apiFilters,
    columnConfig,
    setActivePopupKey,
    beKey,
    tableHeader,
    dataKey
  } = props;
  return (
    <>
      {
        <Form.Item
          label={
            <div style={{ display: "flex", width: "100%" }}>
              <NewCustomFormItemLabel label={label} {...apiFilters} />
            </div>
          }
          key={`${ITEM_TEST_ID}-azure-teams`}
          data-filterselectornamekey={`${ITEM_TEST_ID}-azure-teams`}
          data-filtervaluesnamekey={`${ITEM_TEST_ID}-azure-teams`}>
          {(azureTeamsOptions || []).length < 10 ? (
            <AntSelect
              showSearch
              dropdownTestingKey={`${ITEM_TEST_ID}-azure-teams_dropdown`}
              mode="multiple"
              value={selectedValue}
              options={azureTeamsOptions}
              showArrow={true}
              onChange={(value: string[]) => handleFilterValueChange(value, beKey)}
              filterOption={(input: string, option: any) => {
                const teamsOptionValue = get(option, ["props", "children"], "");
                return teamsOptionValue.toLowerCase().indexOf((input || "").toLowerCase()) >= 0;
              }}
            />
          ) : (
            <TagSelect
              switchValue={null}
              selectMode={"full"}
              partialValue={null}
              switchWithDropdown={null}
              isCustom={false}
              isVisible={activePopupKey}
              columns={columnConfig("label")}
              valueKey={"value"}
              labelKey={"label"}
              dataKey={dataKey}
              filterValueLoading={false}
              dataSource={azureTeamsOptions || []}
              selectedValues={selectedValue}
              tableHeader={tableHeader as string}
              onCancel={() => setActivePopupKey(false)}
              onPartialValueChange={(dataKey: string, data: any) => {}}
              onFilterValueChange={(data: any) => {
                let finalData = data;
                handleFilterValueChange(finalData, beKey);
                setActivePopupKey(false);
              }}
              onVisibleChange={() => setActivePopupKey(!activePopupKey)}
              onChange={(data: any) => {
                let finalData = data;
                handleFilterValueChange(finalData, beKey);
              }}
            />
          )}
        </Form.Item>
      }
    </>
  );
};

export default AzureTeamFilter;
