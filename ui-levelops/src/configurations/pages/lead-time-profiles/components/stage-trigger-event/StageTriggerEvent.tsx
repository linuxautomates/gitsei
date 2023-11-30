import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { Icon, Radio, Select, Tooltip } from "antd";
import FormItem from "antd/lib/form/FormItem";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import { CICD_TRIGGER_EVENT_TYPE, ISSUE_MANAGEMENT_TRIGGER_TYPE, RestVelocityConfigStage, TriggerEventType } from "classes/RestVelocityConfigs";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { FILTER_VALUES_LIST_ID } from "reduxConfigs/selectors/velocityConfigs.selector";
import { AntSelect, AntText } from "shared-resources/components";
import { IntegrationTypes } from "constants/IntegrationTypes";
import Loader from "components/Loader/Loader";
import CICDParams from "../../containers/workflowDetails/components/cicdEvents/CICDParams";
import CICDFilterComponent from "../../containers/workflowDetails/components/cicdEvents/CICDFilterComponent";
import {
  STAGE_TRIGGER_EVENT_AFTER_MESSAGE,
  STAGE_TRIGGER_EVENT_BEFOR_MESSAGE,
  STAGE_TRIGGER_EVENT_DISABLED_MESSAGE,
  STAGE_TRIGGER_EVENT_HARNESSCD_DISABLED_MESSAGE,
  STAGE_TRIGGER_EVENT_HARNESS_DISABLED_MESSAGE
} from "../../helpers/constants";
import CICDJobComponent from "../../containers/workflowDetails/components/cicdEvents/CICDJobComponent";
import { IssueManagementOptions } from "constants/issueManagementOptions";

interface StageTriggerEventProps {
  stage: RestVelocityConfigStage;
  onChange: (val: any) => void;
  issueManagementSystem?: string;
  disabledSelectedEventTypeOption?: string[];
  currentSelectedEventType?: string;
  jira_only?: boolean;
}

