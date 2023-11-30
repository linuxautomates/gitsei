import React, { useMemo } from "react";
import { AntFormItem, SwitchWithLabel, FilterLabel, AntText, CustomFormItemLabel } from "shared-resources/components";
import TagSelect from "../tag-select/TagSelect";
import { baseColumnConfig } from "utils/base-table-config";
import classnames from "classnames";
import "./APIFilterManyOptions.style.scss";
import { Checkbox, Form } from "antd";
import { stringSortingComparator } from "../sort.helper";
import {
  SELECT_NAME_TYPE_SPRINT,
  ACTIVE_SPRINT_TYPES,
  ACTIVE_SPRINT_CONFIG_BY_TYPE
} from "../../../../dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import { SUPPORTED_FILTERS_WITH_INFO } from "dashboard/constants/filter-name.mapping";

interface APIFilterManyOptionsProps {
  data_testId: string;
  help?: any;

  // Limits user to selecting one option
  singleSelect?: boolean;
  createOption?: boolean;
  APIFiltersProps: any;
  apiFilterProps: any;
  switchWithDropdown?: any;
  hideSprintFilters?: boolean;
  reportType?: string;
  useDefaultOptionKeys?: boolean;
  formLabel?: React.ReactNode;
}

const APIFilterManyOptions: React.FC<APIFilterManyOptionsProps> = props => {
  const {
    data_testId,
    help,
    APIFiltersProps,
    apiFilterProps,
    switchWithDropdown,
    hideSprintFilters,
    useDefaultOptionKeys,
    formLabel
  } = props;

  const { selectName, switchValue, dataKey, value, withSwitchConfig, partialValue, partialKey } = apiFilterProps;

  const _selectName = (selectName || "").toLowerCase();

  const finalClassName = classnames("custom-form-item", "api-filter-many-options");

  const inPartialMode = Object.keys(apiFilterProps.partialValue || {}).length > 0;

  const showSwitch = withSwitchConfig && withSwitchConfig.showSwitch;
  const customfieldcheckboxkey = apiFilterProps?.withSwitchConfig?.customFieldObjectCheckBox?.datakey || undefined;
  let headerStyles = {};
  if (!showSwitch) {
    headerStyles = {
      marginBottom: "0.6rem"
    };
  }

  const newCheckTextStyle = useMemo(
    () => ({
      color: "#5f5f5f",
      fontSize: "12px",
      marginTop: "0.3rem",
      fontWeight: "500",
      textTransform: "none"
    }),
    []
  );

  const columnConfig = useMemo(
    () => [
      {
        ...baseColumnConfig(
          "Value",
          useDefaultOptionKeys
            ? "label"
            : APIFiltersProps.isCustom
            ? "label"
            : apiFilterProps.options && apiFilterProps.options[0] && apiFilterProps.options[0].value
            ? "value"
            : "key"
        ),

        sorter: stringSortingComparator(),
        sortDirections: ["descend", "ascend"]
      }
    ],
    [APIFiltersProps.isCustom, useDefaultOptionKeys]
  );

  return (
    <AntFormItem data-testid={data_testId} className={finalClassName} style={{ flex: 1 }} help={help}>
      <React.Fragment>
        <div className="flex" style={headerStyles}>
          <div style={{ flex: 1, color: "black" }}>
            <Form.Item
              label={
                <>
                  {formLabel ? (
                    formLabel
                  ) : (
                    <CustomFormItemLabel
                      label={selectName}
                      withInfo={{
                        showInfo: get(widgetConstants, [props.reportType, SUPPORTED_FILTERS_WITH_INFO, dataKey], false),
                        description: get(
                          widgetConstants,
                          [props.reportType, SUPPORTED_FILTERS_WITH_INFO, dataKey, "info"],
                          ""
                        )
                      }}
                      required={get(widgetConstants, [props.reportType, "requiredSupportedFilter", dataKey], false)}
                    />
                  )}
                </>
              }
            />
          </div>
          {!ACTIVE_SPRINT_TYPES.includes(_selectName) && formLabel === undefined && showSwitch && (
            <SwitchWithLabel
              {...{
                ...withSwitchConfig
              }}
            />
          )}
        </div>
        {!hideSprintFilters && ACTIVE_SPRINT_TYPES.includes(_selectName) && (
          <div className={"flex mb-5 mt-5"}>
            <div style={{ flex: 1 }}>
              <Checkbox
                className="mr-5"
                checked={
                  APIFiltersProps.filters.hasOwnProperty(ACTIVE_SPRINT_CONFIG_BY_TYPE[_selectName].filterKey) &&
                  (APIFiltersProps.filters[ACTIVE_SPRINT_CONFIG_BY_TYPE[_selectName].filterKey] || []).length > 0
                }
                onChange={event => {
                  APIFiltersProps.handleFilterValueChange(
                    event.target.checked,
                    ACTIVE_SPRINT_CONFIG_BY_TYPE[_selectName].filterKey
                  );
                }}>
                <AntText style={newCheckTextStyle}>{ACTIVE_SPRINT_CONFIG_BY_TYPE[_selectName].label}</AntText>
              </Checkbox>
            </div>
            {_selectName === SELECT_NAME_TYPE_SPRINT && (
              <div style={{ flex: 1 }}>
                <Checkbox
                  className="mr-5"
                  checked={
                    APIFiltersProps.filters.hasOwnProperty("last_sprint") && APIFiltersProps.filters["last_sprint"]
                  }
                  onChange={event => APIFiltersProps.handleLastSprintChange(event.target.checked, dataKey)}>
                  <AntText style={newCheckTextStyle}>Filter by last sprint</AntText>
                </Checkbox>
              </div>
            )}
            {formLabel === undefined && showSwitch && (
              <SwitchWithLabel
                {...{
                  ...withSwitchConfig
                }}
              />
            )}
          </div>
        )}
        <TagSelect
          reportType={props.reportType}
          switchValue={switchValue}
          singleSelect={props.singleSelect}
          selectMode={inPartialMode ? "partial" : "full"}
          partialValue={partialValue}
          switchWithDropdown={switchWithDropdown}
          isCustom={!!APIFiltersProps.isCustom}
          isVisible={APIFiltersProps.activePopkey === dataKey}
          columns={columnConfig}
          valueKey={useDefaultOptionKeys ? "value" : APIFiltersProps.isCustom ? "value" : "key"}
          labelKey={
            useDefaultOptionKeys
              ? "label"
              : APIFiltersProps.isCustom
              ? "label"
              : apiFilterProps.options && apiFilterProps.options[0] && apiFilterProps.options[0].value
              ? "value"
              : "key"
          }
          dataKey={dataKey}
          filterValueLoading={false}
          dataSource={apiFilterProps.options}
          selectedValues={value && !Array.isArray(value) ? [value] : value}
          tableHeader={selectName}
          onCancel={() => APIFiltersProps.handleActivePopkey(undefined)}
          partialKey={partialKey}
          onPartialValueChange={(dataKey: string, data: any) => {
            APIFiltersProps.handlePartialValueChange(dataKey, data, {
              allowExcludeWithPartialMatch: APIFiltersProps?.allowExcludeWithPartialMatch,
              keyName: partialKey ?? dataKey
            });
            APIFiltersProps.handleActivePopkey(undefined);
          }}
          onFilterValueChange={(data: any) => {
            let finalData = data;
            if (props.singleSelect) {
              finalData = finalData[0] || "";
            }

            APIFiltersProps.handleFilterValueChange(finalData, dataKey, switchValue);
            APIFiltersProps.handleActivePopkey(undefined);
          }}
          onVisibleChange={APIFiltersProps.handleActivePopkey}
          onChange={(data: any) => {
            let finalData = data;
            if (props.singleSelect) {
              finalData = finalData[0] || "";
            }

            APIFiltersProps.handleFilterValueChange(finalData, dataKey, switchValue);
          }}
          useDefaultOptionKeys={useDefaultOptionKeys}
          customClassNames={APIFiltersProps?.customProps?.classNames}
        />
      </React.Fragment>
    </AntFormItem>
  );
};

export default APIFilterManyOptions;
