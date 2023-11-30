import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { ServerPaginatedTable } from "shared-resources/containers";
import { AntCard } from "shared-resources/components";
import { tableCell } from "utils/tableUtils";
import { tableColumns } from "./table-config";
import { Typography } from "antd";
import { restapiClear } from "reduxConfigs/actions/restapi";

const { Text } = Typography;

interface SignaturesListPageProps {}
export const SignaturesListPage: React.FC<SignaturesListPageProps> = (props: SignaturesListPageProps) => {
  const dispatch = useDispatch();

  useEffect(() => {
    return () => {
      dispatch(restapiClear("signatures", "list", "0"));
    }; // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const onRowClickHandler = (e: any, t: any, rowInfo: any) => {};

  const subComponent = (row: any) => {
    return (
      <div style={{ marginTop: "20px", marginBottom: "20px" }}>
        <Text type={"secondary"}>{row.description}</Text>
      </div>
    );
  };

  const PAGE_ERROR = "Could not fetch signatures";

  const mappedColumns = tableColumns.map(column => {
    if (["tags", "products", "integration_types", "enabled"].includes(column.key)) {
      return {
        ...column,
        render: (props: any) => tableCell(column.key, props)
      };
    }
    return column;
  });
  return (
    <AntCard title="Signatures">
      <ServerPaginatedTable
        restCall="getSignatures"
        uri="signatures"
        backendErrorMessage={PAGE_ERROR}
        columns={mappedColumns}
        getTrProps={(state: any, rowInfo: any) => {
          return {
            onMouseOver: (e: any, t: any) => {
              onRowClickHandler(e, t, rowInfo);
            }
          };
        }}
        expandedRowRender={(newExpanded: any) => subComponent(newExpanded)}
        partialFilters={{}}
        hasFilters={false}
      />
    </AntCard>
  );
};

export default React.memo(SignaturesListPage);
