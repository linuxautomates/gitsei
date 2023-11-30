import React, { useEffect, useState } from "react";
import { get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { RouteComponentProps } from "react-router-dom";
import { AntText, TableRowActions } from "shared-resources/components";
import { ServerPaginatedTable } from "shared-resources/containers";
import { tableColumns } from "./table-config";
import { toTitleCase } from "utils/stringUtils";
import { ApiKeyCreate } from "../../../containers/apikeys";
import { pageSettings } from "reduxConfigs/selectors/pagesettings.selector";
import { clearPageSettings, setPageButtonAction, setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { restapiClear } from "reduxConfigs/actions/restapi/restapiActions";
import { apikeysDelete } from "reduxConfigs/actions/restapi";
import ErrorWrapper from "hoc/errorWrapper";
import Loader from "components/Loader/Loader";
import { restAPILoadingState } from "../../../../utils/stateUtil";
import { apiKeyDeleteState } from "reduxConfigs/selectors/apiKeysSelector";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";

interface ApikeysListPageProps extends RouteComponentProps {
  moreFilters?: any;
}

export const ApikeysListPage: React.FC<ApikeysListPageProps> = (props: ApikeysListPageProps) => {
  const [deleteApiKey, setDeleteApiKey] = useState<string | undefined>(undefined);
  const [deleting, setDeleting] = useState(false);
  const [showCreateApiKeyModal, setCreateApiKeyModalVisibility] = useState(false);
  const [apiKey, setAPIKey] = useState<{ id: string; key: string } | undefined>(undefined);

  const dispatch = useDispatch();

  const pageSettingsState = useSelector(pageSettings);
  const apiKeyDltState = useSelector(apiKeyDeleteState);
  const entApiKeys = useHasEntitlements(Entitlement.SETTING_API_KEYS);
  const entApiKeysCountExceed = useHasEntitlements(Entitlement.SETTING_API_KEYS_COUNT_2, EntitlementCheckType.AND);

  const { pathname } = props.location;

  useEffect(() => {
    if (deleting) {
      const { loading } = restAPILoadingState(apiKeyDltState, deleteApiKey);
      if (!loading) {
        setDeleting(false);
        setDeleteApiKey(undefined);
      }
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [apiKeyDltState]);

  useEffect(() => {
    const addBtnClicked = get(pageSettingsState, [pathname, "action_buttons", "primary", "hasClicked"], false);
    if (addBtnClicked) {
      setCreateApiKeyModalVisibility(true);
      dispatch(setPageButtonAction(pathname, "primary", { hasClicked: false }));
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pageSettingsState]);

  useEffect(() => {
    dispatch(
      setPageSettings(pathname, {
        title: "Apikeys",
        action_buttons: {
          primary: {
            type: "primary",
            label: "Create API Key",
            hasClicked: false,
            disabled: !entApiKeys || entApiKeysCountExceed,
            tooltip: (!entApiKeys || entApiKeysCountExceed) && TOOLTIP_ACTION_NOT_ALLOWED
          }
        }
      })
    );
    return () => {
      dispatch(restapiClear("apikeys", "list", "0"));
      dispatch(restapiClear("apikeys", "delete", "-1"));
      dispatch(restapiClear("apikeys", "create", "0"));
      dispatch(clearPageSettings(pathname));
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [entApiKeys, entApiKeysCountExceed]);

  const onRemoveHandler = (id: string) => {
    setDeleteApiKey(id);
    setDeleting(true);
    if (apiKey && apiKey.id === id) {
      setAPIKey(undefined);
    }
    dispatch(apikeysDelete(id));
  };

  const buildActionOptions = (actionProps: { id: string }) => {
    const actions = [
      {
        type: "delete",
        id: actionProps.id,
        onClickEvent: onRemoveHandler,
        disabled: !entApiKeys || entApiKeysCountExceed,
        tooltip: (!entApiKeys || entApiKeysCountExceed) && TOOLTIP_ACTION_NOT_ALLOWED
      }
    ];
    return <TableRowActions actions={actions} />;
  };

  const mappedColumns = tableColumns.map((column: any) => {
    if (column.dataIndex === "id") {
      return {
        ...column,
        render: (item: any, record: any, index: number) => buildActionOptions(record)
      };
    }
    if (column.dataIndex === "role") {
      return {
        ...column,
        render: (props: any) => toTitleCase(props)
      };
    }
    return column;
  });

  if (deleting) {
    return <Loader />;
  }

  const renderApiKeyCreateModal = () => {
    if (!showCreateApiKeyModal) {
      return null;
    }
    return (
      <ApiKeyCreate
        // @ts-ignore
        onCancel={() => setCreateApiKeyModalVisibility(false)}
        onOk={(apiKey: { id: string; key: string }) => {
          setCreateApiKeyModalVisibility(false);
          setAPIKey(apiKey);
        }}
      />
    );
  };

  const renderNewlyCreatedApiKey = () => {
    if (!apiKey) {
      return null;
    }
    const { key } = apiKey;
    return (
      <div style={{ margin: "10px" }}>
        Created:
        <AntText
          style={{
            margin: "10px 10px 0px 10px",
            maxWidth: "80vw",
            overflowX: "hidden",
            display: "block",
            wordWrap: "break-word"
          }}
          code
          copyable={{
            text: key
          }}>
          {key}
        </AntText>
        <span style={{ margin: "0px 10px" }}>(copy key, will not be displayed again)</span>
      </div>
    );
  };

  const renderPaginatedTable = () => {
    if (showCreateApiKeyModal) {
      return null;
    }
    return (
      <>
        <ServerPaginatedTable
          pageName={"apikeys"}
          uri="apikeys"
          moreFilters={props.moreFilters || {}}
          columns={mappedColumns}
          hasFilters={false}
        />
      </>
    );
  };

  return (
    <div>
      {renderApiKeyCreateModal()}
      {renderNewlyCreatedApiKey()}
      {renderPaginatedTable()}
    </div>
  );
};

export default ErrorWrapper(ApikeysListPage);
