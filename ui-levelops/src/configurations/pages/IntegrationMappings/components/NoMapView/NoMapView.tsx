import { Container, FontVariation, Text, ButtonVariation } from "@harness/uicore";
import React from "react";
import NoMapedIntegrations from "@assets/img/NoMappedIntegrations.png";
import "./NoMapView.scss";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useParentProvider } from "contexts/ParentProvider";
import { useAppStore } from "contexts/AppStoreContext";

const NoMapView = ({ setOpenModal }: { setOpenModal: (value: boolean) => void }): JSX.Element => {
  const {
    components: { RbacButton }
  } = useParentProvider();
  const { accountInfo } = useAppStore();
  return (
    <Container className="noMappingViewContainer">
      <img width="352" height="193" src={NoMapedIntegrations} />
      <Text font={{ variation: FontVariation.H4 }}>Seems like you have no integrations mapped to this project</Text>
      <RbacButton
        icon="plus"
        text="Map Integrations"
        variation={ButtonVariation.PRIMARY}
        className="align-items-center"
        onClick={() => {
          console.log("setting open modal");
          setOpenModal(true);
        }}
        permission={{
          permission: PermissionIdentifier.EDIT_SEI_CONFIGURATIONSETTINGS,
          resource: {
            resourceType: ResourceType.SEI_CONFIGURATION_SETTINGS
          },
          resourceScope: {
            accountIdentifier: accountInfo?.identifier || ""
          }
        }}
      />
    </Container>
  );
};

export default NoMapView;
