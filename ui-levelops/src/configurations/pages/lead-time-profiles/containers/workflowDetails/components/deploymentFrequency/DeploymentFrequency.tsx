import {
  WorkflowIntegrationType,
  RestDeploymentFrequency,
  getDefaultCalculationField,
  CICDFilter,
  SCMFilter,
  getDefaultIntegrationFilters,
  IMFilter
} from "classes/RestWorkflowProfile";
import { sectionSelectedFilterType } from "configurations/configuration-types/OUTypes";
import { getIntegrationType } from "helper/integration.helper";
import { useAllIntegrationState } from "custom-hooks/useAllIntegrationState";
import { Integration } from "model/entities/Integration";
import React, { useCallback, useMemo, useState } from "react";
import { Divider } from "antd";
import { AntText } from "shared-resources/components";
import IntegrationFilterSelector from "../../../../helpers/IntegrationFilterSelector";
import EventComponent from "../cicdEvents/EventComponent";
import IssueManagement from "../issueManagementFilter/IssueManagement";
import SCMConfigurations from "../scmDefinitions/SCMConfigurations";
import "./deploymentFrequency.scss";
import { WORKFLOW_PROFILE_TABS } from "../constant";
import IntApplicationSelector from "../int-application-selector/IntApplicationSelector";
import SwitchButtonControl from "../switch-button-control/SwitchButtonControl";
import { azureIntTypeOptions, gitlabIntTypeOptions } from "configurations/pages/lead-time-profiles/helpers/constants";
import IntegrationWarningModal from "../warning-modal/integrationWarningModal";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface DeploymentFrequencyProps {
  deploymentFrequencyConfig: RestDeploymentFrequency;
  onChange: (newValue: any) => void;
  setExclamationFlag: (value: boolean) => void;
}

