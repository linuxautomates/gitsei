import React from "react";
import { IntegrationCardWrapper } from "./integration-card-wrapper";
import { getIntegrationUrlMap } from "constants/integrations";
import { Tooltip, Button } from "antd";
import { TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { useParentProvider } from "contexts/ParentProvider";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { ButtonVariation } from "@harness/uicore";
import { useAppStore } from "contexts/AppStoreContext";

export interface AutomatedIntegrationCardComponentProps {
  integration_type: string;
  onInstallClick: (id: string) => void;
  onDetailsClick: (id: string) => void;
  scmDisabled?: boolean;
}

export const AutomatedIntegrationCardComponent = (props: AutomatedIntegrationCardComponentProps) => {
  const integrationSelected = () => {
    props.onInstallClick(props.integration_type);
  };

  const {
    components: { RbacButton }
  } = useParentProvider();
  const { accountInfo } = useAppStore();
  const integration_type = props.integration_type;

  // @ts-ignore
  const integrationMap: any = getIntegrationUrlMap();
  const integrationDetails = integrationMap[integration_type];
  const { title, description, docs_url_slug } = integrationDetails;
  const showInstall = integrationDetails.hasOwnProperty("install") ? integrationDetails.install : true;

  return (
    <IntegrationCardWrapper
      title={title || integration_type}
      description={description}
      type={integration_type}
      tileClickEvent={integrationSelected}
      docs_url_slug={docs_url_slug}>
      {showInstall &&
        (window.isStandaloneApp ? (
          <Tooltip title={props.scmDisabled && TOOLTIP_ACTION_NOT_ALLOWED}>
            <Button type="primary" onClick={integrationSelected} disabled={props.scmDisabled}>
              Install
            </Button>
          </Tooltip>
        ) : (
          <RbacButton
            variation={ButtonVariation.PRIMARY}
            text={"Install"}
            onClick={integrationSelected}
            permission={{
              permission: PermissionIdentifier.CREATE_SEI_CONFIGURATIONSETTINGS,
              resource: {
                resourceType: ResourceType.SEI_CONFIGURATION_SETTINGS
              },
              resourceScope: {
                accountIdentifier: accountInfo?.identifier || ""
              }
            }}
          />
        ))}
      <Button onClick={() => props.onDetailsClick(integration_type)}>Details</Button>
    </IntegrationCardWrapper>
  );
};
