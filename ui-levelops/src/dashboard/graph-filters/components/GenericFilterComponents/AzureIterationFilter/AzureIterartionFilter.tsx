import React from "react";
import { Form } from "antd";
import { get } from "lodash";
import { AntSelect, CustomTreeSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { optionType } from "dashboard/dashboard-types/common-types";
import { ITEM_TEST_ID } from "../../Constants";
import TagSelect from "../../tag-select/TagSelect";
import { LevelOpsFilter } from "model/filters/levelopsFilters";
import { USE_PAGINATED_FILTERS_THRESHOLD } from "dashboard/constants/constants";
import APIFilterManyOptions from "../../APIFilterManyOptions/APIFilterManyOptions";
import { getFilterValue } from "helper/widgetFilter.helper";

interface AzureIterationFilterProps {
  label: string;
  apiFilters: any;
  handleFilterValueChange: (value: any, key: string) => void;
  filterProps: LevelOpsFilter;
  selectedValue: string[];
  activePopupKey: any;
  columnConfig: any;
  setActivePopupKey: (value: boolean) => void;
  beKey: string;
  tableHeader?: string;
  APIFiltersProps: any;
  reportType: string;
  mappedApiFilterProps: any;
}
const AzureIterationFilter: React.FC<AzureIterationFilterProps> = props => {
  const {
    label,
    activePopupKey,
    selectedValue,
    handleFilterValueChange,
    apiFilters,
    columnConfig,
    setActivePopupKey,
    beKey,
    tableHeader,
    filterProps,
    mappedApiFilterProps
  } = props;

  return (
    <>
      {
        <APIFilterManyOptions
          data_testId={ITEM_TEST_ID}
          APIFiltersProps={props.APIFiltersProps()}
          useDefaultOptionKeys
          apiFilterProps={mappedApiFilterProps()}
          // help={helpValue}
          switchWithDropdown={apiFilters.switchWithDropdown}
          createOption={true}
          reportType={props.reportType}
          formLabel={
            <NewCustomFormItemLabel
              label={label}
              withDelete={apiFilters.withDelete}
              withSwitch={apiFilters.withSwitch}
            />
          }
        />
      }
    </>
  );
};

export default AzureIterationFilter;
