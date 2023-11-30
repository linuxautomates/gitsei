import React, { FunctionComponent, useEffect, useRef, useState } from "react";
import { RouteComponentProps } from "react-router-dom";
import Loader from "../components/Loader/Loader";
import { ProductMappings, WorkspaceCreateEdit } from "./containers";

import { ServerPaginatedTable } from "../shared-resources/containers";
import { useDispatch, useSelector } from "react-redux";
import ErrorWrapper from "../hoc/errorWrapper";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { AntText, TableRowActions } from "../shared-resources/components";
import { tableColumns } from "./table-config";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { productsDelete, productsList, productsBulkDelete } from "reduxConfigs/actions/restapi";
import { ProductsSelectorState } from "reduxConfigs/selectors/restapiSelector";
import { get } from "lodash";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { notification } from "antd";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { WebRoutes } from "../routes/WebRoutes";
import { WORKSPACES, WORKSPACE_NAME_MAPPING } from "dashboard/constants/applications/names";

const ProductsListPage: FunctionComponent<RouteComponentProps> = (props: RouteComponentProps) => {
  const deleteProductIdRef = useRef<any>();
  const headerRef = useRef<boolean>(false);

  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [modalVisible, setModalVisible] = useState<boolean>(false);
  const [selectedProduct, setSelectedProduct] = useState<any>();
  const [selectedIds, setSelectedIds] = useState<any>([]);
  const [rowSelection, setRowSelection] = useState<any>({});
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);
  const [reload, setReload] = useState(1);

  const productsApiState = useSelector(state => ProductsSelectorState(state));
  const pageState = useSelector(state => pageSettings(state));
  const isTrialUser = useSelector(isSelfOnboardingUser);

  const dispatch = useDispatch();

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = productsApiState.bulkDelete["0"];
      if (!loading) {
        if (!error) {
          const data = get(productsApiState, ["bulkDelete", "0", "data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            setReload(reload => reload + 1);
          } else {
            setSelectedIds([]);
            setReload(reload => reload + 1);
          }
        }
        setBulkDeleting(false);
      }
    }
  }, [productsApiState]); // eslint-disable-next-line react-hooks/exhaustive-deps

  useEffect(() => {
    if (deleteLoading) {
      const loading = productsApiState.delete?.[deleteProductIdRef.current]?.loading;
      const error = productsApiState.delete?.[deleteProductIdRef.current]?.error;
      if (loading !== undefined && !loading) {
        if (error !== undefined && !error) {
          const data = get(productsApiState, ["delete", deleteProductIdRef.current, "data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            onDeleteSuccess();
          }
        }
        setDeleteLoading(false);
        deleteProductIdRef.current = undefined;
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [deleteLoading, productsApiState.delete?.[deleteProductIdRef.current]]);

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
    // eslint-disable-next-line react-hooks/exhaustive-deps
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
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [headerRef.current, pageState]);

  useEffect(() => {
    return () => {
      dispatch(restapiClear("products", "list", "0"));
      dispatch(restapiClear("products", "delete", "-1"));
      dispatch(restapiClear("products", "get", "-1"));
      dispatch(restapiClear("mappings", "list", "-1"));
      dispatch(restapiClear("products", "update", "-1"));
      dispatch(restapiClear("products", "create", "0"));
      dispatch(restapiClear("products", "bulkDelete", "-1"));
      dispatch(clearPageSettings(props.location.pathname));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (isTrialUser) {
      // @ts-ignore
      props.history.push({ pathname: WebRoutes.dashboard.details(props.match.params, "") });
    }
  }, [isTrialUser]);

  const onDeleteSuccess = () => {
    const deleteId = deleteProductIdRef.current;
    setSelectedIds((ids: string[]) => ids.filter(id => id !== deleteId));
    dispatch(productsList({}));
  };

  const onEditHandler = (productId: any) => {
    setSelectedProduct(productId);
    setModalVisible(true);
  };

  const onRemoveHandler = (productId: any) => {
    deleteProductIdRef.current = productId;
    setDeleteLoading(true);
    dispatch(productsDelete(productId));
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
  const onSelectChange = (rowKeys: any) => {
    setSelectedIds(rowKeys);
  };

  useEffect(() => {
    setRowSelection({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange
    });
  }, [selectedIds]);

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const onBulkDelete = () => {
    dispatch(productsBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  if (deleteLoading) {
    return <Loader />;
  }

  return (
    <>
      {modalVisible && (
        <WorkspaceCreateEdit
          onUpdate={() => setModalVisible(false)}
          display={modalVisible}
          onCancel={() => setModalVisible(false)}
          product_id={selectedProduct}
        />
      )}
      {!modalVisible && (
        <ServerPaginatedTable
          pageName={"products"}
          restCall="getProducts"
          uri="products"
          moreFilters={{}}
          partialFilters={{}}
          columns={getMappedColumns()}
          hasFilters={false}
          clearSelectedIds={clearSelectedIds}
          rowSelection={rowSelection}
          onBulkDelete={onBulkDelete}
          reload={reload}
          hasDelete={true}
          bulkDeleting={bulkDeleting}
        />
      )}
    </>
  );
};
export default ErrorWrapper(ProductsListPage);
