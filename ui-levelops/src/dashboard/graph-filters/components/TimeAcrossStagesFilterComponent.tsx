import { Form } from "antd";
import { get } from "lodash";
import React from "react";
import { CustomSelect } from "shared-resources/components";
import { ITEM_TEST_ID } from "./Constants";

interface TimeAcrossStagesFilterComponentProps {
  filters: any;
  stateOptions: any;
  onFilterValueChange: (value: any, type?: any, exclude?: boolean) => void;
}

const FROM_STATE = "from_state";
const TO_STATE = "to_state";

const TimeAcrossStagesFilterComponent: React.FC<TimeAcrossStagesFilterComponentProps> = props => {
  const getValueForTimeAcrossFilter = (dataKey: string) => {
    return get(props.filters, ["state_transition", dataKey], "");
  };

  const handleTimeAcrossFilterChange = (value: any, dataKey: string) => {
    const adjacentKey = dataKey === FROM_STATE ? TO_STATE : FROM_STATE;
    const adjacentValue = getValueForTimeAcrossFilter(adjacentKey);
    const dataObject: any = {};
    dataObject[dataKey] = value;
    dataObject[adjacentKey] = adjacentValue;
    props.onFilterValueChange(dataObject, "state_transition");
  };

  return (
    <div style={{ width: "100%", display: "flex", justifyContent: "space-between", alignItems: "center" }}>
      <Form.Item
        className={"custom-form-item"}
        required
        label="State From"
        style={{ width: "48%" }}
        data-filterselectornamekey={`${ITEM_TEST_ID}-state-from`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-state-from`}>
        <CustomSelect
          valueKey={"key"}
          mode="default"
          dataFilterNameDropdownKey={`${ITEM_TEST_ID}-state-from_dropdown`}
          createOption={true}
          labelCase={"title_case"}
          options={props.stateOptions?.["jira_status"] || []}
          showArrow={true}
          sortOptions
          value={getValueForTimeAcrossFilter(FROM_STATE)}
          onChange={(value: any) => {
            handleTimeAcrossFilterChange(value, FROM_STATE);
          }}
        />
      </Form.Item>
      <Form.Item
        className={"custom-form-item"}
        required
        label="State To"
        style={{ width: "48%" }}
        data-filterselectornamekey={`${ITEM_TEST_ID}-state-to`}
        data-filtervaluesnamekey={`${ITEM_TEST_ID}-state-to`}>
        <CustomSelect
          valueKey={"key"}
          mode="default"
          dataFilterNameDropdownKey={`${ITEM_TEST_ID}-state-to_dropdown`}
          sortOptions
          createOption={true}
          labelCase={"title_case"}
          options={props.stateOptions?.["jira_status"] || []}
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

export default TimeAcrossStagesFilterComponent;
