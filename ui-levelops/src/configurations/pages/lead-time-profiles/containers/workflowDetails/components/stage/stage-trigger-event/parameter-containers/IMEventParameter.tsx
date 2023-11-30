import { Icon } from "antd";
import { RestStageConfig, TriggerEventType } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";
import { get } from "lodash";
import { Integration } from "model/entities/Integration";
import React, { useCallback, useEffect, useRef, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { FILTER_VALUES_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";
import { AntText, AntSelect, AntFormItem } from "shared-resources/components";

interface IMEventParameterProps {
  stage: RestStageConfig;
  onChange: (value: any) => void;
  integration: Integration;
}

const IMEventParameter: React.FC<IMEventParameterProps> = ({ onChange, integration, stage }) => {
  const [filtersValueFetching, setFiltersValueFetching] = useState(false);
  const [options, setOptions] = useState<any[]>([]);
  const dispatch = useDispatch();

  const type = stage?.event?.type || TriggerEventType.JIRA_STATUS;
  const values = stage?.event?.values || [];
  const uri =
    integration.application === IntegrationTypes.JIRA ? "jira_filter_values" : "issue_management_workitem_values";

  const filterValuesState = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: FILTER_VALUES_LIST_ID
  });

  useEffect(() => {
    if (filtersValueFetching) {
      const loading = get(filterValuesState, ["loading"], true);
      const error = get(filterValuesState, ["error"], false);
      if (!loading && !error) {
        const path = ["data", "records", "0", "status"];
        const data = get(filterValuesState, path, []);
        data.hasOwnProperty("records");
        let finalData = data.hasOwnProperty("records") ? data.records : data;

        const options = finalData.map((opt: any) => ({ label: opt.key, value: opt.key }));
        setOptions(options);
        setFiltersValueFetching(false);
      }
    }
  }, [filterValuesState]);

  const onOptionFilter = useCallback((value: string, option: any) => {
    if (!value) return true;
    return (option?.label || "").toLowerCase().includes(value.toLowerCase());
  }, []);

  const unMountRefToUpdate = useRef(options);

  useEffect(() => {
    unMountRefToUpdate.current = [
      {
        options,
        stage: stage.json,
        val: values
      }
    ];
  }, [options, values, stage]);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("jira_filter_values", "list", "-1"));
    };
  }, []);

  useEffect(() => {
    const filters = {
      fields: ["status"]
    };
    dispatch(genericList(uri, "list", filters, null, FILTER_VALUES_LIST_ID));
    setOptions([]);
    setFiltersValueFetching(true);
  }, [type]);

  return (
    <div className="event-param-container">
      <AntFormItem label="Event Parameters" required>
        <AntSelect
          mode="multiple"
          className="event-selector"
          value={values}
          placeholder="Any Statuses"
          options={options}
          loading={filterValuesState?.loading}
          showSearch
          showArrow
          onChange={onChange}
          onOptionFilter={onOptionFilter}
          maxTagCount={2}
        />
      </AntFormItem>
    </div>
  );
};

export default IMEventParameter;
