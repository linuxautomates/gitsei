import * as React from "react";
import { useState, useEffect, useMemo } from "react";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { get } from "lodash";
import { jiraStatusFilterValues } from "reduxConfigs/actions/restapi/jiraFilterStatusValue.action";
import { useDispatch } from "react-redux";
import { AntSelect } from "../../../../shared-resources/components";
import { Form } from "antd";
import { ITEM_TEST_ID } from "../Constants";

interface SprintDoneStatusSelectProps {
  onFilterValueChange: (value: any, type?: any, exclude?: boolean, addToMetaData?: any) => void;
  value: string[];
  integrationIds: string[];
  apiOptions: { [key: string]: string }[];
  valueKey?: string;
}

const SprintDoneStatusSelectComponent: React.FC<SprintDoneStatusSelectProps> = ({
  onFilterValueChange,
  value,
  integrationIds,
  apiOptions,
  valueKey = ""
}) => {
  const dispatch = useDispatch();

  const [excludeFiltersLoading, setExcludeFiltersLoading] = useState(false);
  const [excludeOptions, setExcludeOptions] = useState<string[]>([]);

  const excludeStatusState = useParamSelector(getGenericRestAPISelector, {
    uri: "jira_filter_values",
    method: "list",
    uuid: "exclude_status"
  });

  useEffect(() => {
    const data = get(excludeStatusState, "data", {});
    const loading = get(excludeStatusState, "loading", false);
    if (Object.keys(data).length > 0 && !loading) {
      setExcludeFiltersLoading(false);
    } else {
      if (excludeFiltersLoading) {
        return;
      }
      setExcludeFiltersLoading(true);
      dispatch(
        jiraStatusFilterValues(
          {
            fields: ["status"],
            filter: {
              status_categories: ["Done", "DONE"],
              integration_ids: integrationIds
            }
          },
          "exclude_status"
        )
      );
    }
  }, []);

  useEffect(() => {
    const loading = get(excludeStatusState, "loading", true);
    if (!loading) {
      const list = get(excludeStatusState, ["data", "records"], []);
      setExcludeOptions(list[0]["status"].map((option: any) => option.key));
      setExcludeFiltersLoading(false);
    }
  }, [excludeStatusState]);

  const options = useMemo(() => {
    if (excludeFiltersLoading) {
      return [];
    }

    return apiOptions.map(option => option.key).filter(key => !excludeOptions.includes(key));
  }, [apiOptions, excludeFiltersLoading, excludeOptions]);

  return (
    <Form.Item
      key={"sprint-done-status-select"}
      label={"Additional Done Statuses"}
      data-filterselectornamekey={`${ITEM_TEST_ID}-additional-done-status`}
      data-filtervaluesnamekey={`${ITEM_TEST_ID}-additional-done-status`}>
      <AntSelect
        dropdownTestingKey={`${ITEM_TEST_ID}-additional-done-status_dropdown`}
        showArrow={true}
        value={value}
        mode="multiple"
        options={options}
        onChange={(value: any) => onFilterValueChange(value, valueKey)}
        loading={excludeFiltersLoading}
      />
    </Form.Item>
  );
};

export default SprintDoneStatusSelectComponent;
