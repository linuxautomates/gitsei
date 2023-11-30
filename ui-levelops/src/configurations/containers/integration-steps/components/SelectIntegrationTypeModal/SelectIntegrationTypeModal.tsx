import { IconName } from "@blueprintjs/core";
import { ModalDialog, Layout, Text, FontVariation, Card, CardBody, Color } from "@harness/uicore";
import IntegrationConnectionWizard from "configurations/pages/integrations/integration-wizard/IntegrationConnectionWizard";
import { useDrawer } from "custom-hooks/useDrawerHook/useDrawerHook";
import React, { useEffect, useState } from "react";
import { SELECT_TYPE_DRAWER_OPTIONS } from "./SelectIntegrationType.constants";
import { SELECT_INTEGRATION_TYPE_CONFIG } from "./SelectIntegrationTypeConfig";
import { SelectIntegrationTypeConfig } from "./SelectIntegrationTypeConfig.types";
import css from "./SelectIntegrationType.module.scss";

interface SelectIntegrationTypeModalProps {
  integrationType: string;
  handleClose: () => void;
}

export default function SelectIntegrationTypeModal(props: SelectIntegrationTypeModalProps): JSX.Element {
  const { integrationType, handleClose } = props;
  const [selectedType, setSelectedType] = useState<string>();
  const integrationTypeConfig = SELECT_INTEGRATION_TYPE_CONFIG[integrationType as keyof SelectIntegrationTypeConfig];
  const { title = "", subHeading = "", integrationTypes = [] } = integrationTypeConfig || {};

  const { showDrawer: showConnectionDrawer } = useDrawer({
    createDrawerContent: drawerProps => <IntegrationConnectionWizard {...drawerProps} />,
    drawerOptions: SELECT_TYPE_DRAWER_OPTIONS,
    showConfirmationDuringClose: false
  });

  useEffect(() => {
    if (selectedType) {
      showConnectionDrawer({ integrationType, selectedType });
    }
  }, [selectedType]);

  return (
    <ModalDialog
      isOpen={true}
      onClose={handleClose}
      title={<Text color={Color.WHITE}>{title}</Text>}
      canEscapeKeyClose={false}
      canOutsideClickClose={false}
      enforceFocus={false}
      lazy
      className={css.selectTypeModal}
      width={705}>
      <Layout.Vertical>
        <Text font={{ variation: FontVariation.BODY1 }} height={140} color={Color.WHITE}>
          {subHeading}
        </Text>
        <Layout.Horizontal>
          {integrationTypes.map(el => {
            const { id, icon, typeName, typeNameInfo, iconProps = {} } = el || {};
            return (
              <Layout.Vertical margin={{ right: "xxxlarge" }}>
                <Card interactive selected={selectedType === id} onClick={() => setSelectedType(id)}>
                  <CardBody.Icon icon={icon as IconName} iconProps={iconProps} iconSize={64}></CardBody.Icon>
                </Card>
                <Text font={{ variation: FontVariation.H6 }} padding={{ top: "medium" }} color={Color.WHITE}>
                  {typeName}
                </Text>
                <Text font={{ variation: FontVariation.H6 }} color={Color.WHITE}>
                  {typeNameInfo}
                </Text>
              </Layout.Vertical>
            );
          })}
        </Layout.Horizontal>
      </Layout.Vertical>
    </ModalDialog>
  );
}
