import { RestStageConfig } from "classes/RestWorkflowProfile";
import { get } from "lodash";
import { Integration } from "model/entities/Integration";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { FILTER_VALUES_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";
import { AntButton, AntFormItem, AntSelect, AntText } from "shared-resources/components";
import ParameterItem from "../../../parameter-item/ParameterItem";
import { Select } from "antd";

interface CICDEventParameterProps {
  stage: RestStageConfig;
  onChange: (value: any) => void;
  onChangeParam: (value: any) => void;
  integration: Integration;
  disabledSelectedJobs?: string[];
}

const CICDEventParameter: React.FC<CICDEventParameterProps> = ({
  onChange,
  integration,
  stage,
  onChangeParam,
  disabledSelectedJobs
}) => {
  const [filtersValueFetching, setFiltersValueFetching] = useState(false);
  const [options, setOptions] = useState<any[]>([]);
  const dispatch = useDispatch();

  const params = stage?.event?.params || {};
  const values = stage?.event?.values || [];
  const uri = "jenkins_jobs_filter_values";

  const filterValuesState = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: FILTER_VALUES_LIST_ID
  });

  const onOptionFilter = useCallback((value: string, option: any) => {
    if (!value) return true;
    return (option?.label || "").toLowerCase().includes(value.toLowerCase());
  }, []);

  const handleAddParameter = useCallback(() => {
    const stageData = stage.json;
    const temp_param = get(stageData, ["event", "params", "New Param"], []);
    onChangeParam({
      ...stageData,
      event: {
        ...stageData?.event,
        params: {
          ...stageData?.event?.params,
          ["New Param"]: temp_param
        }
      }
    });
  }, [stage]);

  useEffect(() => {
    const filters = {
      fields: ["job_normalized_full_name"]
    };
    dispatch(genericList(uri, "list", filters, null, FILTER_VALUES_LIST_ID));
    setOptions([]);
    setFiltersValueFetching(true);
    return () => {
      dispatch(restapiClear("jenkins_jobs_filter_values", "list", "-1"));
    };
  }, []);

  useEffect(() => {}, []);

  useEffect(() => {
    if (filtersValueFetching) {
      const loading = get(filterValuesState, ["loading"], true);
      const error = get(filterValuesState, ["error"], false);
      if (!loading && !error) {
        const path = ["data", "records", "0", "job_normalized_full_name"];
        const data = get(filterValuesState, path, []);
        const options = data.map((opt: any) => ({ label: opt.key, value: opt.cicd_job_id }));
        setOptions(options);
        setFiltersValueFetching(false);
      }
    }
  }, [filterValuesState]);

  const handleDeleteParameter = useCallback(
    (key: string) => {
      const updatedParams = Object.keys(params)
        .filter(param => param !== key)
        .reduce((acc: any, next: any) => {
          return { ...acc, [next]: params[next] };
        }, {});
      const stageData = stage.json;
      onChangeParam({
        ...stageData,
        event: {
          ...stageData.event,
          params: {
            ...updatedParams
          }
        }
      });
    },
    [stage]
  );

  const handleParamNameChange = useCallback(
    (oldName: string, newName: string) => {
      const stageData = stage.json;
      const data = get(stageData, ["event", "params", oldName], []);
      const updatedParams = Object.keys(params || {})
        .filter(key => key !== oldName)
        .reduce((acc: any, next: string) => {
          return {
            ...acc,
            [next]: params[next]
          };
        }, {});
      onChangeParam({
        ...stageData,
        event: {
          ...stageData.event,
          params: {
            ...updatedParams,
            [newName]: data
          }
        }
      });
    },
    [stage]
  );

  const handleParamValuesChange = useCallback(
    (key: string, value: any) => {
      const stageData = stage.json;
      onChangeParam({
        ...stageData,
        event: {
          ...stageData?.event,
          params: {
            ...stageData?.event?.params,
            [key]: value || []
          }
        }
      });
    },
    [stage]
  );

  const parameterList = useMemo(() => {
    const eventParams = stage?.event?.params || {};
    const params = Object.keys(eventParams);

    return (
      <div className="parameter-list">
        {params.map((param, index) => (
          <ParameterItem
            key={index}
            param={param}
            values={eventParams[param] || []}
            onDelete={handleDeleteParameter}
            onNameChange={handleParamNameChange}
            onValuesChange={handleParamValuesChange}
          />
        ))}
      </div>
    );
  }, [stage]);

  const jobsDropdownSelection = useMemo(() => {
    return options.map((job: { label: string; value: string }) => {
      const disabledFlag = disabledSelectedJobs?.includes(job.value) && !values.includes(job.value);

      return (
        <Select.Option value={job.value} key={job.value} label={job.label} disabled={disabledFlag}>
          {job.label}
        </Select.Option>
      );
    });
  }, [options, disabledSelectedJobs]);

  return (
    <div className="event-param-container">
      <div className="parameter-header">
        <AntFormItem label="Event Parameters" required></AntFormItem>

        <AntButton
          className="add-event-parameter-btn"
          size="small"
          type="link"
          icon="plus"
          onClick={handleAddParameter}>
          Add parameter
        </AntButton>
      </div>
      <AntSelect
        mode="multiple"
        className="event-selector"
        value={values}
        placeholder={"Any Jobs"}
        loading={filterValuesState?.loading}
        showSearch
        showArrow
        onChange={onChange}
        onOptionFilter={onOptionFilter}
        maxTagCount={2}>
        {jobsDropdownSelection}
      </AntSelect>
      {parameterList}
    </div>
  );
};

export default CICDEventParameter;
