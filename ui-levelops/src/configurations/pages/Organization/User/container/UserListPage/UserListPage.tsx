import React, { useCallback, useEffect, useState, useMemo, useRef } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { cloneDeep, get, isArray, isEqual, uniq, uniqBy } from "lodash";
import queryString from "query-string";
import { Button, Dropdown, Icon, Menu, Modal, notification, Popconfirm, Spin, Tooltip } from "antd";
import { usersCsvTransformer } from "../../../Helpers/users-csv.transformer";
import ConfigureUsers from "../../component/ConfigureUsers/ConfigureUsers";
import VersionModal from "../../component/versionModal";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import {
  ORG_USER_DELETE_ID,
  ORG_USER_LIST_ID,
  ORG_USER_SCHEMA_ID,
  ORG_USERS,
  orgUserDeleteState,
  orgUsersGenericSelector
} from "reduxConfigs/selectors/orgUsersSelector";
import {
  OrgUserCreate,
  orgUserDelete,
  OrgUserFiltersList,
  OrgUserSchemaGet,
  OrgUserUpdate,
  OrgUserVersionList
} from "reduxConfigs/actions/restapi/orgUserAction";
import { restapiClear } from "reduxConfigs/actions/restapi";
import {
  AntText,
  AntAlert,
  SvgIcon,
  AntButton,
  AntTooltip,
  TooltipWithTruncatedText
} from "shared-resources/components";
import { toTitleCase } from "utils/stringUtils";
import { baseColumnConfig } from "utils/base-table-config";
import { restAPILoadingState } from "utils/stateUtil";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { csvDownloadSampleUser, csvDownloadUser } from "reduxConfigs/actions/csvDownload.actions";
import { tableColumns } from "./tableConfig";
import "./UserList.style.scss";

import { ServerPaginatedTable } from "shared-resources/containers";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { WebRoutes } from "routes/WebRoutes";
import MergeUsers from "../MergeUsers/MergeUsers";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { unSavedChangesSelector } from "reduxConfigs/selectors/unSavedChangesSelector";
import LeavePageBlocker from "./LeavePageBlocker";
import { setUnSavedChanges } from "reduxConfigs/actions/unSavedChanges";
import { useBreadCrumbsForOrgUsersPage } from "configurations/pages/Organization/Helpers/useBreadCrumbsForOrgUsersPage";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getBaseUrl, ORGANIZATION_USERS_ROUTES, getSettingsPage } from "constants/routePaths";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";
import { getIsStandaloneApp } from "helper/helper";
import cx from "classnames";

