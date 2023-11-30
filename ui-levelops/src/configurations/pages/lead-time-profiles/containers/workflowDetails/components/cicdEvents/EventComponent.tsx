import React, { useCallback, useEffect, useMemo, useState } from "react";
import { get } from "lodash";
import { useDispatch } from "react-redux";
import queryString from "query-string";
import { useLocation } from "react-router-dom";
import { genericList, restapiClear } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { velocityConfigsGetSelector } from "reduxConfigs/selectors/velocityConfigs.selector";
import { AntText } from "shared-resources/components";
import { CICDFilter } from "classes/RestWorkflowProfile";
import "./eventComponent.scss";
import { CICD_CHECKBOX_DESCRIPTION, CICD_FOOTER_INFO, WORKFLOW_PROFILE_TABS } from "../constant";
import { Card, Checkbox, Collapse, Icon, Steps } from "antd";
import { IntegrationTypes } from "constants/IntegrationTypes";
import Loader from "components/Loader/Loader";
import CICDFilterComponent from "./CICDFilterComponent";
import CICDParams from "./CICDParams";
import CICDJobComponent from "./CICDJobComponent";
import CalculationCicdCriteria from "../calculation-criteria/CalculationCicdCriteria";
interface EventComponentProps {
  onChange: (updatedConfig: CICDFilter) => void;
  calculationType: string;
  integrationIds: string[];
  integrationApplication: string;
  config: CICDFilter;
  selectedApplication?: string;
  allowIncludeAllJobs?: boolean;
  otherTabSelectionType?: string;
}

