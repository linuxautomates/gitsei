import { Form, Spin } from "antd";
import { filterFieldType } from "configurations/configuration-types/OUTypes";
import { ITEM_TEST_ID } from "dashboard/graph-filters/components/Constants";
import { isArray, uniqBy } from "lodash";
import { ApiDropDownData, APIFilterConfigType, DropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useEffect, useMemo, useState } from "react";
import { useSelector } from "react-redux";
import { customTimeFilterKeysSelector } from "reduxConfigs/selectors/jira.selector";
import { AntSelect, CustomSelect } from "shared-resources/components";
import CustomTreeSelectComponent from "shared-resources/components/custom-tree-select-component/custom-tree-select-component";

interface OrganisationFilterSelectProps {
  filterProps: LevelOpsFilter;
  handleFieldChange: (type: filterFieldType, index: number, value: any) => void;
  index: number;
  handleAzureAreasSelectChange: (event: string[]) => void;
}

const OrganisationFilterSelect: React.FC<OrganisationFilterSelectProps> = (props: OrganisationFilterSelectProps) => {
  const { filterProps, handleFieldChange, index, handleAzureAreasSelectChange } = props;
  const { label, filterMetaData, allFilters, labelCase, beKey, disabled } = filterProps;
  const { options, selectMode, sortOptions, clearSupport, checkValueWithApiResponse } = filterMetaData as DropDownData;

  const [oneTimeCallOnly, setOneTimeCallOnly] = useState<boolean>(true);

  const dataFilterNameKey = `${ITEM_TEST_ID}-${(label || "").split(" ").join("-")}`;

  const value = allFilters?.value || [];
  const customTimeFilterKeys: Array<any> = useSelector(customTimeFilterKeysSelector);

  const getOptions = useMemo(() => {
    let _options: Array<{ label: string; value: string | number }> = [];
    if (isArray(options)) _options = options;
    if (options instanceof Function) _options = options({ ...filterProps, customTimeFilterKeys });
    return uniqBy(_options, "value");
  }, [filterProps, options]);

  const onChange = (value: any) => {
    handleFieldChange("value", index, value);
  };

  // THIS WILL RUN ONLY ONE TIME FOR CHECK WHAT ARE THE DEFAULT VALUES THAT ARE GIVEN ARE COMMING IN API RESPONSE OR NOT
  // THIS WILL ONLY RUN WHEN checkValueWithApiResponse WILL COME TRUE FROM FILTER CONFIG OF THAT APPLICATION
  useEffect(() => {
    if (
      checkValueWithApiResponse &&
      getOptions &&
      getOptions.length > 0 &&
      value &&
      value.length > 0 &&
      oneTimeCallOnly
    ) {
      let updatedValue = getOptions.reduce((acc: any, defaultData: any) => {
        let filterData = value.filter((filterData: string) => filterData == defaultData.value);
        if (filterData.length > 0) {
          acc.push(defaultData.value);
        }
        return acc;
      }, []);
      if (updatedValue.length > 0) onChange(updatedValue);

      setOneTimeCallOnly(false);
    }
  }, [getOptions, value, oneTimeCallOnly, setOneTimeCallOnly, checkValueWithApiResponse]);

  const apiConfig = useMemo(() => (filterMetaData as ApiDropDownData)?.apiConfig, [filterMetaData]) as
    | APIFilterConfigType
    | undefined;

  const loading = useMemo(() => !!apiConfig?.loading, [apiConfig]);

  const dataFilterNameDropdownKey = `${ITEM_TEST_ID}-${(label || "").split(" ").join("-")}_dropdown`;

  const preventDefault = (e: any) => {
    e.preventDefault();
    e.stopPropagation();
  };

  if (loading) {
    return <Spin size="small" />;
  }

  if (beKey === "code_area") {
    return (
      <div className="customSelect-width">
        <AntSelect
          mode="multiple"
          className="azure-areas-select"
          value={value}
          onChange={handleAzureAreasSelectChange}
          dropdownRender={() => (
            <div className="customTree-component-wrapper">
              <div onMouseDown={preventDefault}>
                <CustomTreeSelectComponent
                  data={getOptions}
                  selected={value}
                  returnAllValues={true}
                  createParentChildRelationInData={true}
                  onCheckboxValueChange={(value: string) => handleFieldChange("value", index, value)}
                />
              </div>
            </div>
          )}
        />
      </div>
    );
  }

  return (
    <Form.Item
      data-filterselectornamekey={dataFilterNameKey}
      data-filtervaluesnamekey={dataFilterNameKey}
      className={"custom-form-item"}>
      <div className="customSelect-width">
        <CustomSelect
          dataFilterNameDropdownKey={dataFilterNameDropdownKey}
          className="filter-col-select"
          showArrow={true}
          mode={selectMode}
          allowClear={clearSupport}
          createOption={false}
          sortOptions={sortOptions}
          labelCase={labelCase ?? "title_case"}
          disabled={!!disabled}
          labelKey={"label"}
          valueKey={"value"}
          options={getOptions}
          value={value}
          onChange={(value: string) => onChange(value)}
        />
      </div>
    </Form.Item>
  );
};

export default OrganisationFilterSelect;
