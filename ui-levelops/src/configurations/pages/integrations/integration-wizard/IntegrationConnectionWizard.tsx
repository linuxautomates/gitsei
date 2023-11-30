import React from "react";
import JiraCloudConnection from "./components/JiraCloudConnection/JiraCloudConnection";
import JiraSelfManagedConnection from "./components/JiraSelfManagedConnection/JiraSelfManagedConnection";
import { CONNECTION_TYPE } from "configurations/containers/integration-steps/components/SelectIntegrationTypeModal/SelectIntegrationType.constants";

interface IntegrationConnectionWizardProps {
  integrationType: string;
  selectedType: string;
}

export default function IntegrationConnectionWizard(props: IntegrationConnectionWizardProps): JSX.Element {
  const { integrationType, selectedType } = props;

  const renderConnection = (): JSX.Element => {
    switch (selectedType) {
      case CONNECTION_TYPE.JIRA_CLOUD:
        return <JiraCloudConnection integrationType={integrationType} />;
      case CONNECTION_TYPE.JIRA_SELF_MANAGED:
        return <JiraSelfManagedConnection integrationType={integrationType} />;
      default:
        return <></>;
    }
  };

  return <>{renderConnection()}</>;
}
