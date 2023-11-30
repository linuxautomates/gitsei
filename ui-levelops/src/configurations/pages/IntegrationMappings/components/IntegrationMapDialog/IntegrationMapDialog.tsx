import React, { useEffect, useState } from "react";
import type { IDialogProps } from "@blueprintjs/core";
import {
  Text,
  Container,
  Button,
  ExpandingSearchInput,
  ButtonVariation,
  FontVariation,
  ModalDialog,
  PageSpinner
} from "@harness/uicore";
import ConnectorsList from "../ConnectorsList/ConnectorsList";
import "./IntegrationMapDialog.scss";
import { debounce, isEmpty, isEqual } from "lodash";
import { WorkspaceModel, WorkspaceMethodType } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { AccountDTO, Project } from "@harness/microfrontends/dist/services/cd-ng";
import { useDispatch } from "react-redux";
import { workspaceRead } from "reduxConfigs/actions/workspaceActions";
import ChangeScreenWarning from "../ChangeScreenWarning/ChangeScreenWarning";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useParentProvider } from "contexts/ParentProvider";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericWorkSpaceUUIDSelector } from "reduxConfigs/selectors/workspace/workspace.selector";

interface IntegrationMapDialogProps {
  openModal: boolean;
  setOpenModal: (value: boolean) => void;
  workspace?: WorkspaceModel;
  selectedProject?: Project;
  accountInfo?: AccountDTO;
  refetch?: () => void;
}

const IntegrationMapDialog = ({
  workspace,
  openModal,
  setOpenModal,
  selectedProject,
  accountInfo,
  refetch
}: IntegrationMapDialogProps) => {
  const [search, setSearch] = useState<string>("");
  const [openWarning, setOpenWarning] = useState<boolean>(false);
  const [selectedIntegrations, setSelectedIntegrations] = useState<number[]>([]);
  const [saving, setSaving] = useState<boolean>(false);
  const method = workspace && !isEmpty(workspace) ? "update" : "create";
  const dispatch = useDispatch();
  const {
    components: { RbacButton }
  } = useParentProvider();
  const workspaceState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method,
    uuid: workspace?.id
  });

  useEffect(() => {
    setSelectedIntegrations(workspace?.integration_ids || []);
  }, [workspace?.integration_ids]);

  const onSearchChange = (searchText: string) => {
    debounce(() => setSearch(searchText), 300);
  };
  const onApplyClick = () => {
    if (workspace && !isEmpty(workspace)) {
      // update workspace
      const _workspace = {
        ...workspace,
        integration_ids: selectedIntegrations
      };
      dispatch(workspaceRead(workspace.id, method, _workspace));
    } else if (selectedProject) {
      // create new workspace
      const _workspace = {
        name: selectedProject.name,
        key: selectedProject.identifier,
        integration_ids: selectedIntegrations,
        orgIdentifier: selectedProject.orgIdentifier
      };
      dispatch(workspaceRead("0", method, _workspace));
    }
    setSaving(true);
  };

  useEffect(() => {
    if (saving) {
      const { loading, error, data } = workspaceState;
      if (!loading) {
        if (!error) {
          refetch?.();
        }
        setSaving(false);
        setOpenModal(false);
      }
    }
  }, [workspaceState, saving]);

  const title = (
    <Container>
      <Container className="mapHeader">
        <Text font={{ variation: FontVariation.H4 }}>Select the integrations to map to this project</Text>
        <Button icon="cross" variation={ButtonVariation.ICON} onClick={() => setOpenModal(false)} />
      </Container>
      <Container className="mapHeader" padding={{ top: "medium" }}>
        <RbacButton
          icon="plus"
          text="New Integration"
          variation={ButtonVariation.SECONDARY}
          className="align-items-center"
          onClick={() => setOpenWarning(true)}
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
        <ExpandingSearchInput
          width={250}
          defaultValue={search as string}
          key={search}
          onChange={onSearchChange}
          autoFocus={false}
        />
      </Container>
    </Container>
  );
  const footer = (
    <Container className="mapFooter">
      <RbacButton
        text="Apply"
        variation={ButtonVariation.PRIMARY}
        onClick={onApplyClick}
        disabled={
          !(selectedIntegrations.length > 0 && !isEqual(workspace?.integration_ids || [], selectedIntegrations))
        }
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
      <Button text="Cancel" variation={ButtonVariation.TERTIARY} onClick={() => setOpenModal(false)} />
    </Container>
  );

  const DialogProps: IDialogProps = {
    isOpen: false,
    usePortal: true,
    autoFocus: true,
    canEscapeKeyClose: true,
    canOutsideClickClose: true,
    isCloseButtonShown: false,
    enforceFocus: false,
    style: { width: 1057, height: 667, borderLeft: 0, padding: 0, position: "relative" },
    onClose: () => setOpenModal(false),
    title
  };

  const WarningDialogProps: IDialogProps = {
    ...DialogProps,
    style: { width: 618, height: 316, borderLeft: 0, padding: 0, position: "relative" },
    onClose: () => setOpenWarning(false),
    title: null
  };

  return (
    <ModalDialog {...DialogProps} isOpen={openModal} className="no-padding" footer={footer}>
      {saving && <PageSpinner message="Saving.." />}
      <Container className="integrationMapDialog">
        <ConnectorsList
          selectedIntegrations={selectedIntegrations}
          onSelectionChange={setSelectedIntegrations}
          searchItem={search}
          setOpenWarning={setOpenWarning}
        />
      </Container>
      <ModalDialog {...WarningDialogProps} isOpen={openWarning}>
        <ChangeScreenWarning setOpen={setOpenWarning} />
      </ModalDialog>
    </ModalDialog>
  );
};

export default IntegrationMapDialog;
