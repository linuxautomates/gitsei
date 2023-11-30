import React, { useEffect, useState } from "react";

import { TableRowActions } from "shared-resources/components";
import { ServerPaginatedTable } from "shared-resources/containers";
import { tableColumns } from "./table-config";
import { RouteComponentProps } from "react-router-dom";
import { useSelector, useDispatch } from "react-redux";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { restAPILoadingState } from "utils/stateUtil";
import { userDeleteState } from "reduxConfigs/selectors/usersSelector";
import { usersDelete, userTrellisPermissionUpdate } from "reduxConfigs/actions/restapi";
import Loader from "components/Loader/Loader";
import ErrorWrapper from "hoc/errorWrapper";
import { clearPageSettings, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { BASE_UI_URL } from "helper/envPath.helper";
import { Switch } from "antd";
import { getSettingsPage } from "constants/routePaths";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

export const UsersListPage: React.FC<RouteComponentProps> = (props: RouteComponentProps) => {
  const [delete_loading, setDeleteLoading] = useState(false);
  const [delete_user_id, setDeleteUserId] = useState<string | undefined>(undefined);

  const { pathname } = props.location;

  const dispatch = useDispatch();
  const userDltState = useSelector(userDeleteState);

  const [hasCreateAccess, hasEditAccess, hasDeleteAccess] = useConfigScreenPermissions();
  useEffect(() => {
    return () => {
      dispatch(restapiClear("users", "list", 0));
      dispatch(restapiClear("users", "delete", -1));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
  useEffect(() => {
    dispatch(
      setPageSettings(pathname, {
        action_buttons: {
          primary: {
            actionId: "users",
            id: "users",
            type: "primary",
            label: "Add User",
            hasClicked: false,
            disabled: !hasCreateAccess,
            buttonHandler: () => {
              window.location.replace(BASE_UI_URL.concat(`${getSettingsPage()}/add-user-page`));
            }
          }
        }
      })
    );
    return () => {
      dispatch(clearPageSettings(pathname));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (delete_loading) {
      const { loading } = restAPILoadingState(userDltState, delete_user_id);
      if (!loading) {
        setDeleteLoading(false);
        setDeleteUserId(undefined);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userDltState]);

  const onRemoveHandler = (userId: string) => {
    setDeleteLoading(true);
    setDeleteUserId(userId);
    dispatch(usersDelete(userId));
  };

  const buildActionOptions = (actionProps: { id: string }) => {
    const actions = [
      {
        type: "delete",
        id: actionProps.id,
        onClickEvent: onRemoveHandler,
        disabled: window.isStandaloneApp ? false : !hasDeleteAccess
      }
    ];
    return <TableRowActions actions={actions} />;
  };

  /**
   * User update on trellis toggle button
   * @param val toggle value
   * @param record selected user row record
   *
   * @return user update for trellis access
   **/
  const handleTrellisAccess = (val: boolean, record: any) => {
    record.scopes = val ? { dev_productivity_write: [] } : {};
    dispatch(userTrellisPermissionUpdate(record, true));
  };

  const mappedColumns = tableColumns.map(column => {
    if (column.key === "id") {
      return {
        ...column,
        render: (item: any, record: any, index: number) => buildActionOptions(record)
      };
    }
    if (column.key === "scopes") {
      return {
        ...column,
        render: (item: any, record: any, index: number) => {
          return (
            <Switch checked={record?.scopes?.dev_productivity_write} onChange={e => handleTrellisAccess(e, record)} />
          );
        }
      };
    }
    return column;
  });

  if (delete_loading) {
    return <Loader />;
  }

  return (
    <>
      <ServerPaginatedTable
        pageName={"users"}
        generalSearchField={"email"}
        restCall="getUsers"
        uri={"users"}
        columns={mappedColumns}
        hasFilters={false}
      />
    </>
  );
};

export default ErrorWrapper(UsersListPage);
