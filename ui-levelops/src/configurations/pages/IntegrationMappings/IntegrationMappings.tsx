import {
  ButtonVariation,
  Container,
  ExpandingSearchInput,
  FontVariation,
  ModalDialog,
  PageSpinner,
  Text
} from "@harness/uicore";
import { isEmpty } from "lodash";
import React, { useMemo, useState } from "react";
import NoMapView from "./components/NoMapView/NoMapView";
import "./IntegrationMappings.scss";
import IntegrationMapDialog from "./components/IntegrationMapDialog/IntegrationMapDialog";
import ConnectorsList from "./components/ConnectorsList/ConnectorsList";
import { useAppStore } from "contexts/AppStoreContext";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useParentProvider } from "contexts/ParentProvider";
import ChangeScreenWarning from "./components/ChangeScreenWarning/ChangeScreenWarning";
import { IDialogProps } from "@blueprintjs/core";
import { useWorkspace } from "custom-hooks/useWorkspace";
import RBACNoAccessScreen from "components/RBACNoAccessScreen/RBACNoAccessScreen";

const IntegrationMappings = (): JSX.Element => {
  const [openWarning, setOpenWarning] = useState<boolean>(false);
  const [openModal, setOpenModal] = useState<boolean>(false);
  const [search, setSearch] = useState<string>();

  const WarningDialogProps: IDialogProps = {
    isOpen: false,
    usePortal: true,
    autoFocus: true,
    canEscapeKeyClose: true,
    canOutsideClickClose: true,
    isCloseButtonShown: false,
    enforceFocus: false,
    style: { width: 618, height: 316, borderLeft: 0, padding: 0, position: "relative" },
    onClose: () => setOpenWarning(false),
    title: null
  };

  const { selectedProject, accountInfo } = useAppStore();
  const { identifier: accountId = "" } = accountInfo || {};
  const { identifier: projectIdentifier = "", orgIdentifier = "" } = selectedProject || {};
  const {
    components: { RbacButton },
    hooks: { usePermission }
  } = useParentProvider();
  const [hasViewAccess] = usePermission
    ? usePermission({
        resourceScope: {
          accountIdentifier: accountInfo?.identifier || ""
        },
        resource: {
          resourceType: ResourceType.SEI_CONFIGURATION_SETTINGS
        },
        permissions: [PermissionIdentifier.VIEW_SEI_CONFIGURATIONSETTINGS]
      })
    : [false];
  const { selectedWorkspace, isFetching, refetch } = useWorkspace({
    accountId,
    orgIdentifier,
    projectIdentifier
  });

  const subHeader = (
    <Container className="header">
      <RbacButton
        icon="plus"
        text="Map Integrations"
        variation={ButtonVariation.PRIMARY}
        className="align-items-center"
        onClick={() => setOpenModal(true)}
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
      <ExpandingSearchInput
        width={250}
        defaultValue={search as string}
        key={search}
        onChange={setSearch}
        autoFocus={false}
        throttle={300}
      />
    </Container>
  );

  const header = useMemo(
    () => (
      <Container className="sticky">
        <Container className="header">
          <Text font={{ variation: FontVariation.H4 }}>Integration Mapping</Text>
        </Container>
        {hasViewAccess && subHeader}
      </Container>
    ),
    [hasViewAccess]
  );

  return (
    <Container className="integrationMappings">
      {header}
      {hasViewAccess ? (
        <>
          {isFetching && <PageSpinner />}
          {!isFetching &&
            (isEmpty(selectedWorkspace) ? (
              <NoMapView setOpenModal={setOpenModal} />
            ) : (
              <ConnectorsList
                selectedIntegrations={selectedWorkspace?.integration_ids || []}
                workspaceIntegrations={selectedWorkspace?.integration_ids || undefined}
                searchItem={search}
                setOpenWarning={setOpenWarning}
              />
            ))}
          <IntegrationMapDialog
            workspace={selectedWorkspace}
            setOpenModal={setOpenModal}
            openModal={openModal}
            selectedProject={selectedProject}
            refetch={refetch}
          />
          <ModalDialog {...WarningDialogProps} isOpen={openWarning}>
            <ChangeScreenWarning setOpen={setOpenWarning} />
          </ModalDialog>
        </>
      ) : (
        <RBACNoAccessScreen />
      )}
    </Container>
  );
};

export default IntegrationMappings;
