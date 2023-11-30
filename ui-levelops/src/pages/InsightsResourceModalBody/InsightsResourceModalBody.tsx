import React, { ReactElement, useCallback, useEffect, useMemo, useState } from "react";
import { TableV2, Checkbox, PageSpinner } from "@harness/uicore";
import type { Renderer, CellProps } from "react-table";
import { useDashboardsListMutation } from "@harnessio/react-sei-service-client";
import { useWorkspace } from "custom-hooks/useWorkspace";
import ReduxStoreProvider from "reduxConfigs/ReduxStoreProvider";
import { RbacResourceModalProps } from "@harness/microfrontends/dist/modules/20-rbac/factories/RbacFactory";
import useOpenApiClients from "custom-hooks/useOpenAPIClients";
import { buildQueryParam } from "helper/queryParamHelper";

const InsightsResource = ({
  searchTerm,
  selectedData,
  onSelectChange,
  resourceScope
}: RbacResourceModalProps): ReactElement => {
  const [pageIndex, setPageIndex] = useState<number>(0);
  const [itemCount, setItemCount] = useState<number>(1);
  const [dashboards, setDashboards] = useState<any[]>([]);
  const { accountIdentifier, projectIdentifier = "", orgIdentifier = "" } = resourceScope;

  const pageCount = useMemo(() => Math.ceil(itemCount / 10), [itemCount]);
  const queryParams = buildQueryParam(accountIdentifier, orgIdentifier, projectIdentifier);

  const { mutate: getDashboards, isLoading, isError, data } = useDashboardsListMutation({});

  const { workspaceId, isFetching } = useWorkspace({
    accountId: accountIdentifier,
    projectIdentifier: projectIdentifier,
    orgIdentifier: orgIdentifier
  });

  useEffect(() => {
    if (!isLoading && !isError && data) {
      // @ts-ignore
      const { records } = data.content;
      if (records) {
        setDashboards(records);
        setItemCount(records.length);
      }
    }
  });

  useEffect(() => {
    if (!isFetching && workspaceId) {
      getDashboards({
        queryParams,
        body: {
          filter: {
            // @ts-ignore
            workspace_id: Number(workspaceId),
            has_rbac_access: true
          }
        }
      });
    }
  }, [isFetching, workspaceId]);

  const updateSelection = useCallback(
    (nodeId: string, action: "add" | "remove") => {
      let newSelection = [];
      if (action === "add") {
        newSelection = [...selectedData, nodeId];
      } else {
        newSelection = selectedData.filter(id => id !== nodeId);
      }
      onSelectChange(newSelection);
    },
    [selectedData, onSelectChange]
  );

  const renderInsightName: Renderer<CellProps<any>> = ({ row }) => {
    const { name, id } = row.original;
    const isChecked = selectedData.includes(id);
    return (
      <Checkbox
        labelElement={name}
        checked={isChecked}
        onChange={e => {
          updateSelection(id, e.currentTarget.checked ? "add" : "remove");
        }}
      />
    );
  };
  const pageData = useMemo(() => dashboards.slice((pageIndex - 1) * 10, 10), [dashboards, pageIndex]);
  if (isLoading) return <PageSpinner />;
  return (
    <TableV2
      columns={[
        {
          Header: "Insights",
          Cell: renderInsightName,
          id: "insights"
        }
      ]}
      data={pageData}
      pagination={{
        gotoPage: pageNo => setPageIndex(pageNo),
        itemCount,
        pageCount,
        pageIndex,
        pageSize: 10
      }}
    />
  );
};

const InsightsResourceModalBody = (props: RbacResourceModalProps) => {
  window.isStandaloneApp = false;
  useOpenApiClients(() => {}, props.resourceScope.accountIdentifier);
  return (
    <ReduxStoreProvider>
      <InsightsResource {...props} />
    </ReduxStoreProvider>
  );
};

export default InsightsResourceModalBody;