const UserListPage: React.FC = () => {
  const history = useHistory();
  const location = useLocation();
  const dispatch = useDispatch();

  const version = queryString.parse(location.search).version as string;

  const [missingEmails, setMissingEmails] = useState<number>(0);
  const [deleteLoading, setDeleteLoading] = useState(false);
  const [versionModal, setVersionModal] = useState<boolean>(false);
  const [isMerged, setIsMerged] = useState<boolean>(false);
  const [isUpdateUser, setIsUpdateUser] = useState<boolean>(false);

  const [configureUsersModel, setConfigureUsersModel] = useState<string | undefined>(undefined);

  const [reload, setReload] = useState<number>(0);
  const [selectedIds, setSelectedIds] = useState<number[]>([]);
  const [selectedRows, setSelectedRows] = useState<any[]>([]);
  const [clearFilters, setClearFilters] = useState<number>(0);
  const [updatedUserList, setUpdatedUserList] = useState<Array<any>>([]);
  const [saveWarningModalState, setSaveWarningModalState] = useState<boolean>(false);
  const [searchValue, setSearchValue] = useState<string>("");

  const [currentVersion, setCurrentVersion] = useState<string>(version);
  const [filters, setFilters] = useState<any>({ page: 0, page_size: 50 });
  const [mappedColumns, setMappedColumns] = useState<any[]>(tableColumns);
  const [versionLoading, setVersionLoading] = useState<boolean>(true);
  const [userUpdateLoading, setUserUpdateLoading] = useState<boolean>(false);
  const queryParams = useMemo(() => ({ version: currentVersion }), [currentVersion]);
  const entOrgUnits = useHasEntitlements(Entitlement.SETTING_ORG_UNITS);
  const entOrgUnitsCountExceed = useHasEntitlements(Entitlement.SETTING_ORG_UNITS_COUNT_5, EntitlementCheckType.AND);
  const [loadActiveVersion, setloadActiveVersion] = useState<boolean>(false);

  const changesSelector = useSelector(unSavedChangesSelector);
  const breadCrumb = useBreadCrumbsForOrgUsersPage();
  const isStandaloneApp = getIsStandaloneApp();

  const versionListState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_version",
    method: "get",
    id: "org_versions_id"
  });

  const userSchemaState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_schema",
    method: "get",
    id: ORG_USER_SCHEMA_ID
  });

  const userListState = useParamSelector(orgUsersGenericSelector, {
    uri: ORG_USERS,
    method: "list",
    id: ORG_USER_LIST_ID
  });

  const userFilterListState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_filter",
    method: "list",
    id: `version=${currentVersion}`
  });

  const userDeleteState = useSelector(orgUserDeleteState);

  const importFileState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_import",
    method: "create",
    id: "import_file"
  });

  const userUpdateState = useParamSelector(orgUsersGenericSelector, {
    uri: ORG_USERS,
    method: "update",
    id: "org_user_update"
  });

  const userCreateState = useParamSelector(orgUsersGenericSelector, {
    uri: ORG_USERS,
    method: "create",
    id: "org_user_create"
  });

  useEffect(() => {
    dispatch(OrgUserVersionList({}, "org_versions_id"));
    dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema", null));
    updateButtonState();
    return () => {
      dispatch(restapiClear("org_users_schema", "get", -1));
      dispatch(restapiClear("org_users_version", "get", "org_versions_id"));
      dispatch(restapiClear("org_users_version", "create", -1));
      dispatch(restapiClear("org_users_import", "create", -1));
      dispatch(restapiClear("users", "list", "members-list"));
      dispatch(restapiClear("users", "delete", -1));
      dispatch(restapiClear("users", "bulkDelete", -1));
    };
  }, []);

  const activeVersionRef = useRef("");

  const activeVersion = useMemo(() => {
    let _activeVersion = activeVersionRef.current;
    const loading = get(versionListState, "loading", true);
    const error = get(versionListState, "error", true);
    if (!loading && !error) {
      const data = get(versionListState, ["data", "records"], []);
      if (loading !== versionLoading) {
        setVersionLoading(loading);
        setUserUpdateLoading(loading);
      }
      if (data.length) {
        _activeVersion = data.find((item: any) => item.active)?.version;
        if (!!_activeVersion && activeVersionRef.current !== _activeVersion) {
          activeVersionRef.current = _activeVersion;
          if (!version) {
            history.push(WebRoutes.organization_users_page.root(_activeVersion));
          }
        } else {
          _activeVersion = activeVersionRef.current;
        }
      }
    }
    return _activeVersion;
  }, [versionListState]);

  useEffect(() => {
    if (loadActiveVersion) {
      if (activeVersion?.toString() !== currentVersion) {
        setUserUpdateLoading(false);
        setCurrentVersion(activeVersion);
        history.push(WebRoutes.organization_users_page.root(activeVersion));
        setClearFilters(reload => reload + 1);
        setloadActiveVersion(false);
      }
    } else if (version && version !== currentVersion) {
      setUserUpdateLoading(false);
      setCurrentVersion(version);
      history.push(WebRoutes.organization_users_page.root(version));
      setClearFilters(reload => reload + 1);
    } else if (!version && activeVersion) {
      setUserUpdateLoading(false);
      setCurrentVersion(activeVersion);
      history.push(WebRoutes.organization_users_page.root(activeVersion));
      setClearFilters(reload => reload + 1);
    }
  }, [activeVersion, version, loadActiveVersion]);

  useEffect(() => {
    const loading = get(importFileState, "loading", true);
    const error = get(importFileState, "error", true);
    if (!loading && !error) {
      const version = get(importFileState, ["data", "version"], undefined);
      if (version) {
        dispatch(OrgUserVersionList({}, "org_versions_id"));
      }
    }
  }, [importFileState]);

  useEffect(() => {
    const loading = get(userSchemaState, "loading", true);
    const error = get(userSchemaState, "error", true);
    if (!loading && !error) {
      const data = get(userSchemaState, ["data", "fields"], []);
      const filters = {
        fields: data
          .filter((item: any) => !["integration", "start_date"].includes(item.key))
          .map((item: any) => (["full_name", "email"].includes(item.key) ? item.key : `custom_field_${item.key}`))
      };
      dispatch(OrgUserFiltersList(filters, `version=${currentVersion}`));
    }
  }, [userSchemaState, currentVersion]);

  useEffect(() => {
    const loading = get(userFilterListState, "loading", true);
    const error = get(userFilterListState, "error", true);
    if (!loading && !error) {
      const data = get(userFilterListState, ["data", "records"], []);
      const fieldTypeData = get(userSchemaState, ["data", "fields"], [])
        .filter((item: any) => !["integration", "start_date"].includes(item.key))
        .reduce((acc: { [key: string]: string }, item: { [key: string]: string }) => {
          return {
            ...acc,
            [item.key]:
              (item.type === "string"
                ? "search"
                : item.type === "boolean"
                ? "binary"
                : item.type === "date"
                ? "dateRange"
                : "") || "multiSelect"
          };
        }, {});
      const hashOptions = data.reduce((acc: any, next: any) => {
        const key = Object.keys(next)[0];
        let values = get(next, [key, "records"], []);
        if (isArray(values) && values.length) {
          values = values.map((item: any) => {
            return {
              label: (item["key"] || "").toUpperCase(),
              value: item["key"]
            };
          });
        }
        return {
          ...acc,
          [key]: values
        };
      }, {});

      const customFieldColumns = Object.keys(hashOptions)
        .filter((key: string) => (key || "").startsWith("custom_field_"))
        .map((key: string) => {
          const updatedKey = key.replace("custom_field_", "");
          const title = toTitleCase(updatedKey);
          const options = hashOptions[key] || [];
          return {
            ...baseColumnConfig(title, key),
            width: "auto",
            span: 8,
            filterType: fieldTypeData[updatedKey] || "multiSelect",
            filterLabel: title,
            filterField: key,
            options,
            render: (item: any, record: any, index: number) => {
              const value = get(record, ["additional_fields", updatedKey], undefined);
              return <AntText>{`${value || ""}`}</AntText>;
            }
          };
        });

      const defaultColumns = tableColumns.map((column: any) => {
        if (column.hasOwnProperty("valueName")) {
          return {
            ...column,
            options: hashOptions[column.valueName] || [{ label: " ", value: " " }]
          };
        }
        return column;
      });

      const finalColumns = [...defaultColumns, ...customFieldColumns];

      if (!isEqual(mappedColumns, finalColumns)) {
        setMappedColumns(finalColumns);
      }
    }
  }, [userFilterListState]);

  useEffect(() => {
    const loading = get(userListState, "loading", true);
    const error = get(userListState, "error", true);
    if (!loading && !error) {
      const missingCount = get(userListState, ["data", "non_users_count"], 0);
      setMissingEmails(missingCount);
      /* Updating the userListState with the updatedUserList. */
      if (updatedUserList?.length > 0) {
        let newData = userListState?.data?.records?.slice(0);
        const mergedIds = updatedUserList.flatMap((usr: any) => usr.mergedIds); // for hiding merged rows
        const updatedUsers = updatedUserList.filter((usr: any) => usr?.email);
        newData = newData.reduce((acc: any, data: any) => {
          const dynamicId = getId(data);
          if (mergedIds.indexOf(dynamicId) === -1) {
            const found = updatedUserList.find((search: any) => {
              const searchId = getId(search);
              return search?.email == data?.email || dynamicId === searchId;
            });
            if (!found) {
              acc.push(data);
            }
          }
          return acc;
        }, []);
        if (updatedUsers?.[0]?.email !== newData?.[0]?.email) {
          // check already updated to store or not
          newData = [...updatedUsers, ...newData];
        }
        if (searchValue) {
          const newSearchValue = searchValue?.toUpperCase();
          newData = newData.filter((data: any) => data?.full_name?.toUpperCase()?.includes(newSearchValue));
        }
        if (!isEqual(newData, userListState?.data?.records)) {
          dispatch(genericRestAPISet(newData, "org_users", "list", ORG_USER_LIST_ID));
        }
      }
    }
  }, [userListState, updatedUserList]);

  useEffect(() => {
    if (deleteLoading) {
      const { loading, error } = restAPILoadingState(userDeleteState, ORG_USER_DELETE_ID);
      if (!loading) {
        if (!error) {
          const data = get(userDeleteState, ["0", "data", "records"], []);
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
            dispatch(OrgUserVersionList({}, "org_versions_id"));
          }
        }
        setDeleteLoading(false);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [userDeleteState]);

  useEffect(() => {
    dispatch(setUnSavedChanges({ dirty: updatedUserList?.length !== 0, show_modal: false }));
    updateButtonState();
  }, [updatedUserList]);

  useEffect(() => {
    if (userUpdateState?.data === "ok" && !userUpdateState?.loading && !userUpdateState?.error) {
      notification.success({
        message: "Org users updated successfully."
      });
      setloadActiveVersion(true);
      setUpdatedUserList([]);
      dispatch(restapiClear(ORG_USERS, "update", "org_user_update"));
      dispatch(OrgUserVersionList({}, "org_versions_id"));
      dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema", null));
      setClearFilters(reload => reload + 1);
    }
  }, [userUpdateState]);

  useEffect(() => {
    if (userCreateState?.data?.success.length && !userCreateState?.loading && !userCreateState?.error) {
      notification.success({
        message: "Org users created successfully."
      });
      setloadActiveVersion(true);
      setUpdatedUserList([]);
      dispatch(restapiClear(ORG_USERS, "create", "org_user_create"));
      dispatch(OrgUserVersionList({}, "org_versions_id"));
      dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema", null));
      setClearFilters(reload => reload + 1);
    }
  }, [userCreateState]);

  const onAddIntegrations = useCallback(() => {
    history.push(`${getSettingsPage()}/integrations`);
  }, []);

  const onRemoveHandler = useCallback((userId: string) => {
    return (e: any) => {
      setDeleteLoading(true);
      dispatch(orgUserDelete(ORG_USER_DELETE_ID, [userId]));
    };
  }, []);

  const onBulkDelete = useCallback(() => {
    dispatch(orgUserDelete(ORG_USER_DELETE_ID, selectedIds));
    setDeleteLoading(true);
  }, [selectedIds]);

  const handleCsvDownload = useCallback(
    (type: string) => {
      return () => {
        if (type === "sample") {
          dispatch(csvDownloadSampleUser());
        } else {
          const _filters = {
            ...filters,
            version: currentVersion
          };
          dispatch(
            csvDownloadUser("org_users", "list", {
              transformer: usersCsvTransformer,
              filters: _filters,
              columns: [],
              jsxHeaders: [],
              derive: true,
              shouldDerive: []
            })
          );
        }
      };
    },
    [filters, currentVersion]
  );

  const handleFiltersChange = useCallback(
    (updatedFilters: any) => {
      if (!isEqual(filters, updatedFilters)) {
        setFilters(updatedFilters);
      }
    },
    [filters]
  );

  const onSelectionChange = useCallback(
    (rowKeys: number[], records: any[]) => {
      const newUsers: any = [];
      let allUsers = [...records, ...selectedRows];
      rowKeys.forEach(id => {
        // @ts-ignore
        const user = allUsers.find(usr => getId(usr) === id || (usr?.id === id && id === "0" && usr?.email));
        if (user) {
          newUsers.push(user);
        }
      });
      setSelectedIds(rowKeys);
      setSelectedRows(newUsers);
    },
    [selectedRows]
  );

  const clearSelection = useCallback(() => {
    onSelectionChange([], []);
  }, [selectedIds]);

  const handleShowUsersModal = useCallback((type: "attribute" | "import") => {
    return () => {
      setConfigureUsersModel(type);
    };
  }, []);

  const handleHideUsersModal = useCallback(() => {
    setConfigureUsersModel(undefined);
  }, []);

  const handleShowVersionsModal = useCallback(() => {
    setVersionModal(true);
  }, []);

  const handleHideVersionsModal = useCallback(() => {
    setVersionModal(false);
  }, []);

  const switchActiveVersionHandler = useCallback(() => {
    dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema", null));
    history.push(WebRoutes.organization_users_page.root(activeVersion));
  }, [activeVersion]);

  const getRowKey = useCallback(
    (record: any, index: number) =>
      !!record.email
        ? record.id
        : `${record?.email}_${record.integration_user_ids?.[0]?.integration_id}_${record?.integration_user_ids?.[0]?.user_id}`,
    []
  );
  const updateUser = (record: any) => {
    setSelectedIds([record?.id]);
    setSelectedRows([record]);
    setIsUpdateUser(true);
  };
  const toggleEditMergeView = () => {
    if (isUpdateUser) {
      setSelectedIds([]);
      setSelectedRows([]);
      setIsUpdateUser(false);
    }
    if (isMerged) {
      setIsMerged(false);
    }
    setSearchValue("");
  };
  const getId = (user: any) => {
    return user?.id !== "0"
      ? user.id
      : `${user?.email}_${user?.integration_user_ids?.[0]?.integration_id}_${user?.integration_user_ids?.[0]?.user_id}`;
  };
  const getMergedIds = (dynamicId: string) => {
    return selectedRows.reduce((acc, usr) => {
      const searchId = getId(usr);
      if (searchId !== dynamicId) {
        if (usr?.mergedIds) {
          acc = [...acc, searchId, ...usr?.mergedIds];
        } else {
          acc.push(searchId);
        }
      }
      return acc;
    }, []);
  };
  const onSaveUpdatedUsers = (user: any, discard: boolean = false) => {
    if (!discard) {
      let allRecords = cloneDeep([...updatedUserList]);
      const dynamicId = getId(user);
      const index = allRecords.findIndex((search: any) => {
        const searchId = getId(search);
        return searchId === dynamicId;
      });
      if (index !== -1) {
        const ids = getMergedIds(dynamicId);
        user.mergedIds = uniq([...user.mergedIds, ...ids]);
        allRecords[index] = user;
      } else {
        user.mergedIds = getMergedIds(dynamicId);
        allRecords.push(user);
      }
      allRecords = allRecords.filter(record => user.mergedIds.indexOf(getId(record)) === -1);
      setUpdatedUserList(allRecords);
    } else {
      setUpdatedUserList(user);
    }
    setSearchValue("");
    setSelectedIds([]);
    setSelectedRows([]);
  };

  const submitUpdatedUsers = (allUsers: Record<string, string>) => {
    // @ts-ignore
    const users = (allUsers || []).map((user: any) => {
      delete user.mergedIds;
      delete user.disabled;
      return user;
    });
    if (activeVersion) {
      dispatch(OrgUserUpdate("org_user_update", users));
    } else {
      dispatch(OrgUserCreate(users, "org_user_create"));
    }
    setUserUpdateLoading(true);
    setUpdatedUserList([]);
    setSaveWarningModalState(false);
  };

  const updateButtonState = () => {
    dispatch(setUnSavedChanges({ dirty: updatedUserList?.length !== 0, show_modal: false }));
    dispatch(
      setPageSettings(location.pathname, {
        title: "Contributors",
        action_buttons: {
          manage_cancel: {
            type: "default",
            label: "Cancel",
            hasClicked: false,
            buttonHandler: () => {
              setReload(reload => reload + 1);
              dispatch(restapiClear("org_users_schema", "get", -1));
              dispatch(restapiClear("org_users_version", "get", "org_versions_id"));
              dispatch(OrgUserVersionList({}, "org_versions_id"));
              setUpdatedUserList([]);
              dispatch(OrgUserSchemaGet(ORG_USER_SCHEMA_ID, "org_users_schema", null));
            }
          },
          save: {
            type: "primary",
            label: "Save",
            hasClicked: false,
            disabled: updatedUserList?.length === 0,
            buttonHandler: () => {
              setSaveWarningModalState(!saveWarningModalState);
            }
          }
        },
        withBackButton: true,
        bread_crumbs: breadCrumb,
        bread_crumbs_position: "before",
        headerClassName: "org-user-list"
      })
    );
  };

  const isHarnessReadonly = useHasConfigReadOnlyPermission();
  const oldReadOnly = getRBACPermission(PermeableMetrics.ORG_UNIT_USER_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isHarnessReadonly;

  const _columns = useMemo(() => {
    return mappedColumns.map(column => {
      if (column.key === "full_name") {
        return {
          ...column,
          render: (item: any, record: any, index: number) => {
            return (
              <Button type="link" onClick={isReadOnly ? () => {} : () => updateUser(record)}>
                <TooltipWithTruncatedText
                  allowedTextLength={15}
                  title={record?.full_name}
                  textClassName={"org-name-label"}
                />
              </Button>
            );
          }
        };
      }
      if (column.key === "email" && missingEmails !== 0) {
        return {
          ...column,
          title: (
            <div className="flex">
              Email <SvgIcon className={"pl-10"} icon="warning" style={{ width: "24px", height: "24px" }} />
            </div>
          )
        };
      }
      if (column.key === "id") {
        return {
          ...column,
          render: (item: any, record: any, index: number) => {
            if (activeVersion?.toString() !== currentVersion?.toString() || !entOrgUnits || entOrgUnitsCountExceed) {
              return (
                <Tooltip
                  title={
                    !entOrgUnits || entOrgUnitsCountExceed
                      ? TOOLTIP_ACTION_NOT_ALLOWED
                      : "This action can only be performed on the active version"
                  }
                  trigger="hover">
                  <Button className="ant-btn-outline mx-5" disabled icon="delete" />
                </Tooltip>
              );
            }

            return (
              <Popconfirm
                title={"Do you want to delete this item?"}
                disabled={!record.email}
                onConfirm={onRemoveHandler(record.id)}
                okText={"Yes"}
                cancelText={"No"}>
                <AntButton className="ant-btn-outline mx-5" icon={"delete"} disabled={!record.email || isReadOnly} />
              </Popconfirm>
            );
          }
        };
      }
      return column;
    });
  }, [missingEmails, mappedColumns, activeVersion, currentVersion]);

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedIds,
      onChange: onSelectionChange,
      hideDefaultSelections: false,
      selectedRows: selectedRows,
      getCheckboxProps: (records: any) => ({
        disabled: records?.disabled || false
      })
    }),
    [selectedIds, selectedRows]
  );

  const scrollX = useMemo(() => {
    return { x: "max-content" };
  }, []);

  const renderExtraContent = useMemo(() => {
    if (selectedIds.length === 1) {
      return null;
    }
    if (selectedIds.length > 1) {
      return (
        <Button onClick={() => setIsMerged(prev => !prev)} type="primary">
          Merge {selectedIds.length} Contributors
        </Button>
      );
    }
    const menu = (
      <Menu className="user-list-import-btn-menu">
        <Menu.Item key="1">
          <Tooltip title={!entOrgUnits || entOrgUnitsCountExceed ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
            <Button
              type="link"
              className="mr-10"
              onClick={handleCsvDownload("user")}
              disabled={!entOrgUnits || entOrgUnitsCountExceed}>
              {/* <Icon type="download" /> */}
              Export Contributors CSV
            </Button>
          </Tooltip>
        </Menu.Item>
        <Menu.Item key="2">
          <Tooltip title={!entOrgUnits || entOrgUnitsCountExceed ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
            <Button
              type="link"
              onClick={handleShowUsersModal("import")}
              disabled={!entOrgUnits || entOrgUnitsCountExceed || isReadOnly}>
              Import Contributors
            </Button>
          </Tooltip>
        </Menu.Item>
      </Menu>
    );
    return (
      <div className="flex">
        {!isReadOnly && (
          <Tooltip title="Configure Attributes" trigger="hover">
            <AntButton type="link" className="configure-btn" onClick={handleShowUsersModal("attribute")}>
              <Icon type="tool" theme="outlined" />
            </AntButton>
          </Tooltip>
        )}
        <Dropdown trigger={["click"]} overlay={menu} className="import-export-dropdown">
          <Button type="primary" className="import-export-btn">
            Import and Export Contributors <Icon type="down" />
          </Button>
        </Dropdown>
      </div>
    );
  }, [filters, currentVersion, entOrgUnits, entOrgUnitsCountExceed, selectedIds]);

  const renderEmailWarning = useMemo(() => {
    if (!missingEmails) {
      return null;
    }

    return (
      <div className="userlist-email-warning">
        <SvgIcon className={"pl-10"} icon="warning" />
        <AntText className={"pl-10"}>
          Warning: There are {missingEmails} users missing email IDs. This can be fixed by exporting the user list,
          adding email IDs and re-importing the user list.
        </AntText>
      </div>
    );
  }, [missingEmails]);

  const renderConfigureUsersModal = useMemo(() => {
    if (!configureUsersModel) {
      return null;
    }

    return (
      <ConfigureUsers
        type={configureUsersModel}
        onClose={handleHideUsersModal}
        exportExistingUsersHandler={handleCsvDownload("user")}
        exportSampleCsvHandler={handleCsvDownload("sample")}
      />
    );
  }, [configureUsersModel, filters]);

  const renderVersionsModal = useMemo(() => {
    if (!versionModal) {
      return null;
    }

    return <VersionModal handleClose={handleHideVersionsModal} />;
  }, [versionModal]);

  const renderWarning = useMemo(() => {
    if (!currentVersion || !activeVersion) {
      return null;
    }

    if (currentVersion.toString() !== activeVersion.toString()) {
      return (
        <AntAlert
          message="Informational Notes"
          type="info"
          showIcon
          className="userlist-version-warning"
          description={
            <AntText>
              {`You are viewing version ${currentVersion} of Contributors. The active version is ${activeVersion}.`}
              <a onClick={switchActiveVersionHandler}> Switch to the Active Version</a>
            </AntText>
          }
        />
      );
    }

    return null;
  }, [activeVersion, currentVersion]);

  const renderContent = useMemo(() => {
    const loading = get(versionListState, "loading", true);
    if ((currentVersion && loading === false) || (loading === false && !activeVersion)) {
      return (
        <ServerPaginatedTable
          queryParam={queryParams}
          pageName="users"
          title="Contributors"
          generalSearchField="full_name"
          className="org-user-list"
          uri="org_users"
          rowKey={getRowKey}
          columns={_columns}
          hasFilters
          hasDelete
          uuid={ORG_USER_LIST_ID}
          reload={reload}
          rowSelection={isReadOnly ? undefined : rowSelection}
          onFiltersChanged={handleFiltersChange}
          clearSelectedIds={clearSelection}
          bulkDeleting={deleteLoading}
          onBulkDelete={onBulkDelete}
          scroll={scrollX}
          extraSuffixActionButtons={renderExtraContent}
          clearFiltersCount={clearFilters}
          showOnlyFilterIcon={true}
          showNewVersion={isReadOnly ? undefined : handleShowVersionsModal}
          newSearch={true}
          skipConfirmationDialog={true}
          hideFilterButton={selectedIds.length > 0}
          setSearchValue={setSearchValue}
        />
      );
    }
  }, [
    versionListState,
    missingEmails,
    currentVersion,
    activeVersion,
    reload,
    selectedIds,
    selectedRows,
    filters,
    deleteLoading,
    mappedColumns
  ]);

  const renderLeavePagePopup = useMemo(() => {
    // @ts-ignore
    return (
      <LeavePageBlocker
        when={changesSelector?.dirty || false}
        path={`${getBaseUrl()}${ORGANIZATION_USERS_ROUTES._ROOT}`}
      />
    );
  }, [changesSelector, saveWarningModalState]);

  if (versionLoading) {
    return <Spin className="centered" size="large" />;
  }
  if (userUpdateLoading) {
    return <Spin className="centered" size="large" />;
  }
  if (isMerged || isUpdateUser) {
    return (
      <MergeUsers
        // @ts-ignore
        isUpdateUser={isUpdateUser}
        toggleEditMergeView={() => toggleEditMergeView()}
        allSelectedUsers={selectedRows}
        setUpdatedUserList={(user: any, discard: boolean) => onSaveUpdatedUsers(user, discard)}
        updatedUserList={updatedUserList}
        updateButtonState={() => updateButtonState()}
      />
    );
  }
  return (
    <div className={cx({ userListPage: !isStandaloneApp })}>
      {renderWarning}
      {renderEmailWarning}
      {renderConfigureUsersModal}
      {renderVersionsModal}
      {renderContent}
      {renderLeavePagePopup}
      {
        <Modal
          title={
            <div className="warning-title">
              <span>UNSAVED CHANGES</span>
            </div>
          }
          visible={(changesSelector as any)?.show_modal}
          className="org-user-warning"
          closable={false}
          cancelText="Discard"
          okText="Save"
          onCancel={(changesSelector as any)?.onCancel}
          onOk={() => {
            // @ts-ignore
            submitUpdatedUsers(updatedUserList);
          }}
          width={390}
          maskClosable={false}>
          <div className="flex flex-column warning-content">
            <span className="warning-icon">
              {" "}
              <Icon type="warning" />
              <span className="text">Warning</span>
            </span>
            <span>There are some unsaved changes.</span>
            <span className="warning-text">What would you like to do?</span>
          </div>
        </Modal>
      }
      {
        <Modal
          title={
            <div className="warning-title">
              <span>SAVE CHANGES</span>
              <Icon type="close" onClick={() => setSaveWarningModalState(!saveWarningModalState)} />
            </div>
          }
          visible={saveWarningModalState}
          className="org-user-warning"
          closable={false}
          cancelText="Cancel"
          okText="Proceed"
          onCancel={() => setSaveWarningModalState(!saveWarningModalState)}
          onOk={() => {
            // @ts-ignore
            submitUpdatedUsers(updatedUserList);
          }}
          width={390}
          maskClosable={false}>
          <div className="flex flex-column warning-content">
            <span className="warning-icon">
              {" "}
              <Icon type="warning" />
              <span className="text">Warning</span>
            </span>
            <span>
              Saving these changes will impact the Trellis scores for the modified users. However new scores will be
              populated in sometime.
            </span>
            <span className="warning-text">Are you sure you want to proceed?</span>
          </div>
        </Modal>
      }
    </div>
  );
};

export default UserListPage;
