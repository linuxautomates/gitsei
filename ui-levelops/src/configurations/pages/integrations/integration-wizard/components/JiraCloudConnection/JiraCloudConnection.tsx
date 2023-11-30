import { StepWizard } from "@harness/uicore";
import React from "react";
import { getConnectorIconByType, getConnectorTitleIdByType } from "../../IntegrationConnectionWizard.utils";
import Configuration from "./components/Configuration/Configuration";
import SelectProjects from "./components/SelectProjects/SelectProjects";
import css from "./JiraCloudConnection.module.scss";

interface JiraCloudConnectionProps {
  integrationType: string;
}

export default function JiraCloudConnection(props: JiraCloudConnectionProps) {
  const { integrationType } = props;
  return (
    <StepWizard
      icon={getConnectorIconByType(integrationType)}
      iconProps={{ size: 50 }}
      className={css.stepWizard}
      title={getConnectorTitleIdByType(integrationType)}>
      <Configuration
        name={"Install JIRA connect App"}
        subtitle={"Connect SEI to Jira connect app"}
        integrationType={integrationType}
      />
      <SelectProjects
        subtitle={"Select the projects to which you want SEI to collect data"}
        name={"Select JIRA Projects"}
      />
    </StepWizard>
  );
}
