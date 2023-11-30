import { IconName } from "@harness/uicore";
import { IntegrationTypes } from "constants/IntegrationTypes";

export const getConnectorIconByType = (type: string): IconName => {
  switch (type) {
    case IntegrationTypes.JIRA:
      return "service-jira";
    case IntegrationTypes.GITHUB:
      return "github";
    default:
      return "placeholder";
  }
};

export const getConnectorTitleIdByType = (type: string): string => {
  switch (type) {
    case IntegrationTypes.JIRA:
      return "JIRA";
    case IntegrationTypes.GITHUB:
      return "GITHUB";
    default:
      return "placeholder";
  }
};
