import { Form } from "antd";
import { useAPIFilter } from "custom-hooks/useAPIFilter";
import { get } from "lodash";
import { ApiDropDownData, LevelOpsFilter } from "model/filters/levelopsFilters";
import React, { useMemo } from "react";
import { CustomSelect, NewCustomFormItemLabel } from "shared-resources/components";
import { AddWidgetFilterContainerProps } from "./AddWidgetFilter/AddWidgetFilter.container";
import { ITEM_TEST_ID } from "./Constants";

interface TimeAcrossStagesFilterComponentProps extends AddWidgetFilterContainerProps {
  filterProps: LevelOpsFilter;
  handleRemoveFilter: (key: string) => void;
}

const FROM_STATE = "from_state";
const TO_STATE = "to_state";

const StateTranstionFilterComponent: React.FC<TimeAcrossStagesFilterComponentProps> = props => {
  const { filterProps, onFilterValueChange, handleRemoveFilter } = props;

  const { filterMetaData, beKey, deleteSupport, apiFilterProps } = filterProps;

  console.log("[apiProps]", apiFilterProps);
  const getPayload: (args: Record<string, any>) => Record<string, any> = props => {
    const { integrationIds } = props;
    return {
      fields: ["status"],
      filter: {
        integration_ids: integrationIds
      },
      integration_ids: integrationIds
    };
  };

  const { data, loading } = useAPIFilter(
    {
      uri: get(filterMetaData, ["uri"], ""),
      payload: getPayload,
      integration_ids: get(filterMetaData, ["integration_ids"], undefined),
      specialKey: "jira_status"
    } as ApiDropDownData,
    {},
    [filterMetaData]
  );

  const getStatusesOptions = useMemo(() => {
    if (!loading && data && data.length) {
      const statusFilterData = data[0];
      const statusKey = Object.keys(statusFilterData)[0];
      const values = statusFilterData[statusKey] ?? [];
      return values.map((item: any) => ({ label: item?.key, value: item?.key }));
    }
    return [];
  }, [data, loading]);

  const getValueForTimeAcrossFilter = (dataKey: string) => {
    return get(props.filters, ["state_transition", dataKey], "");
  };

  const handleTimeAcrossFilterChange = (value: any, dataKey: string) => {
    const adjacentKey = dataKey === FROM_STATE ? TO_STATE : FROM_STATE;
    const adjacentValue = getValueForTimeAcrossFilter(adjacentKey);
    const dataObject: any = {};
    dataObject[dataKey] = value;
    dataObject[adjacentKey] = adjacentValue;
    onFilterValueChange(dataObject, "state_transition");
  };

  const getApiFilterProps = useMemo(() => {
    return apiFilterProps?.({ beKey, handleRemoveFilter, deleteSupport }) ?? {};
  }, [apiFilterProps, beKey, handleRemoveFilter, deleteSupport]);

  return (
    <div style={{ width: "100%", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
      <Form.Item
        className={"custom-form-item"}
        label={<NewCustomFormItemLabel label={"State From"} required={true} />}
        style={{ width: "48%" }}
        data-filterselectornamekey={`${ITEM_TEST_ID}-state-from`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-state-from`}>
        <CustomSelect
          valueKey={"value"}
          mode="default"
          dataFilterNameDropdownKey={`${ITEM_TEST_ID}-state-from_dropdown`}
          createOption={true}
          labelCase={"title_case"}
          options={getStatusesOptions}
          showArrow={true}
          sortOptions={true}
          value={getValueForTimeAcrossFilter(FROM_STATE)}
          onChange={(value: any) => {
            handleTimeAcrossFilterChange(value, FROM_STATE);
          }}
        />
      </Form.Item>
      <Form.Item
        className={"custom-form-item"}
        label={<NewCustomFormItemLabel label={"State To"} {...getApiFilterProps} required={true} />}
        style={{ width: "48%" }}
        data-filterselectornamekey={`${ITEM_TEST_ID}-state-to`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-state-to`}>
        <CustomSelect
          valueKey={"value"}
          mode="default"
          dataFilterNameDropdownKey={`${ITEM_TEST_ID}-state-to_dropdown`}
          sortOptions={true}
          createOption={true}
          labelCase={"title_case"}
          options={getStatusesOptions}
          showArrow={true}
          value={getValueForTimeAcrossFilter(TO_STATE)}
          onChange={(value: any) => {
            handleTimeAcrossFilterChange(value, TO_STATE);
          }}
        />
      </Form.Item>
    </div>
  );
};

export default StateTranstionFilterComponent;
