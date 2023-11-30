import React, { useCallback, useEffect, useState, useMemo } from "react";
import { notification } from "antd";
import { get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { RouteChildrenProps } from "react-router-dom";
import {
  prepelsCreate,
  prepelsDelete,
  prepelsGet,
  restapiClear,
  prepelsBulkDelete
} from "reduxConfigs/actions/restapi";
import {
  propelCreateState,
  propelDeleteState,
  propelGetState,
  propelBulkDeleteState
} from "reduxConfigs/selectors/restapiSelector";
import { TableRowActions } from "shared-resources/components";
import { RestPropel } from "../../../classes/RestPropel";
import Loader from "../../../components/Loader/Loader";
import { getBaseUrl } from "../../../constants/routePaths";
import { tableColumns } from "./table-config";
import { parseQueryParamsIntoKeys } from "../../../utils/queryUtils";
import RestApiPaginatedTableUrlWrapper from "../../../shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { appendErrorMessagesHelper } from "utils/arrayUtils";
import { useHasEntitlements } from "./../../../custom-hooks/useHasEntitlements";
import { Entitlement, TOOLTIP_ACTION_NOT_ALLOWED } from "./../../../custom-hooks/constants";
import { AntText } from "shared-resources/components";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";

interface PropelListContainerProps extends RouteChildrenProps {}

const PropelListContainer: React.FC<PropelListContainerProps> = (props: PropelListContainerProps) => {
  const [moreFilters, setMoreFilters] = useState<any>({});
  const [deletePropelId, setDeletePropelId] = useState<string | undefined>(undefined);
  const [deletePropelTempId, setDeletePropelTempId] = useState<string | undefined>(undefined);
  const [deleteLoading, setDeleteLoading] = useState<boolean>(false);
  const [reload, setReload] = useState<number>(1);
  const [cloneId, setCloneId] = useState<string | undefined>(undefined);
  const [clonePropelLoading, setClonePropelLoading] = useState<boolean>(false);
  const [cloningPropel, setCloningPropel] = useState<boolean>(false);
  const [selectedIds, setSelectedIds] = useState<any>([]);
  const [bulkDeleting, setBulkDeleting] = useState<boolean>(false);

  const dispatch = useDispatch();
  const deleteState = useSelector((state: any) => propelDeleteState(state, deletePropelId));
  const restDataState = useSelector((state: any) => propelGetState(state, cloneId));
  const bulkDeleteState = useSelector((state: any) => propelBulkDeleteState(state));
  const createState = useSelector(propelCreateState);
  const entPropels = useHasEntitlements(Entitlement.PROPELS);

  const { history, location } = props;

  const queryParamsToParse = useMemo(() => ["enabled", "trigger_template_type"], []);

  useEffect(() => {
    if (deleteLoading) {
      const loading = get(deleteState, ["loading"], true);
      const error = get(deleteState, ["error"], true);
      if (!loading) {
        if (!error) {
          const data = get(deleteState, ["data"], {});
          const success = get(data, ["success"], true);
          if (!success) {
            notification.error({
              message: data["error"]
            });
          } else {
            setSelectedIds((ids: string[]) => ids.filter(id => id !== deletePropelTempId));
            setReload((prev: number) => prev + 1);
          }
        }
        setDeleteLoading(false);
        setDeletePropelId(undefined);
        setDeletePropelTempId(undefined);
      }
    }
  }, [deleteState]);

  useEffect(() => {
    if (bulkDeleting) {
      const { loading, error } = bulkDeleteState;
      if (!loading) {
        if (!error) {
          const data = get(bulkDeleteState, ["data", "records"], []);
          const errorMessages = appendErrorMessagesHelper(data);
          let errorOccurs = false;
          if (errorMessages.length) {
            errorOccurs = true;
            notification.error({
              message: errorMessages
            });
          }
          if (errorOccurs) {
            setReload(state => state + 1);
          } else {
            setSelectedIds([]);
            setReload(state => state + 1);
          }
        }
        setBulkDeleting(false);
      }
    }
  }, [bulkDeleteState]);

  useEffect(() => {
    if (clonePropelLoading) {
      const { loading, error } = restDataState;
      if (!loading && !error) {
        const restData = get(restDataState, ["data"], {});
        const newPropelData = {
          ...restData,
          name: "copy of " + restData.name,
          nodes_dirty: true,
          // LEV-1973 Force enabled to be false to prevent
          // unnecessary running.
          enabled: false
        };
        const newPropel = new RestPropel(newPropelData);
        dispatch(prepelsCreate(newPropel));
        setClonePropelLoading(false);
        setCloningPropel(true);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [restDataState]);

  useEffect(() => {
    if (cloningPropel) {
      const { loading, error } = createState;
      if (!loading && !error) {
        const newPropel = get(createState, ["data"], {});
        notification.info({
          message: "Propel Cloned Successfully"
        });
        history.push(`${getBaseUrl()}/propels/propels-editor?propel=${newPropel.permanent_id}`);
        setCloningPropel(false);
        setCloneId(undefined);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [createState]);

  useEffect(() => {
    const queryFilters = parseQueryParamsIntoKeys(location.search, ["enabled", "trigger_template_type"]);
    if (queryFilters) {
      const { enabled, trigger_template_type } = queryFilters;
      if (enabled) {
        queryFilters["enabled"] = enabled[0] === "true";
      }
      if (trigger_template_type) {
        queryFilters["trigger_template_type"] = trigger_template_type[0];
      }
      setMoreFilters(queryFilters);
    }
    return () => {
      dispatch(restapiClear("propels", "get", 0));
      dispatch(restapiClear("propels", "bulkDelete", "-1"));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const handleParsedQueryParams = useCallback((filters: any) => {
    if (filters) {
      const { enabled, trigger_template_type } = filters;
      if (enabled && enabled.length) {
        filters["enabled"] = enabled[0] === "true";
      }

      if (trigger_template_type && trigger_template_type.length) {
        filters["trigger_template_type"] = trigger_template_type[0];
      }
    }
    return filters;
  }, []);

  const queryParamsFromFilters = useCallback((filters: any, tab = "propels") => {
    if (!filters) {
      return {
        tab
      };
    }
    const { enabled, trigger_template_type } = filters;
    return {
      enabled: enabled !== undefined ? (enabled ? "true" : "false") : undefined,
      trigger_template_type,
      tab
    };
  }, []);

  const clonePropel = (propelId: string) => {
    dispatch(prepelsGet(propelId));
    setCloneId(propelId);
    setClonePropelLoading(true);
  };

  const onRemoveHandler = (propelId: any) => {
    setDeletePropelId(propelId.permanentId);
    setDeletePropelTempId(propelId.id);
    setDeleteLoading(true);
    dispatch(prepelsDelete(propelId.permanentId));
  };

  const [hasCreateAccess, hasEditAccess, hasDeleteAccess] = useConfigScreenPermissions();
  const buildActionOptions = (record: any) => {
    const actions = [
      {
        type: "copy",
        id: record.permanent_id,
        description: "Clone",
        onClickEvent: clonePropel,
        disabled: !entPropels || !hasCreateAccess,
        toolTip: !entPropels || !hasCreateAccess ? TOOLTIP_ACTION_NOT_ALLOWED : ""
      },
      {
        type: "delete",
        id: { permanentId: record.permanent_id, id: record.id }, // we need both ids
        description: "Delete",
        onClickEvent: onRemoveHandler,
        disabled: !entPropels || !hasDeleteAccess,
        toolTip: !entPropels || !hasDeleteAccess ? TOOLTIP_ACTION_NOT_ALLOWED : ""
      }
    ];
    return <TableRowActions actions={actions} />;
  };

  const onSelectChange = (rowKeys: any) => {
    setSelectedIds(rowKeys);
  };

  const clearSelectedIds = () => {
    setSelectedIds([]);
  };

  const mappedColumns = useMemo(
    () =>
      tableColumns().map(column => {
        if (column.dataIndex === "id") {
          return {
            ...column,
            width: 100,
            render: (text: string, record: any, index: number) => buildActionOptions(record)
          };
        }
        if (column.dataIndex === "name" && !entPropels) {
          return {
            ...column,
            width: 100,
            render: (text: string, record: any, index: number) => {
              return <AntText className={"pl-10"}>{text}</AntText>;
            }
          };
        }
        return column;
      }),
    [entPropels]
  );

  const onBulkDelete = () => {
    dispatch(prepelsBulkDelete(selectedIds));
    setBulkDeleting(true);
  };

  const rowSelection = useMemo(
    () => ({
      selectedRowKeys: selectedIds,
      onChange: onSelectChange,
      getCheckboxProps: (rec: any) => ({
        disabled: !entPropels
      })
    }),
    [selectedIds, entPropels]
  );

  if (clonePropelLoading || cloningPropel || deleteLoading) {
    return <Loader />;
  }

  return (
    <>
      <RestApiPaginatedTableUrlWrapper
        pageName="propels"
        uri="propels"
        method="list"
        columns={mappedColumns}
        hasFilters
        filters={moreFilters}
        reload={reload}
        buildQueryParamsFromFilters={queryParamsFromFilters}
        query_params_to_parse={queryParamsToParse}
        onQueryParamsParsed={handleParsedQueryParams}
        hasDelete={false}
        clearSelectedIds={clearSelectedIds}
        rowSelection={rowSelection}
        onBulkDelete={onBulkDelete}
        bulkDeleting={bulkDeleting}
      />
    </>
  );
};

export default PropelListContainer;
