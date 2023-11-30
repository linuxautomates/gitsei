import { WebRoutes } from "routes/WebRoutes";
import { toTitleCase } from "utils/stringUtils";
import { INTEGRATIONS_METADATA_OVERRIDE_VALUES } from "../constants";

export enum IntegrationAuthTypes {
  OAUTH = "oauth",
  API_KEY = "apikey",
  PRIVATE_API_KEY = "private_api_key",
  PUBLIC_API_KEY = "public_api_key",
  MULTIPLE_API_KEYS = "multiple_api_keys"
}

export const INTEGRATION_DETAILS = {
  gitlab: [
    {
      type: IntegrationAuthTypes.PRIVATE_API_KEY,
      title: "Private on-premise GitLab Integration",
      description: "Integration with a private instance of GitLab using an API key.",
      subTitle: "Integration with a private instance of GitLab using an API key."
    },
    {
      type: IntegrationAuthTypes.PUBLIC_API_KEY,
      title: "Public on-premise GitLab Integration",
      description: "Integration of on-premise instance of GitLab that is publicly accessible using an API key.",
      subTitle: "Integration of on-premise instance of GitLab that is publicly accessible using an API key."
    }
  ],
  bitbucket: [
    {
      type: IntegrationAuthTypes.PRIVATE_API_KEY,
      title: "Private on-premise Bitbucket ",
      subTitle: "Integrate with a private instance of Bitbucket using username and password.",
      description: "Integrate with a private instance of Bitbucket."
    },
    {
      type: IntegrationAuthTypes.PUBLIC_API_KEY,
      title: "Public on-premise Bitbucket",
      subTitle:
        "Integrate with an on-premise instance of Bitbucket that is publicly accessible using username and password.",
      description: "Integrate with an on-premise instance of Bitbucket that is publicly accessible."
    }
  ]
};

export const getBreadcrumbsForCreatePage = (application: string) => {
  return [
    {
      label: "Integrations",
      path: WebRoutes.integration.list()
    },
    {
      label: toTitleCase(application),
      path: WebRoutes.integration.newIntegration()
    }
  ];
};

export const addIntegrationMetadataOverrides = (application: string, metadata = {}) => {
  if (application in INTEGRATIONS_METADATA_OVERRIDE_VALUES) {
    return {
      ...metadata,
      ...INTEGRATIONS_METADATA_OVERRIDE_VALUES[application]
    };
  }

  return metadata;
};
