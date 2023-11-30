import { RouteComponentProps } from "react-router-dom";
import ErrorWrapper from "../../../hoc/errorWrapper";
import React, { useEffect, useMemo, useRef, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { integrationsList, restapiClear, usersGet } from "reduxConfigs/actions/restapi";
import { AntForm, AntFormItem, AntInput, AntModal, AntText } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { ERROR } from "constants/formWarnings";
import { get } from "lodash";
import { INTEGRATION_WARNING, WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";
import { setSelectedWorkspace, workspaceApiClear, workspaceRead } from "reduxConfigs/actions/workspaceActions";
import {
  getGenericWorkSpaceUUIDSelector,
  getSelectedWorkspace
} from "reduxConfigs/selectors/workspace/workspace.selector";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { listOrgUnitsForIntegrations } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { notification, Spin } from "antd";
import "./workspaceCreateEdit.style.scss";
import WorkspaceNameComponent from "./workspaceNameComponent";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { sessionCurrentUserState } from "reduxConfigs/selectors/session_current_user.selector";
import { sessionCurrentUser } from "reduxConfigs/actions/sessionActions";
export interface ProductMappingsContainerProps extends RouteComponentProps {
  product_id: any;
  onUpdate: Function;
  display: boolean;
  onCancel: Function;
}

const WorkspaceCreateEdit: React.FC<ProductMappingsContainerProps> = (props: ProductMappingsContainerProps) => {
  const dispatch = useDispatch();
  const workspaceCreateState = useParamSelector(getGenericWorkSpaceUUIDSelector, { method: "create", uuid: "0" });
  const workspaceGetState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "get",
    uuid: props?.product_id || 0
  });

  const workspaceUpdateState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "update",
    uuid: props?.product_id || 0
  });

  const integrationListState = useParamSelector(getGenericUUIDSelector, {
    uri: "integrations",
    method: "list",
    uuid: "integration_list_workspace"
  });

  const selectedWorkspace = useSelector(getSelectedWorkspace);
  const sessionCurrentUserData = useSelector(sessionCurrentUserState);
  const workspaceIdRef = useRef<any>(undefined);
  const nameFieldRef = useRef<any>(undefined);
  const keyFieldRef = useRef<any>(undefined);

  const [workspace, setWorkspace] = useState<any>({});
  const [deletedIntegrations, setDeletedIntegrations] = useState<any[]>([]);
  const [showWarningModalSheet, setShowWarningModalSheet] = useState<boolean>(false);
  const [integrationDeletion, setIntegrationDeletion] = useState<boolean>(false);
  const [integrationData, setIntegrationData] = useState<any[]>([]);
  const [deletedIdsOrgData, setDeletedIdsOrgData] = useState<any>({});
  const [orgListLoading, setOrgListLoading] = useState<boolean>(false);
  const [userSelect, setUserSelect] = useState<any>(undefined);
  const [orgUnitError, setOrgUnitError] = useState<any>(undefined);
  const [nameExist, setNameExist] = useState<undefined | boolean>(undefined);
  const [isNameValid, setIsNameValid] = useState<boolean>(true);
  const orgUnitListState = useParamSelector(getGenericUUIDSelector, {
    uri: "org_units_for_integration",
    method: "list",
    uuid: deletedIntegrations?.join("_")
  });

  const getUserState = useParamSelector(getGenericUUIDSelector, {
    uri: "users",
    method: "get",
    uuid: workspace?.owner_id
  });

  useEffect(() => {
    if (props.product_id !== undefined) {
      workspaceIdRef.current = props.product_id;
      dispatch(workspaceRead(workspaceIdRef.current, "get"));
    }
    return () => {
      dispatch(workspaceApiClear("0", "create"));
      dispatch(workspaceApiClear(props?.product_id, "update"));
      dispatch(workspaceApiClear(props?.product_id, "get"));
      dispatch(workspaceApiClear("workspace_list", "list"));
      dispatch(workspaceApiClear("name_exist", "list"));
      dispatch(restapiClear("users", "get", workspace?.owner_id));
    };
  }, []);

  useEffect(() => {
    const loading = get(workspaceCreateState, ["loading"], true);
    const error = get(workspaceCreateState, ["error"], true);
    if (!loading && !error) {
      props.onUpdate();
    }
  }, [workspaceCreateState]);

  useEffect(() => {
    const loading = get(workspaceUpdateState, ["loading"], true);
    const error = get(workspaceUpdateState, ["error"], true);
    if (!loading && !error) {
      notification.success({
        message: "Successfully updated a project!"
      });
      props.onUpdate();
    }
  }, [workspaceUpdateState]);

  useEffect(() => {
    const loading = get(workspaceGetState, "loading", true);
    const error = get(workspaceGetState, "error", true);
    if (!loading && !error) {
      const product = get(workspaceGetState, "data", {});
      let _product = {
        ...product,
        integration_ids: (product?.integrations || []).map((item: { id: string; name: string }) => ({
          key: item?.id?.toString(),
          label: item?.name
        }))
      };
      if (product.owner_id !== undefined) {
        dispatch(usersGet(product.owner_id));
      }
      ["integrations", "id", "created_at", "updated_at", "immutable", "bootstrapped"].forEach((item: string) => {
        delete _product?.[item];
      });
      nameFieldRef.current = _product?.name;
      setWorkspace(_product);
    }
  }, [workspaceGetState]);

  useEffect(() => {
    if (integrationDeletion) {
      const id = deletedIntegrations.join("_");
      const filters = {
        integration_ids: deletedIntegrations,
        workspace_id: workspaceIdRef.current
      };
      dispatch(listOrgUnitsForIntegrations(id, filters));
      setOrgListLoading(true);
    }
  }, [integrationDeletion]);

  const onOkPressed = () => {
    const _workspace = {
      ...workspace,
      integration_ids: workspace.integration_ids.map((item: any) => item.key)
    };
    if (selectedWorkspace.id === props.product_id) {
      updateSelectedWorkspace(_workspace);
    }
    dispatch(workspaceRead(workspaceIdRef.current, "update", _workspace));
  };

  useEffect(() => {
    const loading = get(orgUnitListState, "loading", true);
    const error = get(orgUnitListState, "error", true);
    if (!error && !loading) {
      const data = get(orgUnitListState, ["data"], {});
      const integrationids = Object.keys(data || {});
      if (integrationids?.length > 0) {
        setShowWarningModalSheet(true);
      } else {
        setIntegrationDeletion(false);
        onOkPressed();
      }
      setDeletedIdsOrgData(data);
      setOrgListLoading(false);
    } else if (!loading && error) {
      setShowWarningModalSheet(true);
      setOrgUnitError(error);
      setOrgListLoading(false);
    }
  }, [orgUnitListState]);

  useEffect(() => {
    const loading = get(integrationListState, "loading", true);
    const error = get(integrationListState, "error", true);
    if (!error && !loading) {
      const data = get(integrationListState, ["data", "records"], []);
      setIntegrationData(data);
    }
  }, [integrationListState]);

  useEffect(() => {
    const loading = get(getUserState, "loading", true);
    const error = get(getUserState, "error", true);
    if (!loading && !error) {
      const data = get(getUserState, "data", {});
      const user = { key: data.id, label: data.email };
      setUserSelect(user);
    }
  }, [getUserState]);

  const onNameChangeHandler = (value: string) => {
    let _workspace = { ...workspace };
    _workspace = {
      ..._workspace,
      name: value
    };
    setWorkspace(_workspace);
  };

  const onChangeHandler = (field: string) => {
    return (e: any) => {
      let _product = { ...workspace };
      switch (field) {
        case "description":
          _product = {
            ..._product,
            description: e.currentTarget.value
          };
          break;
        case "key":
          _product = {
            ..._product,
            key: e.currentTarget.value
          };
          break;
        case "owner":
          _product = {
            ..._product,
            owner_id: e ? e.key : undefined
          };
          setUserSelect(e || {});
          break;
        case "integration_ids":
          _product = {
            ..._product,
            integration_ids: e
          };
          break;
        default:
          break;
      }
      setWorkspace(_product);
    };
  };

  const onUpdate = () => {
    const _workspace = {
      ...workspace,
      integration_ids: workspace.integration_ids.map((item: any) => parseInt(item.key))
    };
    if (workspaceIdRef.current !== undefined) {
      const previousIds = get(workspaceGetState, ["data", "integrations"], [])?.map((item: any) =>
        item?.id?.toString()
      );
      const newIds = get(_workspace, ["integration_ids"], [])?.map((item: any) => item.toString());
      const deletedIds: any[] = [];
      previousIds.forEach((id: string) => {
        if (!newIds.includes(id)) {
          deletedIds.push(id);
        }
      });
      if (deletedIds.length) {
        setDeletedIntegrations(deletedIds);
        setIntegrationDeletion(true);
      } else {
        if (selectedWorkspace.id === props.product_id) {
          updateSelectedWorkspace(_workspace);
        }
        dispatch(workspaceRead(workspaceIdRef.current, "update", _workspace));
      }
    } else {
      dispatch(workspaceRead("0", "create", _workspace));
    }
  };

  const okButtonProps = useMemo(() => {
    const valid =
      workspace?.name !== undefined &&
      workspace?.name.trim() !== "" &&
      workspace?.key !== undefined &&
      workspace?.key.trim() !== "" &&
      workspace?.owner_id !== undefined &&
      workspace?.integration_ids !== undefined &&
      workspace?.integration_ids?.length > 0 &&
      !nameExist &&
      !workspace?.demo &&
      isNameValid;
    return { disabled: !valid };
  }, [workspace, nameExist]);

  const confirmPopupContent = useMemo(() => {
    if (orgListLoading) {
      return <Spin />;
    }
    if (orgUnitError) {
      return <AntText>{orgUnitError} Something bad happened please try again later</AntText>;
    }
    const integrationids = Object.keys(deletedIdsOrgData || {});
    if (integrationids?.length > 0) {
      const list = integrationids.map((integration: any) => {
        const integData = integrationData?.find((item: any) => item.id === integration);
        return (
          <>
            <AntText>
              Integration <b>{integData?.name || integration}</b> is associated with the following Collections:
            </AntText>
            <div className="org-list">
              <ul>
                {deletedIdsOrgData?.[integration]?.map((unit: any) => (
                  <li>{unit?.ou_name}</li>
                ))}
              </ul>
            </div>
          </>
        );
      });
      return (
        <>
          {list}
          <AntText>Do you still want to proceed with deletion?</AntText>
        </>
      );
    }
  }, [orgListLoading, deletedIdsOrgData, orgUnitError]);

  const updateSelectedWorkspace = (updatedWorkspace: any = {}) => {
    const integrations = (workspace?.integration_ids || []).map((integration: { key: string; label: string }) => {
      const data = integrationData?.find((item: any) => item?.id === integration?.key);
      return {
        append_metadata: data?.append_metadata,
        id: data?.id,
        name: data?.name,
        application: data?.application
      };
    });
    const _updatedWorkspace = {
      ...selectedWorkspace,
      ...updatedWorkspace,
      integrations: integrations || []
    };
    const sessionCurrentData = {
      ...sessionCurrentUserData,
      metadata: { ...sessionCurrentUserData?.metadata, selected_workspace: _updatedWorkspace }
    };
    dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, _updatedWorkspace));
    dispatch(sessionCurrentUser({ loading: false, error: false, data: sessionCurrentData }));
  };

  const onCancelPressed = () => {
    setShowWarningModalSheet(false);
    setIntegrationDeletion(false);
    setOrgUnitError(false);
    const workSpace = get(workspaceGetState, "data", {});
    const _workspace = {
      ...workspace,
      integration_ids: workSpace?.integrations.map((item: { id: string; name: string }) => ({
        key: item?.id?.toString(),
        label: item?.name
      }))
    };
    setWorkspace(_workspace);
    setDeletedIntegrations([]);
  };

  return (
    <AntModal
      className="workspace-modal-sheet"
      title={showWarningModalSheet ? WORKSPACE_NAME_MAPPING[INTEGRATION_WARNING] : WORKSPACE_NAME_MAPPING[WORKSPACES]}
      visible={props.display}
      onCancel={showWarningModalSheet ? onCancelPressed : props.onCancel}
      okButtonProps={okButtonProps}
      onOk={orgUnitError ? onCancelPressed : showWarningModalSheet ? onOkPressed : onUpdate}
      okText={orgUnitError ? "OK" : showWarningModalSheet ? "Delete" : "Save"}
      cancelText={showWarningModalSheet ? "No" : "Cancel"}>
      {!showWarningModalSheet ? (
        <AntForm layout={"vertical"}>
          {workspaceIdRef.current !== undefined && (
            <AntFormItem label={"project id"}>
              <AntText copyable>{workspaceIdRef.current}</AntText>
            </AntFormItem>
          )}
          <WorkspaceNameComponent
            name={workspace.name}
            onChange={onNameChangeHandler}
            label={"Name"}
            nameExist={nameExist}
            setNameExist={setNameExist}
            isNameValid={isNameValid}
            setIsNameValid={setIsNameValid}
            nameFieldRef={nameFieldRef}
          />
          <AntFormItem label={"Description"} colon={false} required={false} hasFeedback={false}>
            <AntInput name={"description"} value={workspace.description} onChange={onChangeHandler("description")} />
          </AntFormItem>
          <AntFormItem label={"Owner"} colon={false} required={true} hasFeedback={true}>
            <SelectRestapi
              placeholder={"Project Owner"}
              value={userSelect}
              mode={"single"}
              labelInValue={true}
              uri={"users"}
              searchField={"email"}
              onChange={onChangeHandler("owner")}
            />
          </AntFormItem>
          <AntFormItem
            label={"Key"}
            colon={false}
            required={true}
            hasFeedback={true}
            validateStatus={keyFieldRef.current && workspace.key === "" ? ERROR : ""}>
            <AntInput
              name={"key"}
              disabled={workspaceIdRef.current !== undefined}
              value={workspace.key}
              onChange={onChangeHandler("key")}
              placeholder={"KEY"}
              onBlur={(e: any) => {
                keyFieldRef.current = true;
              }}
            />
          </AntFormItem>
          <AntFormItem required={true} label={"Integration Mappings"} colon={false}>
            <SelectRestapi
              searchField="name"
              uuid="integration_list_workspace"
              uri="integrations"
              fetchData={(filters: any, complete: any) => dispatch(integrationsList(filters, complete))}
              method="list"
              isMulti={false}
              closeMenuOnSelect
              value={workspace.integration_ids || []}
              creatable={false}
              mode={"multiple"}
              labelInValue
              onChange={onChangeHandler("integration_ids")}
            />
          </AntFormItem>
        </AntForm>
      ) : (
        confirmPopupContent
      )}
    </AntModal>
  );
};

export default ErrorWrapper(WorkspaceCreateEdit);
