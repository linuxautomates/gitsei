import React, { FunctionComponent, useCallback, useEffect, useRef, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import queryString from "query-string";
import { WorkspaceCreateEdit } from "./containers";
import { ServerPaginatedTable } from "../shared-resources/containers";
import { useDispatch, useSelector } from "react-redux";
import ErrorWrapper from "../hoc/errorWrapper";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { AntText, TableRowActions } from "../shared-resources/components";
import { tableColumns } from "./table-config";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { WebRoutes } from "../routes/WebRoutes";
import { WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";
import DeleteWarningModal from "./containers/workspaceDeleteWarningModal/WarningModal";
import { setSelectedWorkspace, workspaceApiClear, workspaceRead } from "reduxConfigs/actions/workspaceActions";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  getGenericWorkSpaceUUIDSelector,
  getSelectedWorkspace
} from "reduxConfigs/selectors/workspace/workspace.selector";
import { WorkspaceModel } from "reduxConfigs/reducers/workspace/workspaceTypes";
import { SELECTED_WORKSPACE_ID } from "reduxConfigs/selectors/workspace/constant";
import { get } from "lodash";

const WorkspaceListPage: FunctionComponent<RouteComponentProps> = (props: RouteComponentProps) => {
  const headerRef = useRef<boolean>(false);
  const [selectedWorkspaceUpdateLoading, setSelectedWorkspaceUpdateLoading] = useState<boolean>(false);
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [selectedProduct, setSelectedProduct] = useState<string | undefined>("");
  const [reload, setReload] = useState(1);
  const pageState = useSelector(state => pageSettings(state));
  const isTrialUser = useSelector(isSelfOnboardingUser);
  const [selectedIds, setSelectedIds] = useState<any>([]);
  const [rowSelection, setRowSelection] = useState<any>({});
  const [deleteClicked, setDeleteClicked] = useState<boolean>(false);

  const selectedWorkspace: WorkspaceModel = useSelector(getSelectedWorkspace);
  const { workspace_id } = queryString.parse(props.location.search);

  const workspaceGetState = useParamSelector(getGenericWorkSpaceUUIDSelector, {
    method: "get",
    uuid: selectedWorkspace.id
  });

  const dispatch = useDispatch();

  useEffect(() => {
    if (!headerRef.current) {
      dispatch(
        setPageSettings(props.location.pathname, {
          title: WORKSPACES,
          action_buttons: {
            add_project: {
              type: "primary",
              label: `Add ${WORKSPACE_NAME_MAPPING[WORKSPACES]}`,
              hasClicked: false
            }
          }
        })
      );
      headerRef.current = true;
    }
  }, [headerRef.current]);

  useEffect(() => {
    if (headerRef.current && pageState && Object.keys(pageState).length > 0) {
      const page = pageState[props.location.pathname];
      if (page && page.hasOwnProperty("action_buttons")) {
        if (page.action_buttons.add_project && page.action_buttons.add_project.hasClicked) {
          dispatch(setPageButtonAction(props.location.pathname, "add_project", { hasClicked: false }));
          setModalVisible(true);
          setSelectedProduct(undefined);
        }
      }
    }
  }, [headerRef.current, pageState]);

  useEffect(() => {
    if (workspace_id) {
      onEditHandler(workspace_id);
    }
    return () => {
      dispatch(restapiClear("workspace", "list", "0"));
      dispatch(clearPageSettings(props.location.pathname));
    };
  }, []);

  useEffect(() => {
    if (selectedWorkspaceUpdateLoading) {
      const loading = get(workspaceGetState, "loading", true);
      const error = get(workspaceGetState, "error", true);
      if (!loading) {
        if (!error) {
          const data = get(workspaceGetState, ["data"], {});
          if (Object.keys(data).length) {
            dispatch(setSelectedWorkspace(SELECTED_WORKSPACE_ID, data));
          }
        }
        setSelectedWorkspaceUpdateLoading(false);
      }
    }
  }, [selectedWorkspaceUpdateLoading, workspaceGetState]);

  useEffect(() => {
    if (isTrialUser) {
      // @ts-ignore
      props.history.push({ pathname: WebRoutes.dashboard.details(props.match.params, "") });
    }
  }, [isTrialUser]);

  const onSelectChange = (rowKeys: any) => {
    setSelectedIds(rowKeys);
  };

  useEffect(() => {
    setRowSelection({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange
    });
  }, [selectedIds]);

  const onEditHandler = (productId: any) => {
    setSelectedProduct(productId);
    setModalVisible(true);
  };

  const onRemoveHandler = (productId: any) => {
    setDeleteClicked(true);
  };

  const buildActionOptions = (record: any) => {
    const actions = [
      {
        type: "delete",
        id: record.id,
        onClickEvent: onRemoveHandler
      }
    ];
    return <TableRowActions actions={actions} />;
  };

  const buildName = (record: any) => {
    return (
      <AntText style={{ paddingLeft: "10px" }} onClick={() => onEditHandler(record.id)}>
        <a className={"ellipsis"}>{record.name}</a>
      </AntText>
    );
  };

  const getMappedColumns = () => {
    return tableColumns.map(column => {
      if (column.key === "id" || column.key === "name") {
        return {
          ...column,
          render: (item: any, record: any, index: any) =>
            column.key === "id" ? buildActionOptions(record) : buildName(record)
        };
      }
      return column;
    });
  };

  const onBulkDelete = () => {
    setDeleteClicked(true);
  };

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const handleModalClose = () => {
    setModalVisible(false);
    setSelectedProduct(undefined);
    if (workspace_id) {
      props.history.push({
        search: ""
      });
    }
  };
  const handleWorkspaceUpdate = useCallback(() => {
    if (selectedProduct && selectedProduct === selectedWorkspace.id) {
      dispatch(workspaceApiClear(selectedWorkspace.id, "get"));
      dispatch(workspaceRead(selectedWorkspace.id, "get"));
      setSelectedWorkspaceUpdateLoading(true);
    }
    setModalVisible(false);
  }, [selectedProduct, selectedWorkspace]);

  return (
    <>
      {deleteClicked && <DeleteWarningModal showWarning={deleteClicked} handleOk={() => setDeleteClicked(false)} />}
      {modalVisible && (
        <WorkspaceCreateEdit
          onUpdate={handleWorkspaceUpdate}
          display={modalVisible}
          onCancel={handleModalClose}
          product_id={selectedProduct}
        />
      )}
      {!modalVisible && (
        <>
          <ServerPaginatedTable
            generalSearchField="name"
            pageName={"Project"}
            uri="workspace"
            moreFilters={{}}
            partialFilters={{}}
            columns={getMappedColumns()}
            hasFilters={false}
            reload={reload}
            generalSearchPartialKey="starts"
            bulkDeleting={false}
            clearSelectedIds={clearSelectedIds}
            rowSelection={rowSelection}
            onBulkDelete={onBulkDelete}
            hasDelete={true}
            bulkDeleteRestriction={true}
          />
        </>
      )}
    </>
  );
};
export default ErrorWrapper(WorkspaceListPage);
