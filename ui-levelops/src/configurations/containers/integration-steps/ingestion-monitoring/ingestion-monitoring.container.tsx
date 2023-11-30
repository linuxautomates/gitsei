import React, { useCallback, useMemo, useState } from "react";
import { Icon } from "antd";
import RestApiPaginatedTableUrlWrapper from "../../../../shared-resources/containers/server-paginated-table/rest-api-paginated-table-url-wrapper";
import { IngestionLogTableConfig } from "./ingestion-table-config";
import { AntText } from "../../../../shared-resources/components";
import "./ingestion-monitoring.style.scss";
import { INTEGRATION_EDIT_TABS } from "../../../../constants/integrationEdit";
import { IngestionStats } from "..";
import { baseColumnConfig } from "../../../../utils/base-table-config";
import { AntTableComponent } from "../../.../../../../shared-resources/components/ant-table/ant-table.component";
import { INCLUDE_RESULT_FIELD_KEY } from "./constants";
import { IntegrationTypes } from "constants/IntegrationTypes";

interface IngestionMonitoringProps {
  id: string;
  application: string;
}

const queryParamsToParse = ["statuses"];
const unsupportedApplication = ["jenkins"];
const FailureTabelColumns = [
  {
    title: "message",
    dataIndex: "message",
    key: "message"
  },
  {
    title: "severity",
    dataIndex: "severity",
    key: "severity"
  },
  {
    title: "url",
    dataIndex: "url",
    key: "url"
  }
];

const IngestionMonitoringContainer: React.FC<IngestionMonitoringProps> = ({ id, application }) => {
  const scrollX = useMemo(() => {
    return { x: "fit-content" };
  }, []);

  let ingestionLogTableConfig = [...IngestionLogTableConfig];
  let expandedRowRender;
  const [expandedRowKeys, setExpandedRowKeys] = useState<string[]>([]);
  if (application === IntegrationTypes.GITLAB) {
    const onClick = (row: any) => {
      const rowKey = row.id;
      if (expandedRowKeys.includes(rowKey)) {
        setExpandedRowKeys([]);
      } else {
        setExpandedRowKeys([rowKey]);
      }
    };
    ingestionLogTableConfig.push({
      ...baseColumnConfig("Failures", "failures", { width: "6%" }),
      // @ts-ignore
      render: (failures: string, row: any) => {
        if (failures?.length) {
          const expanded = expandedRowKeys.includes(row.id);
          const failureText = `${failures.length} failure(s) `;
          return (
            <span>
              {failureText}
              {expanded ? (
                <Icon type="minus-square" className={"icon-style"} onClick={() => onClick(row)} />
              ) : (
                <Icon type="plus-square" className={"icon-style"} onClick={() => onClick(row)} />
              )}
            </span>
          );
        }
        return <span>--</span>;
      }
    });

    expandedRowRender = (row: any) =>
      row.failures?.length ? (
        <AntTableComponent
          dataSource={row.failures}
          columns={FailureTabelColumns}
          hasPagination={row.failures?.length > 10}
        />
      ) : (
        ""
      );
  }

  const queryParamsFromFilters = useCallback(
    (filters: any, tab = INTEGRATION_EDIT_TABS.MONITORING) => {
      if (!filters) {
        return {
          tab,
          id
        };
      }

      const { statuses } = filters;
      return {
        tab,
        id,
        statuses
      };
    },
    [id]
  );

  const handleParsedQueryParams = (filters: any) => {
    const { statuses } = filters;
    let filter: { id?: string; statuses?: string[] | string; [INCLUDE_RESULT_FIELD_KEY]?: boolean } = {
      id,
      [INCLUDE_RESULT_FIELD_KEY]: true
    };
    if (statuses) {
      filter = { ...filter, statuses };
    }
    return filter;
  };

  const titleComponent = useMemo(() => {
    return <AntText className="ingestion-monitoring--title">Ingestion Logs</AntText>;
  }, []);

  return (
    <div className="ingestion-monitoring">
      <IngestionStats id={id} />
      {!unsupportedApplication.includes(application) && (
        <RestApiPaginatedTableUrlWrapper
          pageName="ingestionMonitoring"
          uri="ingestion_integration_logs"
          method="list"
          columns={ingestionLogTableConfig}
          hasFilters
          hasSearch={false}
          uuid={"something_random"}
          scroll={scrollX}
          buildQueryParamsFromFilters={queryParamsFromFilters}
          query_params_to_parse={queryParamsToParse}
          onQueryParamsParsed={handleParsedQueryParams}
          componentTitle={titleComponent}
          expandedRowRender={expandedRowRender}
          expandIconColumnIndex={-1}
          expandedRowKeys={expandedRowKeys}
        />
      )}
    </div>
  );
};

export default React.memo(IngestionMonitoringContainer);
