import { Color } from "@harness/uicore";
import { CONNECTION_TYPE } from "./SelectIntegrationType.constants";
import { SelectIntegrationTypeConfig } from "./SelectIntegrationTypeConfig.types";

export const SELECT_INTEGRATION_TYPE_CONFIG: SelectIntegrationTypeConfig = {
  ["jira"]: {
    title: "Choose your JIRA type",
    subHeading: "Jira offers two types of hosting and managing tickets",
    integrationTypes: [
      {
        id: CONNECTION_TYPE.JIRA_CLOUD,
        icon: "main-cloud-providers",
        typeName: "Cloud",
        typeNameInfo: "(SaaS)",
        iconProps: { color: Color.PRIMARY_7 }
      },
      {
        id: CONNECTION_TYPE.JIRA_SELF_MANAGED,
        icon: "service-mydatacenter",
        typeName: "Data Center",
        typeNameInfo: "(Self-managed)"
      }
    ]
  }
};
