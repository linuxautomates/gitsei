import React, { useEffect, useMemo, useState } from "react";
import { TableV2, Container, Text, FontVariation, Button, ButtonVariation } from "@harness/uicore";
import { restapiClear } from "reduxConfigs/actions/restapi";
import { get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { paginationGet } from "reduxConfigs/actions/paginationActions";
import NoMapedIntegrations from "@assets/img/NoMappedIntegrations.png";
import {
  CheckboxCell,
  getFilter,
  RenderHeader,
  RenderIntName,
  RenderStatus,
  updatedAtCell
} from "./ConnectorsList.utils";
import { PermissionIdentifier, ResourceType } from "@harness/microfrontends";
import { useAppStore } from "contexts/AppStoreContext";
import { useParentProvider } from "contexts/ParentProvider";

interface ConnectorsListProps {
  workspaceIntegrations?: number[];
  selectedIntegrations: any[];
  onSelectionChange?: (selectedIntList: number[]) => void;
  searchItem?: string;
  setOpenWarning?: (value: boolean) => void;
}

const ConnectorsList = ({
  selectedIntegrations,
  onSelectionChange,
  searchItem,
  setOpenWarning,
  workspaceIntegrations
}: ConnectorsListProps): JSX.Element => {
  const [pageIndex, setPageIndex] = useState<number>(0);
  const [integrations, setIntegration] = useState<Array<any>>([]);
  const [loadingInt, setLoadingInt] = useState<boolean>(false);
  const [itemCount, setItemCount] = useState<number>(1);

  const pageCount = useMemo(() => Math.ceil(itemCount / 10), [itemCount]);

  const integrationListState = useSelector(state =>
    get(state, ["restapiReducer", "integrations", "list", "integration_list"], {})
  );

  const dispatch = useDispatch();
  const { accountInfo } = useAppStore();
  const {
    components: { RbacButton }
  } = useParentProvider();

  useEffect(() => {
    const filter = getFilter(workspaceIntegrations, searchItem);
    const filters = {
      page: pageIndex,
      page_size: 10,
      filter
    };

    setLoadingInt(true);
    setIntegration([]);
    dispatch(paginationGet("integrations", "list", filters, "integration_list", true, "all"));
    return () => {
      dispatch(restapiClear("integrations", "list", 0));
    };
  }, [pageIndex, searchItem, workspaceIntegrations]);

  useEffect(() => {
    if (loadingInt) {
      const loading = get(integrationListState, ["loading"], true);
      const error = get(integrationListState, ["error"], true);

      if (!loading && !error) {
        setLoadingInt(false);
        const data = get(integrationListState, ["data", "records"], []);
        const total = get(integrationListState, ["data", "_metadata", "total_count"], {});
        setItemCount(total);
        setIntegration(data);
      }
    }
  }, [integrationListState, loadingInt]);

  const columns = useMemo(() => {
    const col = [];
    if (onSelectionChange) {
      col.push({
        id: "checkbox",
        disableSortBy: true,
        Cell: CheckboxCell(selectedIntegrations, onSelectionChange),
        width: "5%"
      });
    }
    col.push({
      Header: RenderHeader("Integations"),
      id: "integrationName",
      Cell: RenderIntName,
      width: "30%"
    });
    col.push({
      Header: RenderHeader("Status"),
      id: "ingestionStatus",
      Cell: RenderStatus,
      width: "30%"
    });
    col.push({
      Header: "Last Updated",
      id: "updatedAt",
      Cell: updatedAtCell,
      width: "36%"
    });
    return col;
  }, [onSelectionChange, selectedIntegrations]);

  return (
    <Container padding="medium" className="connectorsList">
      {integrations && integrations.length > 0 ? (
        <TableV2
          columns={columns}
          data={integrations}
          pagination={{
            gotoPage: pageNo => setPageIndex(pageNo),
            itemCount,
            pageCount,
            pageIndex,
            pageSize: 10
          }}
        />
      ) : (
        <Container className="noIntegrations">
          <img width="352" height="193" src={NoMapedIntegrations} />
          <Text font={{ variation: FontVariation.H4 }}>Seems like you have no integrations added in your account</Text>
          <RbacButton
            icon="plus"
            text="New Integration"
            variation={ButtonVariation.SECONDARY}
            className="align-items-center"
            onClick={() => setOpenWarning && setOpenWarning(true)}
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
        </Container>
      )}
    </Container>
  );
};

export default ConnectorsList;