const StageTriggerEvent: React.FC<StageTriggerEventProps> = props => {
  const { stage, onChange, disabledSelectedEventTypeOption, currentSelectedEventType, jira_only } = props;

  const params = stage?.event?.params || {};
  const values = stage?.event?.values || [];
  const type = stage?.event?.type || TriggerEventType.JIRA_STATUS;
  const isIssueManagement = [TriggerEventType.JIRA_STATUS, TriggerEventType.WORKITEM_STATUS].includes(
    type as TriggerEventType
  );
  const isJiraIssueManagement = props.issueManagementSystem === IssueManagementOptions.JIRA;
  const uri = isIssueManagement
    ? isJiraIssueManagement
      ? "jira_filter_values"
      : "issue_management_workitem_values"
    : "jenkins_jobs_filter_values";
  const filterKey = isIssueManagement ? "status" : "job_normalized_full_name";
  const dispatch = useDispatch();

  const [filtersValueFetching, setFiltersValueFetching] = useState(false);
  const [radioButtonValue, setRadioButtonValue] = useState(type);
  const [options, setOptions] = useState<any[]>([]);

  const filterValuesState = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: FILTER_VALUES_LIST_ID
  });

  const unMountRefToUpdate = useRef(options);

  const titleName = [TriggerEventType.HARNESSCD_JOB_RUN, TriggerEventType.HARNESSCI_JOB_RUN].includes(
    type as TriggerEventType
  )
    ? "pipelines"
    : "jobs";

  useEffect(() => {
    unMountRefToUpdate.current = [
      {
        options,
        stage: stage.json,
        val: values
      }
    ];
  }, [options, values, stage, isIssueManagement]);


  useEffect(() => {
    return () => {
      const { options, stage, val } = unMountRefToUpdate.current[0] || [];
      if (val.length === 0) {
        updateStageEventData(
          options.map((val: any) => val.value),
          stage
        );
      }
      dispatch(restapiClear("jira_filter_values", "list", "-1"));
      dispatch(restapiClear("jenkins_jobs_filter_values", "list", "-1"));
    };
  }, []);

  useEffect(() => {
    let filters = {
      fields: [filterKey]
    };
    let harnessFilter = {
      types: [IntegrationTypes.HARNESSNG]
    };
    let githubActionsFilter = {
      types: [IntegrationTypes.GITHUB_ACTIONS]
    }

    if (type === TriggerEventType.HARNESSCD_JOB_RUN) {
      filters = {
        ...filters,
        ...{
          filter: {
            ...harnessFilter,
            is_cd_job: true
          }
        }
      };
    }
    if (type === TriggerEventType.HARNESSCI_JOB_RUN) {
      filters = {
        ...filters,
        ...{
          filter: {
            ...harnessFilter,
            is_ci_job: true
          }
        }
      };
    }
    if (type === TriggerEventType.CICD_JOB_RUN) {
      filters = {
        ...filters,
        ...{
          filter: {
            exclude: {
              types: [IntegrationTypes.HARNESSNG, IntegrationTypes.GITHUB_ACTIONS]
            }
          }
        }
      };
    }
    if (type === TriggerEventType.GITHUB_ACTIONS_JOB_RUN) {
      filters = {
        ...filters,
        ...{
          filter: {
            ...githubActionsFilter,
          }
        }
      };
    }
    dispatch(genericList(uri, "list", filters, null, FILTER_VALUES_LIST_ID));
    setOptions([]);
    setFiltersValueFetching(true);
  }, [type]);

  useEffect(() => {
    if (filtersValueFetching) {
      const loading = get(filterValuesState, ["loading"], true);
      const error = get(filterValuesState, ["error"], false);
      if (!loading && !error) {
        const path =
          isIssueManagement && props.issueManagementSystem === IssueManagementOptions.AZURE
            ? ["data", "records", "0", filterKey, "records"]
            : ["data", "records", "0", filterKey];
        const data = get(filterValuesState, path, []);
        let selectedCicdJobs: string[] = [];
        const options = data.map((opt: any) => {
          if (isIssueManagement) {
            return { label: opt.key, value: opt.key };
          } else {
            selectedCicdJobs.push(opt.cicd_job_id);
            return { label: opt.key, value: opt.cicd_job_id };
          }
        });
        setOptions(options);
        if (!isIssueManagement) {
          let jobsSelected: string[] = [];
          let selectedJob: string = "MANUALLY";
          if (stage.event?.values && stage.event?.values.length > 0) {
            jobsSelected = stage.event?.values;
          } else {
            jobsSelected = selectedCicdJobs;
            selectedJob = "ALL";
          }
          handleJobsChange(jobsSelected || [], selectedJob);
        }
        setFiltersValueFetching(false);
      }
    }
  }, [filterValuesState, stage]);

  const triggerEventOptions: any[] = [
    {
      label: "Other CICD tools",
      value: TriggerEventType.CICD_JOB_RUN,
      info: "Elapsed time for the execution of the jobs.",
      disabledMessage: STAGE_TRIGGER_EVENT_HARNESS_DISABLED_MESSAGE
    },
    {
      label: "Github Actions",
      value: TriggerEventType.GITHUB_ACTIONS_JOB_RUN,
      info: "Elapsed time for the execution of the jobs.",
      disabledMessage: STAGE_TRIGGER_EVENT_HARNESS_DISABLED_MESSAGE
    },
    {
      label: "Harness CI",
      value: TriggerEventType.HARNESSCI_JOB_RUN,
      info: "Elapsed time for the execution of the CI pipelines.",
      disabledMessage: STAGE_TRIGGER_EVENT_DISABLED_MESSAGE
    },
    {
      label: "Harness CD",
      value: TriggerEventType.HARNESSCD_JOB_RUN,
      info: "Elapsed time for the execution of the CD pipelines.",
      disabledMessage: STAGE_TRIGGER_EVENT_HARNESSCD_DISABLED_MESSAGE
    }
  ];

  const triggerRadioButton: any[] = [
    {
      label: "Issue Management",
      value: isJiraIssueManagement ? TriggerEventType.JIRA_STATUS : TriggerEventType.WORKITEM_STATUS,
      info: "Time spent in status.",
      disabledMessage: ""
    },
    {
      label: "CI/CD tools",
      value: TriggerEventType.CICD_JOB_RUN,
      info: "Elapsed time for the execution of the jobs.",
      disabledMessage: STAGE_TRIGGER_EVENT_HARNESS_DISABLED_MESSAGE
    },
  ];

  const onOptionFilter = useCallback((value: string, option: any) => {
    if (!value) return true;
    return (option?.label || "").toLowerCase().includes(value.toLowerCase());
  }, []);

  const updateStageEventData = (values: any, stageData: any) => {
    onChange({
      ...stageData,
      event: {
        ...stageData.event,
        values
      }
    });
  };

  const handleChange = useCallback(
    (key: string) => {
      return (e: any) => {
        const stageData = stage.json;
        switch (key) {
          case "type":
            onChange({
              ...stageData,
              event: {
                ...stageData.event,
                type: e.target.value,
                values: [],
                params: {}
              },
              filter: []
            });
            break;
          case "filter":
            onChange({
              ...stageData,
              event: {
                ...stageData.event,
                values: e
              }
            });
            break;
          case "cicdType":
            onChange({
              ...stageData,
              event: {
                ...stageData.event,
                type: e,
                values: [],
                params: {}
              }
            });
            break;
        }
      };
    },
    [stage]
  );

  const renderJobNoData = useMemo(() => {
    if (filtersValueFetching) return <Loader />;
    return (
      <div className="stage-event-parameter-job-nodata mt-15 mb-15">
        <Icon type="info-circle" className="stage-event-parameter-job-nodata-icon" />
        {STAGE_TRIGGER_EVENT_BEFOR_MESSAGE} ${titleName} {STAGE_TRIGGER_EVENT_AFTER_MESSAGE}
      </div>
    );
  }, [filtersValueFetching, titleName]);

  const updateFilterData = (values: any) => {
    const stageData = stage.json;
    onChange({
      ...stageData,
      filter: values
    });
  };

  const renderAddFilter = useMemo(() => {
    const stageData = stage.json;
    // REMOVEPROJECTFILTER ADDED THIS FOR HIDE PROJECT FILTER FROM LEAD TIME & MTTR STAGE & VELOCITY PROFILE STAGE
    // ONCE BE SUPPORT THIS THERE THEN WE JUST NEED TO REMOVE THIS CONDITION
    return (
      <CICDFilterComponent
        onChange={updateFilterData}
        integrationApplication={titleName === "pipelines" ? IntegrationTypes.HARNESSNG : IntegrationTypes.CIRCLECI}
        filterConfig={stageData.filter}
        titleName={titleName}
        removeProjectFilter={true}
      />
    );
  }, [titleName, stage, type]);

  const updateParamsData = (values: any) => {
    const stageData = stage.json;
    onChange({
      ...stageData,
      event: {
        ...stageData.event,
        params: values
      }
    });
  };

  const renderAddParameter = useMemo(() => {
    const stageData = stage.json;
    return (
      <CICDParams
        event={stageData.event}
        onChange={updateParamsData}
        titleName={titleName}
        calculationType={"Deployment Frequency"}
      />
    );
  }, [titleName, stage, type]);

  const handleJobsChange = useCallback(
    (values: string[], selectdAllJobFlag?: string) => {
      const stageData = stage.json;
      onChange({
        ...stageData,
        event: {
          ...stageData.event,
          values: values,
          selectedJob: selectdAllJobFlag || "MANUALLY"
        }
      });
    },

    [onChange, stage]
  );

  const renderTriggerEventOptions = useMemo(() => {
    return triggerRadioButton.map((option: any) => {
      return (
        <span>
          <Radio value={option.value}>
            {option.label}
          </Radio>
          <Tooltip title={option.info}>
            <Icon type="info-circle" className="info-circle-radio-button" />
          </Tooltip>
        </span>
      );
    });
  }, [type, handleChange, triggerEventOptions, disabledSelectedEventTypeOption, currentSelectedEventType]);

  const cicdTypeDropdownSelection = useMemo(() => {
    return triggerEventOptions.map((option: any) => {

      let disabledFlag;
      if (TriggerEventType.CICD_JOB_RUN === currentSelectedEventType) {
        //IF CURRENT SELECTION IS CICD_JOB_RUN THEN OPEN ALL OPTION
        disabledFlag = TriggerEventType.HARNESSCD_JOB_RUN === option.value ? true : false;
      } else if (
        disabledSelectedEventTypeOption &&
        TriggerEventType.HARNESSCD_JOB_RUN === option.value &&
        disabledSelectedEventTypeOption?.length <= 0
      ) {
        //HARNESS CD WILL ONLY ENABLE WHEN IN PERVIOUS HARNESS CI IS CONFIGURED
        disabledFlag = true;
      } else if (
        disabledSelectedEventTypeOption &&
        TriggerEventType.HARNESSCI_JOB_RUN === currentSelectedEventType &&
        disabledSelectedEventTypeOption?.length > 0 &&
        disabledSelectedEventTypeOption[0] === TriggerEventType.HARNESSCI_JOB_RUN
      ) {
        disabledFlag = TriggerEventType.HARNESSCD_JOB_RUN === option.value ? true : false;
      } else {
        disabledFlag = disabledSelectedEventTypeOption
          ?.filter(typeData => typeData !== type && typeData !== currentSelectedEventType)
          ?.includes(option.value);
      }

      return (
        <Select.Option
          value={option.value}
          key={option.value}
          label={option.label}
          disabled={disabledFlag}
        >
          <Tooltip title={disabledFlag ? option.disabledMessage : ""}>
            {option.label}{' '}
            {disabledFlag && <Icon type="info-circle" className="info-circle-radio-button" />}
          </Tooltip>
        </Select.Option>
      );
    });
  }, [type, handleChange, triggerEventOptions, disabledSelectedEventTypeOption, currentSelectedEventType]);

  const renderJobSelection = useMemo(() => {
    const stageData = stage.json;
    return (
      <CICDJobComponent
        event={stageData.event}
        titleName={titleName}
        handleJobsChange={handleJobsChange}
        options={options}
        calculationType={"Deployment Frequency"}
        allowIncludeAllJobs={true}
      />
    );
  }, [titleName, stage, type]);

  const handleChangeRadioButton = (value: any) => {
    setRadioButtonValue(value.target.value);
    if ([...ISSUE_MANAGEMENT_TRIGGER_TYPE].includes(value.target.value as TriggerEventType)) {
      const stageData = stage.json;
      onChange({
        ...stageData,
        event: {
          ...stageData.event,
          type: value.target.value,
          values: [],
          params: {}
        },
        filter: []
      });
    }
  }

  return (
    <div className="stage-trigger-event-container">
      <FormItem key="trigger-event" label="Trigger Event">
        {jira_only ? (
          "JIRA"
        ) : (
          <Radio.Group onChange={handleChangeRadioButton} value={[...CICD_TRIGGER_EVENT_TYPE].includes(radioButtonValue as any) ? TriggerEventType.CICD_JOB_RUN : radioButtonValue}>
            {renderTriggerEventOptions}
          </Radio.Group>
        )}
      </FormItem>
      {(![...ISSUE_MANAGEMENT_TRIGGER_TYPE].includes(radioButtonValue as TriggerEventType)) &&
        <>
          <AntText strong className="mb-5">Select tool</AntText>
          <br></br>
          <AntSelect
            mode="single"
            className="w-35"
            value={![...ISSUE_MANAGEMENT_TRIGGER_TYPE].includes(type as TriggerEventType) ? type : ''}
            placeholder={"Select Type"}
            loading={filterValuesState?.loading}
            showArrow
            onChange={handleChange("cicdType")}
          >
            {cicdTypeDropdownSelection}
          </AntSelect>
        </>
      }

      {[...CICD_TRIGGER_EVENT_TYPE].includes(
        radioButtonValue as TriggerEventType
      ) ? (
        options && options.length <= 0 ? (
          renderJobNoData
        ) : (
          <div className="stage-event-parameters parameter-container">
            <div className="stage-event-parameter-label">EVENT PARAMETERS</div>
            {renderJobSelection}
            <div className="stage-event-parameter-filter">
              {renderAddFilter}
              {renderAddParameter}
            </div>
          </div>
        )
      ) : (
        <div className="stage-event-parameters parameter-container">
          <div className="stage-event-parameter-label mb-10">EVENT PARAMETERS</div>
          <AntSelect
            mode="multiple"
            className="w-50"
            value={values}
            placeholder={isIssueManagement ? "Any Statuses" : "Any Jobs"}
            options={options}
            loading={filterValuesState?.loading}
            showSearch
            showArrow
            onChange={handleChange("filter")}
            onOptionFilter={onOptionFilter}
          />
        </div>
      )}
    </div>
  );
};

export default StageTriggerEvent;
