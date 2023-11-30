import { filterFieldType, sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import { useIntegrationFilterConfiguration } from "configurations/pages/Organization/Filters/useIntegrationFilterConfiguration";
import OrgUnitIntegrationFilterField from "configurations/pages/Organization/organization-unit/OrgUnitIntegrationFilterField";
import { cloneDeep } from "lodash";
import React, { useMemo, useState } from "react";
import { AntText } from "shared-resources/components";
import {
  CALCULATION_RELEASED_IN_KEY,
  CALCULATION_RELEASED_IN_LABLE,
  CHANGE_FAILURE_RATE_COMMON_NOTE,
  CICD_FOOTER_INFO,
  IM_COMMON_NOTE,
  IM_FILTER_NOTE,
  WORKFLOW_PROFILE_TABS
} from "../constant";
import FilterRequiredModal from "../filterRequiredModal/FilterRequiredModal";
import "./issueManagement.scss";
import { Card, Collapse, Icon, Radio, Steps } from "antd";
import CalculationCicdCriteria from "../calculation-criteria/CalculationCicdCriteria";
import { IMFilter } from "classes/RestWorkflowProfile";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface IssueManagementProps {
  config: sectionSelectedFilterType[];
  calculationType: string;
  integrationApplication: string;
  integrationIds: string[];
  onChange: (updatedConfig: sectionSelectedFilterType[]) => void;
  leadTimeFilterLabel?: string;
  configuration?: IMFilter;
  onIMConfigChange?: (config: IMFilter) => void;
}

const IssueManagement: React.FC<IssueManagementProps> = ({
  config,
  calculationType,
  integrationApplication,
  integrationIds,
  onChange,
  leadTimeFilterLabel,
  configuration,
  onIMConfigChange
}) => {
  const [allFilterConfig, selectedIntegrationFilters] = useIntegrationFilterConfiguration(
    integrationApplication,
    integrationIds
  );
  const [showErrorModal, setShowErrorModal] = useState<boolean>(false);
  const [dfCalculationFiled, setDfCalculationFiled] = useState<string>(
    (configuration?.calculation_field as any) === "issue_updated_at"
      ? "issue_resolved_at"
      : (configuration?.calculation_field as any)
  );
  const [defaultConfig, setDefaultConfig] = useState<sectionSelectedFilterType[]>([
    {
      key: "",
      param: "",
      value: ""
    }
  ]);
  const filterConfig = config.length === 0 ? [...defaultConfig] : [...config];

  const handleRemoveFilter = (index: number) => {
    if (
      calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB ||
      calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB ||
      calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB
    ) {
      if (filterConfig.length === 1) {
        setShowErrorModal(true);
        return;
      }
    }
    const cloneConfig = cloneDeep(filterConfig);
    cloneConfig.splice(index, 1);
    onChange(cloneConfig);
  };

  const handleIntegrationFieldChanges = (type: filterFieldType, index: number, value: any) => {
    const nfilters = cloneDeep(filterConfig);

    if (nfilters.length) {
      let nField: sectionSelectedFilterType = cloneDeep(nfilters[index]);

      (nField as any)[type] = value;
      if (type === "key") {
        nField.param = "";
        nField.value = "";
      }
      if (type === "param") {
        nField.value = "";
      }
      nfilters[index] = nField;
      onChange(nfilters);
    }
  };

  const addFilter = () => {
    onChange([
      ...filterConfig,
      {
        key: "",
        param: "",
        value: ""
      }
    ]);
  };

  const checkDuplicateValidation = (currentKey: string, selectedIntegrationFilters: []) => {
    const newConfig = config.reduce((acc: any, data: any) => {
      if (data.key && data.key !== currentKey) {
        acc.push(data.key);
      }
      return acc;
    }, []);

    const finalIntigrationList = selectedIntegrationFilters.reduce((acc: any, data: any) => {
      if (!newConfig.includes(data.value)) {
        acc.push(data);
      }
      return acc;
    }, []);

    return finalIntigrationList;
  };

  const commonNoteDescription = useMemo(() => <p className="failed-deployment-desc">Note: {IM_COMMON_NOTE}</p>, []);

  const changeFailureRatecommonNoteDescription = useMemo(
    () => (
      <p className="failed-deployment-desc">
        Note:
        <ul>
          <li>{CHANGE_FAILURE_RATE_COMMON_NOTE}</li>
          <li>{IM_COMMON_NOTE}</li>
        </ul>
      </p>
    ),
    []
  );
  const renderFooterData = useMemo(() => {
    return (
      <div className="footer mt-15 mb-15">
        <Icon type="info-circle" className="footer-icon mr-5" /> {CICD_FOOTER_INFO}
      </div>
    );
  }, []);

  const handleCalculation = (value: any) => {
    onIMConfigChange?.({
      ...configuration,
      calculation_field: value
    } as IMFilter);
  };

  const handleDFCalculation = (value: any) => {
    setDfCalculationFiled(value);
    handleCalculation(value);
  };

  const jiraCalcuationType: any[] = [
    {
      label: "By providing filters applicable on Jira tickets",
      value: "issue_resolved_at"
    },
    {
      label: "By considering Jira releases",
      value: CALCULATION_RELEASED_IN_KEY
    }
  ];

  const step1 = useMemo(
    () => (
      <Steps.Step
        title={
          <AntText>
            {"How you want to calculate Deployment frequency ?"}
            <div className="row mb-10">
              <Radio.Group onChange={(e: any) => handleDFCalculation(e.target.value)} value={dfCalculationFiled}>
                <div className="flex direction-column">
                  {jiraCalcuationType.map((option: any) => {
                    return <Radio value={option.value}>{option.label}</Radio>;
                  })}
                </div>
              </Radio.Group>
            </div>
          </AntText>
        }
      />
    ),
    [dfCalculationFiled, jiraCalcuationType, handleDFCalculation]
  );

  const step2 = useMemo(
    () => (
      <Steps.Step
        title={
          leadTimeFilterLabel ? (
            <>
              <AntText>{leadTimeFilterLabel} </AntText>
              {commonNoteDescription}
            </>
          ) : (
            <>
              <>
                <AntText>{IM_FILTER_NOTE}</AntText>
                <AntText strong>{calculationType}</AntText>
              </>
              {calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB ? (
                <>{changeFailureRatecommonNoteDescription}</>
              ) : (
                <>{commonNoteDescription}</>
              )}
            </>
          )
        }
        description={
          <div className="filter-content">
            {filterConfig.map((field: sectionSelectedFilterType, index: number) => (
              <OrgUnitIntegrationFilterField
                integrationApplication={integrationApplication}
                key={`${field.key}_${index}`}
                apiLoading={false}
                apiRecords={checkDuplicateValidation(field.key, selectedIntegrationFilters || [])}
                index={index}
                field={field}
                handleRemoveFilter={handleRemoveFilter}
                handleFieldChange={handleIntegrationFieldChanges}
                allFiltersConfig={allFilterConfig}
                integrationIds={integrationIds}
                hideFilterText={true}
                addFilter={filterConfig.length - 1 === index ? addFilter : undefined}
              />
            ))}
          </div>
        }
      />
    ),
    [filterConfig, allFilterConfig, selectedIntegrationFilters, calculationType, integrationApplication, integrationIds]
  );

  const step3 = useMemo(
    () => (
      <Steps.Step
        title={
          <AntText>
            {calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB
              ? "Total deployment is calculated based on the "
              : calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB
              ? "Deployments causing failure is calculated based on the "
              : "Deployment frequency is calculated based on the "}
            {calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB &&
            integrationApplication === IntegrationTypes.JIRA &&
            configuration?.calculation_field === CALCULATION_RELEASED_IN_KEY ? (
              CALCULATION_RELEASED_IN_LABLE
            ) : (
              <CalculationCicdCriteria
                calculationRoute={calculationType}
                onChange={handleCalculation}
                value={configuration?.calculation_field as any}
                application={integrationApplication}
              />
            )}
            {` in the selected time range on the insight.`}
          </AntText>
        }
      />
    ),
    [calculationType, configuration, handleCalculation, integrationApplication]
  );

  return (
    <div className="issue-management-config-wrapper">
      <Card className="issue-management-config-container">
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
              {calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB &&
                IntegrationTypes.JIRA === integrationApplication &&
                step1}
              {calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB &&
              IntegrationTypes.JIRA === integrationApplication &&
              dfCalculationFiled === CALCULATION_RELEASED_IN_KEY
                ? ""
                : step2}
              {step3}
            </Steps>
          </Collapse.Panel>
        </Collapse>
      </Card>
      {(calculationType === WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB ||
        calculationType === WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB) &&
        renderFooterData}
      <FilterRequiredModal
        showModal={showErrorModal}
        onOK={() => setShowErrorModal(false)}
        calculationType={calculationType}
      />
    </div>
  );
};

export default IssueManagement;