const EventComponent: React.FC<EventComponentProps> = props => {
  const {
    onChange,
    calculationType,
    integrationIds,
    integrationApplication,
    config,
    selectedApplication,
    allowIncludeAllJobs,
    otherTabSelectionType
  } = props;

  const dispatch = useDispatch();
  const [showErrorModal, setShowErrorModal] = useState<boolean>(false);
  const [filtersValueFetching, setFiltersValueFetching] = useState(false);
  const [options, setOptions] = useState<any[]>([]);
  const location = useLocation();
  let configId: string = (queryString.parse(location.search).configId as string) || "new";

  const velocityConfigsListState = useParamSelector(velocityConfigsGetSelector, {
    config_id: configId
  });
  let newScreenFlag = velocityConfigsListState.is_new;

  const uri = "jenkins_jobs_filter_values";
  const filterKey = "job_normalized_full_name";
  const job_keys = integrationIds.sort().join("_");

  const titleName = integrationApplication === IntegrationTypes.HARNESSNG ? "pipelines" : "jobs";

  const filterValuesState = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: job_keys
  });

  useEffect(() => {
    return () => {
      dispatch(restapiClear("jira_filter_values", "list", "-1"));
      dispatch(restapiClear("jenkins_jobs_filter_values", "list", "-1"));
    };
  }, []);


  useEffect(() => {
    setFiltersValueFetching(true);
    const filters = {
      fields: [filterKey],
      filter: {
        integration_ids: integrationIds
      },
      integration_ids: integrationIds
    };
    dispatch(genericList(uri, "list", filters, null, job_keys));
    setOptions([]);
  }, [integrationIds]);

  useEffect(() => {
    if (filtersValueFetching) {
      const loading = get(filterValuesState, ["loading"], true);
      const error = get(filterValuesState, ["error"], false);
      if (!loading && !error) {
        const path = ["data", "records", "0", filterKey];
        const data = get(filterValuesState, path, []);
        let tempVal: string[] = [];
        const options = data.map((opt: any) => {
          tempVal.push(opt.cicd_job_id);
          return { label: opt.key, value: opt.cicd_job_id };
        });
        setOptions(options);
        if (!config?.event?.values?.length) {
          if (options.length <= 0) {
            onChange({
              ...config,
              event: { ...config.event, values: tempVal, selectedJob: allowIncludeAllJobs ? "ALL" : "MANUALLY" },
              filter: undefined
            });
          } else {
            onChange({
              ...config,
              event: { ...config.event, values: tempVal, selectedJob: allowIncludeAllJobs ? "ALL" : "MANUALLY" }
            });
          }
        } else if (config?.event?.values?.length > 0 && config?.event?.selectedJob === "ALL") {
          onChange({
            ...config,
            event: { ...config.event, values: tempVal, selectedJob: "ALL" }
          });
        }

        setFiltersValueFetching(false);
      }
    }
  }, [filterValuesState, integrationIds, config, allowIncludeAllJobs]);

  const handleJobsChange = useCallback(
    (values: string[], selectdAllJobFlag?: string) => {
      onChange({
        ...config,
        event: { ...config.event, values: values, selectedJob: selectdAllJobFlag || "MANUALLY" }
      });
      if (
        calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB ||
        calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB ||
        calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB
      ) {
        if (values.length <= 0) {
          setShowErrorModal(true);
          return;
        }
      }
    },
    [onChange, config]
  );

  const handleCheckboxIntegration = useCallback(
    (value: boolean, key: string) => {
      let tempCheckboxObject =
        key === "is_ci_job"
          ? { is_ci_job: value, is_cd_job: config.is_cd_job }
          : { is_cd_job: value, is_ci_job: config.is_ci_job };

      onChange({
        ...config,
        ...tempCheckboxObject
      });
    },
    [config, onChange]
  );

  const handleCicdParmChanges = useCallback(
    (values: any) => {
      onChange({
        ...config,
        event: { ...config.event, params: values }
      });
    },
    [onChange, config]
  );

  const handleCicdFilterChanges = useCallback(
    (values: any) => {
      onChange({
        ...config,
        filter: values
      });
    },
    [onChange, config]
  );

  const renderJobNoData = useMemo(() => {
    if (filtersValueFetching) return <Loader />;
    return (
      <div className="parameter-job-nodata mt-15 mb-15">
        <Icon type="info-circle" className="parameter-job-nodata-icon" />
        {` The selected integration does not have any ${titleName} under it. Please try selecting any other integration.`}
      </div>
    );
  }, [filtersValueFetching, titleName]);

  const handleCalculation = (value: any) => {
    onChange({
      ...config,
      calculation_field: value
    });
  };

  const step1 = useMemo(
    () => (
      <Steps.Step
        title={
          <AntText>
            {CICD_CHECKBOX_DESCRIPTION}
            {calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB
              ? "for total deployment."
              : calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB
              ? "for deployments causing failure."
              : ""}
            <span className="checkbox-required">*</span>
          </AntText>
        }
        description={
          <div className="filter-content">
            <div className="checkbox-fields">
              <span>
                <Checkbox
                  checked={config.is_ci_job}
                  onChange={e => handleCheckboxIntegration(e?.target?.checked, "is_ci_job")}
                  className="checkbox-margin-left"></Checkbox>
                Continuous Integration
              </span>
              <span>
                <Checkbox
                  checked={config.is_cd_job}
                  onChange={e => handleCheckboxIntegration(e?.target?.checked, "is_cd_job")}
                  className="checkbox-margin-left"></Checkbox>
                Continuous Delivery
              </span>
            </div>
          </div>
        }
      />
    ),
    [calculationType, handleCheckboxIntegration, config.is_ci_job, config.is_cd_job]
  );

  const step2 = useMemo(
    () => (
      <Steps.Step
        title={
          <AntText>
            {`Please add any additional attributes to `}
            {calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB
              ? "identify total deployments."
              : calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB
              ? "identify deployments causing failure."
              : "calculate the deployment frequency."}
          </AntText>
        }
        description={
          <div className="filter-content">
            {config.event?.values && config.event?.values.length <= 0 
              ? <Loader />
              : 
                <>
                  <CICDJobComponent
                  event={config.event}
                  titleName={titleName}
                  handleJobsChange={handleJobsChange}
                  options={options}
                  calculationType={calculationType}
                  allowIncludeAllJobs={allowIncludeAllJobs}
                  otherTabSelectionType={otherTabSelectionType}
                />
                <CICDFilterComponent
                  onChange={handleCicdFilterChanges}
                  integrationIds={integrationIds}
                  integrationApplication={integrationApplication}
                  filterConfig={config.filter}
                  titleName={titleName}
                  integrationType={config.integration_type}
                  selectedApplication={selectedApplication}
                />
                {![IntegrationTypes.CIRCLECI, IntegrationTypes.DRONECI].includes(
                  integrationApplication as IntegrationTypes
                ) && (
                  <CICDParams
                    event={config.event}
                    onChange={handleCicdParmChanges}
                    titleName={titleName}
                    calculationType={calculationType}
                  />
                )}
              </>
            }      
          </div>
        }
      />
    ),
    [
      calculationType,
      config,
      titleName,
      handleJobsChange,
      options,
      handleCicdFilterChanges,
      integrationIds,
      integrationApplication,
      handleCicdParmChanges,
      allowIncludeAllJobs,
      selectedApplication
    ]
  );

  const step3 = useMemo(() => {
    return (
      <Steps.Step
        title={
          <AntText>
            {calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB
              ? "Total deployment is calculated based on the "
              : calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB
              ? "Deployments causing failure is calculated based on the "
              : "Deployment frequency is calculated based on the "}
            <CalculationCicdCriteria
              calculationRoute={titleName}
              onChange={handleCalculation}
              value={config.calculation_field}
            />
            {` in the selected time range on the dashboard.`}
          </AntText>
        }
      />
    );
  }, [calculationType, config.calculation_field, titleName, handleCalculation]);

  const renderFooterData = useMemo(() => {
    return (
      <div className="footer mt-15 mb-15">
        <Icon type="info-circle" className="footer-icon mr-5" /> {CICD_FOOTER_INFO}
      </div>
    );
  }, [integrationApplication]);

  return (
    <div className="cicd-event-container">
      {options && options.length <= 0 ? (
        (calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB ||
          calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB) &&
        renderJobNoData
      ) : (
        <>
          <Card className="cicd-card">
            <Collapse bordered={false} className="cicd-collapse" defaultActiveKey={[calculationType]}>
              <Collapse.Panel
                key={calculationType}
                header={
                  <div className="flex">
                    <AntText className="mr-10" strong>
                      {calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB
                        ? "Define total deployment"
                        : calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB
                        ? "Define deployments causing failure"
                        : "Define deployment frequency"}
                    </AntText>
                  </div>
                }>
                <Steps direction="vertical" current={-1} className="cicd-definition">
                  {integrationApplication && integrationApplication === IntegrationTypes.HARNESSNG && step1}
                  {step2}
                  {step3}
                </Steps>
              </Collapse.Panel>
            </Collapse>
          </Card>
          {(calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB ||
            calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB) &&
            renderFooterData}
        </>
      )}
    </div>
  );
};

export default EventComponent;
