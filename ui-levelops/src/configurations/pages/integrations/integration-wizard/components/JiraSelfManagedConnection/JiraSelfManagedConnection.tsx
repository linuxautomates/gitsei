import { StepWizard } from "@harness/uicore";
import React from "react";
import { getConnectorIconByType, getConnectorTitleIdByType } from "../../IntegrationConnectionWizard.utils";
import Configuration from "./components/Configuration/Configuration";
import ValidateConnection from "./components/ValidateConnection/ValidateConnection";
import css from "./JiraSelfManagedConnection.module.scss";

interface JiraSelfManagedConnectionProps {
  integrationType: string;
}

export default function JiraSelfManagedConnection(props: JiraSelfManagedConnectionProps) {
  const { integrationType } = props;
  return (
    <StepWizard
      icon={getConnectorIconByType(integrationType)}
      iconProps={{ size: 50 }}
      className={css.stepWizard}
      title={getConnectorTitleIdByType(integrationType)}>
      <Configuration
        name={"Provide Jira Details"}
        subtitle={"Provide Jira API credentials and JQL queries to ingest data"}
        integrationType={integrationType}
      />
      <ValidateConnection name={"Validate Connection"} />
    </StepWizard>
  );
}
