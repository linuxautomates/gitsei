import { get } from "lodash";
import React, { useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { OrgUserSchemaGet, OrgUserVersionCreate, OrgUserVersionList } from "reduxConfigs/actions/restapi/orgUserAction";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { orgUsersGenericSelector, ORG_USER_SCHEMA_ID } from "reduxConfigs/selectors/orgUsersSelector";
import { AntButton, AntModal, AntTable } from "../../../../../shared-resources/components";
import { tableColumns } from "./tableConfig";
import VersionTableRowActionsComponent from "./VersionTableRowActions";
import "./versionModal.styles.scss";
import { WebRoutes } from "routes/WebRoutes";

interface VersionModalProps {
  handleClose: (visible: boolean) => void;
}

export const VersionModal: React.FC<VersionModalProps> = ({ handleClose }) => {
  const history = useHistory();
  const dispatch = useDispatch();
  const [versionData, setVersionData] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const versionListState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_version",
    method: "get",
    id: "org_versions_id"
  });

  const versionCreateState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_version",
    method: "create",
    id: "new_version_set"
  });

  useEffect(() => {
    const loading = get(versionListState, "loading", true);
    const error = get(versionListState, "error", true);
    if (!loading && !error) {
      const data = get(versionListState, ["data", "records"], []);
      const sortedData = data.sort((a: any, b: any) => b.version - a.version);
      setVersionData(sortedData);
      setLoading(false);
    }
  }, [versionListState]);

  useEffect(() => {
    const loading = get(versionCreateState, "loading", true);
    const error = get(versionCreateState, "error", true);
    if (!loading && !error) {
      dispatch(OrgUserVersionList({}, "org_versions_id"));
      setLoading(false);
    }
  }, [versionCreateState]);

  const closeHandler = () => {
    handleClose(false);
  };

  const closebutton = useMemo(() => {
    return (
      <AntButton type={"secondary"} onClick={closeHandler}>
        Close
      </AntButton>
    );
  }, []);

  const onViewButtonHandler = useCallback((item: any) => {
    const version = get(item, "version", "1");
    dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema", null, { version }));
    history.push(WebRoutes.organization_users_page.root(version));
    closeHandler();
  }, []);

  const onSetAsActiveButtonHandler = useCallback((item: any) => {
    const data: any = {
      active_version: get(item, "version", "1"),
      user_id: "",
      all_users: true
    };
    dispatch(OrgUserVersionCreate(data, "new_version_set"));
    setLoading(true);
  }, []);

  const buildActionOptions = (actionProps: any) => {
    const actions = [
      {
        type: "view",
        id: actionProps.id,
        title: "View",
        buttonType: "secondary",
        onClickEvent: () => onViewButtonHandler(actionProps)
      },
      {
        type: "active",
        title: actionProps.active ? "Active" : "Set as Active",
        buttonType: actionProps.active ? "primary" : "secondary",
        id: actionProps.id,
        onClickEvent: actionProps.active
          ? () => onViewButtonHandler(actionProps)
          : () => onSetAsActiveButtonHandler(actionProps)
      }
    ].filter((item: any) => !(actionProps.active && item.type === "view"));
    return <VersionTableRowActionsComponent actions={actions} />;
  };

  const mappedColumns = useMemo(() => {
    return tableColumns.map(column => {
      if (column.key === "id") {
        return {
          ...column,
          render: (item: any, record: any, index: number) => buildActionOptions(record)
        };
      }
      return column;
    });
  }, []);

  return (
    <AntModal
      wrapClassName="org-user-version-modal"
      title={"User Versions"}
      scroll={{ y: 240 }}
      visible
      onCancel={closeHandler}
      width="40%"
      footer={closebutton}>
      <AntTable dataSource={versionData} columns={mappedColumns} reload={false} pagination={false} loading={loading} />
    </AntModal>
  );
};

export default VersionModal;
