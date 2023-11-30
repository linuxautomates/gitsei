import { Icon } from "antd";
import { CICDEvents, CICDFilter, WorkflowIntegrationType } from "classes/RestWorkflowProfile";
import { cloneDeep } from "lodash";
import React, { useCallback, useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { clearCICDJobParamsAction, getCICDJobParamsAction } from "reduxConfigs/actions/restapi/workFlowNewAction";
import {
  cicdJobRunParamsSelector,
  cicdJobRunParamsStateSelector
} from "reduxConfigs/selectors/cicdJobRunParamsSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntButton, AntSelect, AntText } from "shared-resources/components";
import { Select } from "antd";
import "./CICDParams.scss";
import { CICD_EXECUTION_FITER_NOTE, CICD_EXECUTION_HARNESS_FITER_NOTE } from "../constant";

interface CICDEventsProps {
  event: CICDEvents;
  onChange: (updatedConfig: CICDFilter) => void;
  titleName: string;
  calculationType: string;
}

const CICDParams: React.FC<CICDEventsProps> = ({ event, onChange, titleName, calculationType }) => {
  const [keyOptions, setKeyOptions] = useState<string[]>([]);

  const dispatch = useDispatch();
  const cicdJobParamsState = useParamSelector(cicdJobRunParamsStateSelector, { id: calculationType });
  const cicdJobParamsData = useParamSelector(cicdJobRunParamsSelector, { id: calculationType });

  useEffect(() => {
    if (event.values && event.values.length > 0) {
      dispatch(getCICDJobParamsAction(calculationType, event.values));
    }
  }, [event.values]);

  useEffect(
    () => () => {
      dispatch(clearCICDJobParamsAction(calculationType));
    },
    []
  );

  const updateParamsChange = (params: any) => {
    onChange(params);
  };

  useEffect(() => {
    if (!cicdJobParamsState.isLoading && !cicdJobParamsState.error && cicdJobParamsState.data) {
      const jobParamKeys = Object.keys(cicdJobParamsState.data || {});
      setKeyOptions(jobParamKeys);
      const paramKeys = Object.keys(event.params || {});
      const cloneParams = cloneDeep(event.params || {});
      let isParamsChanged = false;
      paramKeys.forEach((paramKey: string) => {
        if (!jobParamKeys.includes(paramKey)) {
          delete cloneParams[paramKey];
          isParamsChanged = true;
        }
      });
      isParamsChanged && updateParamsChange(cloneParams);
    }
  }, [cicdJobParamsState]);

  const addParams = () => {
    const params = {
      ...(event.params || {}),
      " ": []
    };
    updateParamsChange(params);
  };

  const handleRemoveFilter = (key: string) => {
    const cloneParams = cloneDeep(event.params);
    if (cloneParams && cloneParams.hasOwnProperty(key)) {
      delete cloneParams[key];
      updateParamsChange(cloneParams);
    }
  };

  const onKeyChange = (newKey: string, index: number) => {
    const paramEntries = Object.keys(event.params || {}).map((key: string) => ({ key, value: event.params[key] }));
    paramEntries[index].key = newKey;
    const newParams = paramEntries.reduce(
      (acc: any, params: { key: string; value: string[] }) => ({
        ...acc,
        [params.key]: params.value
      }),
      {}
    );
    updateParamsChange(newParams);
  };

  const onValuesChange = (newValues: string[], key: string) => {
    const cloneParams = cloneDeep(event.params);
    cloneParams[key] = newValues;
    updateParamsChange(cloneParams);
  };

  const getValues = useCallback(
    (key: string) => {
      const options = cicdJobParamsData?.[key] ?? [];
      return options.map((keyOption: string) => (
        <Select.Option key={keyOption} value={keyOption}>
          {keyOption}
        </Select.Option>
      ));
    },
    [cicdJobParamsData]
  );

  const getKeys = useCallback(
    (key: string) => {
      const selectedKeys = Object.keys(event.params || {});
      return keyOptions
        .filter((keyOption: string) => !selectedKeys.includes(keyOption) && keyOption !== key)
        .map((keyOption: string) => (
          <Select.Option key={keyOption} value={keyOption}>
            {keyOption}
          </Select.Option>
        ));
    },
    [keyOptions, event.params]
  );

  return (
    <div className="cicd-params">
      <AntText strong className="d-block">
        {titleName === "jobs" ? "Job Run Parameters" : "Execution Filters"}
      </AntText>
      <div className="my-5">
        <AntText className="param-note-desc">
          {titleName === "jobs" ? CICD_EXECUTION_FITER_NOTE : CICD_EXECUTION_HARNESS_FITER_NOTE}
        </AntText>
      </div>
      {event.params &&
        Object.keys(event.params || {}).map((paramKey: string, index: number) => (
          <div key={paramKey} className="filter-row">
            <div className="select-content">
              <div className="filter-col">
                <AntSelect
                  showSearch
                  className="filter-col-select"
                  value={paramKey === " " ? undefined : paramKey}
                  onChange={(value: string) => onKeyChange(value, index)}
                  filterOption={(input: any, option: any) =>
                    option.props.value.toLowerCase().includes(input.toLowerCase())
                  }
                  placeholder="select Key">
                  {getKeys(paramKey)}
                </AntSelect>
              </div>
              <AntText className="pt-10">=</AntText>
              <div className="filter-col">
                <AntSelect
                  className="filter-col-select"
                  mode="multiple"
                  showArrow="true"
                  value={event.params[paramKey]}
                  onChange={(values: string[]) => onValuesChange(values, paramKey)}
                  filterOption={(input: any, option: any) =>
                    option.props.value.toLowerCase().includes(input.toLowerCase())
                  }
                  placeholder="select Value">
                  {getValues(paramKey)}
                </AntSelect>
              </div>
            </div>
            <div className="action-filter-col">
              <AntButton onClick={() => handleRemoveFilter(paramKey)}>
                <Icon type="delete" />
              </AntButton>
            </div>
          </div>
        ))}
      <AntButton type="link" onClick={addParams}>
        <Icon type="plus-circle" />
        {`Add ${titleName === "jobs" ? "job run parameters" : "pipeline execution filters"}`}
      </AntButton>
    </div>
  );
};

export default CICDParams;
