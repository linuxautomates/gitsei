import {
  CICDFilter,
  getDefaultCalculationField,
  getDefaultIntegrationFilters,
  IMFilter,
  RestChangeFailureRate,
  SCMFilter,
  WorkflowIntegrationType
} from "classes/RestWorkflowProfile";
import IntegrationFilterSelector from "configurations/pages/lead-time-profiles/helpers/IntegrationFilterSelector";
import React, { useCallback, useMemo, useState } from "react";
import { AntText } from "shared-resources/components";
import { Divider } from "antd";
import ChangeFailureRateInfoPage from "./ChangeFailureRateInfo";
import { Integration } from "model/entities/Integration";
import { getIntegrationType } from "helper/integration.helper";
import IssueManagement from "../issueManagementFilter/IssueManagement";
import SCMConfigurations from "../scmDefinitions/SCMConfigurations";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import { sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import EventComponent from "../cicdEvents/EventComponent";
import {
  CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT,
  CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT,
  WORKFLOW_PROFILE_TABS
} from "../constant";
import { get } from "lodash";
import IntApplicationSelector from "../int-application-selector/IntApplicationSelector";
import SwitchButtonControl from "../switch-button-control/SwitchButtonControl";
import { azureIntTypeOptions, gitlabIntTypeOptions } from "configurations/pages/lead-time-profiles/helpers/constants";
import IntegrationWarningModal from "../warning-modal/integrationWarningModal";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface ChangeFailureRateProps {
  changeFailureRateConfig: RestChangeFailureRate;
  onChange: (newValue: any) => void;
  setExclamationFlag: (value: boolean) => void;
}

const ChangeFailureRatePage: React.FC<ChangeFailureRateProps> = ({
  changeFailureRateConfig,
  onChange,
  setExclamationFlag
}) => {
  const { isLoading, findIntegrationWithId } = useAllIntegrationState();
  const [warningModal, setWarningModal] = useState<boolean>(false);
  const [modifiedIntegration, setModifiedIntegrations] = useState<Integration[] | null>(null);

  const integrationApplication = useMemo(() => {
    if (!isLoading && changeFailureRateConfig.integration_ids && changeFailureRateConfig.integration_ids.length > 0) {
      const selectedInt = findIntegrationWithId(changeFailureRateConfig.integration_ids[0]);
      if (selectedInt) {
        return selectedInt.application;
      }
    }
    return "";
  }, [changeFailureRateConfig.integration_ids, isLoading, findIntegrationWithId]);

  const onIntegraiontChange = useCallback(
    (intId: string[], integration: Integration[]) => {
      const ids = changeFailureRateConfig.integration_ids || [];
      if (ids?.length < intId.length) {
        onChange({
          change_failure_rate: {
            ...changeFailureRateConfig.json,
            integration_id: intId[0],
            integration_ids: intId
          }
        });
        setExclamationFlag(true);
      } else {
        setWarningModal(true);
        setModifiedIntegrations(integration);
      }
    },
    [changeFailureRateConfig.is_absolute, onChange, setExclamationFlag]
  );

  const onProccedHandler = () => {
    const ids = modifiedIntegration?.map((integration: Integration) => integration.id.toString());
    const application = changeFailureRateConfig.application;
    const integrationType = changeFailureRateConfig.integrationType;
    const defaultCalculationField = getDefaultCalculationField(integrationType, application);
    const changeFailure = new RestChangeFailureRate(
      null,
      integrationType,
      defaultCalculationField,
      [],
      application,
      ids?.[0],
      ids
    );
    onChange({
      change_failure_rate: changeFailure.json
    });
    setWarningModal(false);
    setModifiedIntegrations(null);
    setExclamationFlag(true);
  };

  const handleChangesCheckBox = (value: boolean) => {
    changeFailureRateConfig.is_absolute = value;
    onChange({
      change_failure_rate: changeFailureRateConfig.json
    });
  };

  const onApplicationChange = useCallback(
    (app: string) => {
      let application = app;
      if (application.includes(IntegrationTypes.AZURE)) {
        application = IntegrationTypes.AZURE;
      }

      const integrationType = getIntegrationType(app);
      const defaultCalculationField = getDefaultCalculationField(integrationType, app);
      if (application.includes(IntegrationTypes.AZURE)) {
        application = IntegrationTypes.AZURE;
      }
      const changeFailure = new RestChangeFailureRate(null, integrationType, defaultCalculationField, [], application);
      onChange({
        change_failure_rate: changeFailure.json
      });
    },
    [changeFailureRateConfig]
  );

  const applicationSelector = useMemo(() => {
    return (
      <div className="py-5">
        <AntText strong className="d-block">
          Which tool do you use to measure a deployment in your team?
        </AntText>
        <div className="py-5 pl-10" style={{ width: "14.5rem" }}>
          <IntApplicationSelector
            value={changeFailureRateConfig.application || integrationApplication || ""}
            onChange={onApplicationChange}
            integration_type={changeFailureRateConfig.integrationType}
          />
        </div>
      </div>
    );
  }, [changeFailureRateConfig.application, onApplicationChange, integrationApplication]);

  const integrationSelector = useMemo(
    () => (
      <div className="py-5">
        <AntText strong className="d-block">
          Which existing integrations would you like to use to calculate the change failure rate?
        </AntText>
        <div className="py-5 pl-10" style={{ width: "14.5rem" }}>
          <IntegrationFilterSelector
            integration={changeFailureRateConfig.integration_ids}
            setIntegration={onIntegraiontChange}
            application={changeFailureRateConfig.application}
            integrationType={
              changeFailureRateConfig.application === IntegrationTypes.AZURE
                ? [changeFailureRateConfig.integrationType]
                : undefined
            }
          />
        </div>
      </div>
    ),
    [
      changeFailureRateConfig.integration_id,
      changeFailureRateConfig.integration_ids,
      changeFailureRateConfig.integrationType,
      changeFailureRateConfig.application,
      onIntegraiontChange
    ]
  );

  const onIntegrationTypeChange = useCallback(
    (intType: string) => {
      changeFailureRateConfig.filter = {
        failed_deployment: getDefaultIntegrationFilters(
          intType as WorkflowIntegrationType,
          "failedDeployment",
          [],
          changeFailureRateConfig.application
        ),
        total_deployment: getDefaultIntegrationFilters(
          intType as WorkflowIntegrationType,
          "totalDeployment",
          [],
          changeFailureRateConfig.application
        )
      };
      onChange({
        change_failure_rate: changeFailureRateConfig.json
      });
    },
    [onChange, changeFailureRateConfig]
  );

  const FilterTypeSelector = useMemo(
    () => (
      <div className="py-5">
        <AntText strong className="d-block">
          How do you want to configure your integration?
        </AntText>
        <div className="py-5 pl-10" style={{ width: "14.5rem" }}>
          <SwitchButtonControl
            value={changeFailureRateConfig?.filter.failed_deployment.integration_type}
            onChange={onIntegrationTypeChange}
            options={gitlabIntTypeOptions}
          />
        </div>
      </div>
    ),
    [changeFailureRateConfig?.filter.failed_deployment.integration_type, onIntegrationTypeChange]
  );

  const FilterTypeSelectorAzure = useMemo(
    () => (
      <div className="py-5">
        <AntText strong className="d-block">
          How do you want to configure your integration?
        </AntText>
        <div className="py-5 pl-10" style={{ width: "21.5rem" }}>
          <SwitchButtonControl
            value={changeFailureRateConfig?.filter.failed_deployment.integration_type}
            onChange={onIntegrationTypeChange}
            options={azureIntTypeOptions}
          />
        </div>
      </div>
    ),
    [changeFailureRateConfig.filter.failed_deployment.integration_type, onIntegrationTypeChange]
  );

  const filterContent = useCallback(
    (filterObject: IMFilter | SCMFilter | CICDFilter, filterType: "failed_deployment" | "total_deployment") => {
      const onIMFilterChange = (
        updatedConfig: sectionSelectedFilterType[],
        filterType: "failed_deployment" | "total_deployment"
      ) => {
        const { filter } = changeFailureRateConfig;
        changeFailureRateConfig.filter = {
          ...filter,
          [filterType]: {
            ...changeFailureRateConfig.filter?.[filterType],
            integration_type: "IM",
            filter: updatedConfig
          }
        };
        onChange({
          change_failure_rate: changeFailureRateConfig.json
        });
      };

      const onIMConfigChange = (
        updatedConfig: IMFilter,
        filterType: "failed_deployment" | "total_deployment" = "total_deployment"
      ) => {
        changeFailureRateConfig.filter = {
          ...changeFailureRateConfig.filter,
          [filterType]: {
            ...changeFailureRateConfig.filter?.[filterType],
            ...updatedConfig
          }
        };
        onChange({
          change_failure_rate: changeFailureRateConfig.json
        });
      };

      const onSCMFilterChange = (updatedFilter: SCMFilter, filterType: "failed_deployment" | "total_deployment") => {
        const { filter } = changeFailureRateConfig;
        changeFailureRateConfig.filter = {
          ...filter,
          [filterType]: {
            ...updatedFilter
          }
        };
        onChange({
          change_failure_rate: changeFailureRateConfig.json
        });
      };

      const onCICDFilterChange = (updatedConfig: CICDFilter, filterType: "failed_deployment" | "total_deployment") => {
        const { filter } = changeFailureRateConfig;
        const filterTypeData = get(filter, [filterType], {});

        changeFailureRateConfig.filter = {
          ...filter,
          [filterType]: {
            ...filterTypeData,
            ...updatedConfig
          }
        };
        onChange({
          change_failure_rate: changeFailureRateConfig.json
        });
      };

      const calculationFilterType =
        filterType === CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT
          ? WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT_TAB
          : WORKFLOW_PROFILE_TABS.CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT_TAB;
      const filterString = filterType === "failed_deployment" ? "deployments causing failure" : "total deployment";

      const filtersForChangeFailureRate: any = changeFailureRateConfig.filter;
      let otherTabSelectionType =
        filterType === "failed_deployment"
          ? filtersForChangeFailureRate.total_deployment?.event?.selectedJob || "ALL"
          : filtersForChangeFailureRate.failed_deployment?.event?.selectedJob || "ALL";

      switch (filterObject.integration_type) {
        case "IM":
          return (
            <IssueManagement
              config={filterObject.filter}
              calculationType={calculationFilterType}
              integrationApplication={integrationApplication || ""}
              integrationIds={changeFailureRateConfig.integration_ids as string[]}
              onChange={updatedConfig => onIMFilterChange(updatedConfig, filterType)}
              configuration={filterObject}
              onIMConfigChange={updatedConfig => onIMConfigChange(updatedConfig, filterType)}
            />
          );
        case "SCM":
          return (
            <SCMConfigurations
              config={filterObject}
              calculationType={calculationFilterType}
              onChange={updatedConfig => onSCMFilterChange(updatedConfig, filterType)}
              filterString={filterString}
            />
          );
        case "CICD":
          return (
            <EventComponent
              calculationType={calculationFilterType}
              onChange={updatedConfig => onCICDFilterChange(updatedConfig, filterType)}
              integrationIds={changeFailureRateConfig.integration_ids as string[]}
              integrationApplication={integrationApplication || ""}
              config={filterObject}
              selectedApplication={changeFailureRateConfig.application}
              allowIncludeAllJobs={true}
              otherTabSelectionType={otherTabSelectionType}
            />
          );
      }
    },
    [changeFailureRateConfig, integrationApplication, onChange]
  );

  const filterContainer = useMemo(() => {
    const { failed_deployment, total_deployment } = changeFailureRateConfig.filter;

    if (changeFailureRateConfig.integration_id) {
      return (
        <>
          {filterContent(failed_deployment, CHANGE_FAILURE_RATE_FAILED_DEPLOYMENT)}
          {!changeFailureRateConfig.is_absolute &&
            filterContent(total_deployment, CHANGE_FAILURE_RATE_TOTAL_DEPLOYMENT)}
        </>
      );
    }
    return null;
  }, [changeFailureRateConfig, filterContent]);

  return (
    <div className="change-failure-rate">
      <AntText className="medium-16">CHANGE FAILURE RATE</AntText>
      <ChangeFailureRateInfoPage
        checkBoxValue={changeFailureRateConfig.is_absolute}
        handleChanges={handleChangesCheckBox}
      />
      <Divider />
      {applicationSelector}
      {integrationSelector}
      {changeFailureRateConfig.application === IntegrationTypes.GITLAB &&
        changeFailureRateConfig.integration_id &&
        FilterTypeSelector}
      {changeFailureRateConfig.application === IntegrationTypes.AZURE_NON_SPLITTED &&
        changeFailureRateConfig.integration_id &&
        FilterTypeSelectorAzure}
      <Divider />
      {filterContainer}
      {warningModal && (
        <IntegrationWarningModal
          visibility={warningModal}
          setVisibility={setWarningModal}
          handleClickProceedButton={onProccedHandler}
        />
      )}
    </div>
  );
};

export default ChangeFailureRatePage;