const DeploymentFrequency: React.FC<DeploymentFrequencyProps> = ({
  deploymentFrequencyConfig,
  onChange,
  setExclamationFlag
}) => {
  const { isLoading, findIntegrationWithId } = useAllIntegrationState();
  const [warningModal, setWarningModal] = useState<boolean>(false);
  const [modifiedIntegration, setModifiedIntegrations] = useState<Integration[] | null>(null);

  const integrationApplication = useMemo(() => {
    if (
      !isLoading &&
      deploymentFrequencyConfig.integration_ids &&
      deploymentFrequencyConfig.integration_ids.length > 0
    ) {
      const selectedInt = findIntegrationWithId(deploymentFrequencyConfig.integration_ids[0]);
      if (selectedInt) {
        return selectedInt.application;
      }
    }
    return "";
  }, [deploymentFrequencyConfig.integration_ids, isLoading, findIntegrationWithId]);

  const header = useMemo(
    () => (
      <>
        <AntText className="medium-16">DEPLOYMENT FREQUENCY</AntText>
        <AntText className="header" type="secondary">
          Deployment frequency is a measure of how often a team successfully releases or deploys code to production.
        </AntText>
      </>
    ),
    []
  );
  deploymentFrequencyConfig.integration_ids;

  const onApplicationChange = useCallback(
    (app: string) => {
      let application = app;
      const integrationType = getIntegrationType(app);
      const defaultCalculationField = getDefaultCalculationField(integrationType, app);
      if (application.includes(IntegrationTypes.AZURE)) {
        application = IntegrationTypes.AZURE;
      }
      const deploymentFreq = new RestDeploymentFrequency(
        null,
        integrationType,
        defaultCalculationField,
        [],
        application
      );
      onChange({
        deployment_frequency: deploymentFreq.json
      });
    },
    [deploymentFrequencyConfig]
  );

  const applicationSelector = useMemo(() => {
    return (
      <div className="py-5">
        <AntText strong className="d-block">
          Which tool do you use to measure a deployment in your team?
        </AntText>
        <div className="py-10 pl-10" style={{ width: "14.5rem" }}>
          <IntApplicationSelector
            value={deploymentFrequencyConfig.application || integrationApplication || ""}
            onChange={onApplicationChange}
            integration_type={deploymentFrequencyConfig.integrationType}
          />
        </div>
      </div>
    );
  }, [deploymentFrequencyConfig.application, onApplicationChange, integrationApplication]);

  const integrationSelector = useMemo(() => {
    const onIntegraiontChange = (intId: string[], integration: Integration[]) => {
      const ids = deploymentFrequencyConfig.integration_ids || [];
      if (ids?.length < intId.length) {
        onChange({
          deployment_frequency: {
            ...deploymentFrequencyConfig.json,
            integration_id: intId[0],
            integration_ids: intId
          }
        });
        setExclamationFlag(true);
      } else {
        setWarningModal(true);
        setModifiedIntegrations(integration);
      }
    };

    return (
      <div className="py-5">
        <AntText strong className="d-block">
          Which existing integrations would you like to use to calculate the deployment frequency?
        </AntText>
        <div className="py-10 pl-10" style={{ width: "20rem" }}>
          <IntegrationFilterSelector
            integration={deploymentFrequencyConfig.integration_ids}
            setIntegration={onIntegraiontChange}
            application={deploymentFrequencyConfig.application}
            integrationType={
              deploymentFrequencyConfig.application === IntegrationTypes.AZURE
                ? [deploymentFrequencyConfig.integrationType]
                : undefined
            }
          />
        </div>
      </div>
    );
  }, [
    deploymentFrequencyConfig.integration_ids,
    deploymentFrequencyConfig.integrationType,
    deploymentFrequencyConfig.application,
    onChange,
    setExclamationFlag
  ]);

  const onIntegrationTypeChange = useCallback(
    (intType: string) => {
      const newFilter = getDefaultIntegrationFilters(
        intType as WorkflowIntegrationType,
        "deployementFrequency",
        [],
        deploymentFrequencyConfig.application
      );
      deploymentFrequencyConfig.filter = {
        deployment_frequency: newFilter
      };
      onChange({
        deployment_frequency: deploymentFrequencyConfig.json
      });
    },
    [onChange, deploymentFrequencyConfig]
  );

  const FilterTypeSelector = useMemo(
    () => (
      <div className="py-5">
        <AntText strong className="d-block">
          How do you want to configure your integration?
        </AntText>
        <div className="py-5 pl-10" style={{ width: "14.5rem" }}>
          <SwitchButtonControl
            value={deploymentFrequencyConfig?.filter.deployment_frequency.integration_type || "SCM"}
            onChange={onIntegrationTypeChange}
            options={gitlabIntTypeOptions}
          />
        </div>
      </div>
    ),
    [deploymentFrequencyConfig.filter.deployment_frequency.integration_type, onIntegrationTypeChange]
  );

  const FilterTypeSelectorAzure = useMemo(
    () => (
      <div className="py-5">
        <AntText strong className="d-block">
          How do you want to configure your integration?
        </AntText>
        <div className="py-5 pl-10" style={{ width: "21.5rem" }}>
          <SwitchButtonControl
            value={deploymentFrequencyConfig?.filter.deployment_frequency.integration_type}
            onChange={onIntegrationTypeChange}
            options={azureIntTypeOptions}
          />
        </div>
      </div>
    ),
    [deploymentFrequencyConfig?.filter.deployment_frequency.integration_type, onIntegrationTypeChange]
  );

  const onProccedHandler = () => {
    const ids = modifiedIntegration?.map((integration: Integration) => integration.id.toString());
    const application = deploymentFrequencyConfig.application;
    const integrationType = deploymentFrequencyConfig.integrationType;
    const defaultCalculationField = getDefaultCalculationField(integrationType, application);
    const deploymentFreq = new RestDeploymentFrequency(
      null,
      integrationType,
      defaultCalculationField,
      [],
      application,
      ids?.[0],
      ids
    );
    onChange({
      deployment_frequency: deploymentFreq.json
    });
    setWarningModal(false);
    setModifiedIntegrations(null);
    setExclamationFlag(true);
  };

  const filterContent = useMemo(() => {
    const { deployment_frequency } = deploymentFrequencyConfig.filter;

    const onIMFilterChange = (updatedConfig: sectionSelectedFilterType[]) => {
      deploymentFrequencyConfig.filter = {
        deployment_frequency: {
          ...deploymentFrequencyConfig.filter.deployment_frequency,
          integration_type: WorkflowIntegrationType.IM,
          filter: updatedConfig
        }
      };
      onChange({
        deployment_frequency: deploymentFrequencyConfig.json
      });
    };
    const onIMConfigChange = (updatedConfig: IMFilter) => {
      deploymentFrequencyConfig.filter = {
        deployment_frequency: {
          ...deploymentFrequencyConfig.filter.deployment_frequency,
          ...updatedConfig
        }
      };
      onChange({
        deployment_frequency: deploymentFrequencyConfig.json
      });
    };

    const onSCMFilterChange = (updatedConfig: SCMFilter) => {
      deploymentFrequencyConfig.filter = {
        deployment_frequency: updatedConfig
      };
      onChange({
        deployment_frequency: deploymentFrequencyConfig.json
      });
    };

    const onCICDFilterChange = (updatedConfig: CICDFilter) => {
      deploymentFrequencyConfig.filter = {
        deployment_frequency: {
          ...deploymentFrequencyConfig.filter.deployment_frequency,
          ...updatedConfig
        }
      };
      onChange({
        deployment_frequency: deploymentFrequencyConfig.json
      });
    };
    if (deploymentFrequencyConfig.integration_ids?.length) {
      switch (deployment_frequency.integration_type) {
        case "IM":
          return (
            <IssueManagement
              config={deployment_frequency.filter}
              calculationType={WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB}
              integrationApplication={integrationApplication || ""}
              integrationIds={deploymentFrequencyConfig.integration_ids}
              onChange={onIMFilterChange}
              configuration={deployment_frequency}
              onIMConfigChange={onIMConfigChange}
            />
          );
        case "SCM":
          return (
            <SCMConfigurations
              config={deployment_frequency}
              calculationType={WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB}
              onChange={onSCMFilterChange}
              filterString="successful deployment"
            />
          );
        case "CICD":
          return (
            <EventComponent
              calculationType={WORKFLOW_PROFILE_TABS.DEPLOYMENT_FREQUENCY_TAB}
              onChange={onCICDFilterChange}
              integrationIds={deploymentFrequencyConfig.integration_ids}
              integrationApplication={integrationApplication || ""}
              config={deployment_frequency}
              selectedApplication={deploymentFrequencyConfig.application}
              allowIncludeAllJobs={true}
            />
          );
      }
    }
    return null;
  }, [deploymentFrequencyConfig, integrationApplication, onChange]);

  return (
    <div className="deployment-frequancy">
      {header}
      {applicationSelector}
      {integrationSelector}
      {deploymentFrequencyConfig.application === IntegrationTypes.GITLAB &&
        deploymentFrequencyConfig.integration_id &&
        FilterTypeSelector}
      {deploymentFrequencyConfig.application === IntegrationTypes.AZURE_NON_SPLITTED &&
        deploymentFrequencyConfig.integration_id &&
        FilterTypeSelectorAzure}
      <Divider />
      {filterContent}
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

export default DeploymentFrequency;
