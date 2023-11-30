import React, { useCallback, useContext, useEffect, useMemo, useState } from "react";
import { TableChartProps } from "../chart-types";
import { Button, Table } from "antd";
import { Link } from "react-router-dom";
import { AntTable, AntText, EmptyWidget } from "../../components";
import { getReportsPage } from "constants/routePaths";
import { get } from "lodash";
import { levelopsAcrossMap } from "../../../dashboard/constants/helper";
import { mapColumnsWithInfo } from "dashboard/helpers/mapColumnsWithInfo.helper";
import widgetConstants, { getWidgetConstant } from "dashboard/constants/widgetConstants";
import { getTableClick } from "../helper";
import { WidgetFilterContext, widgetOtherKeyDataContext } from "../../../dashboard/pages/context";
import { JIRA_MANAGEMENT_TICKET_REPORT } from "dashboard/constants/applications/names";

const TableChartComponent: React.FC<TableChartProps> = (props: TableChartProps) => {
  const { data, columns, size, reportType, onClick, hasClickEvents, previewOnly, metaData, otherKeyData, id } = props;
  const { setFilters } = useContext(WidgetFilterContext);
  const { setOtherKey } = useContext(widgetOtherKeyDataContext);

  useEffect(() => {
    if (metaData && metaData.length > 0 && metaData[0]?.calculated_at) {
      setOtherKey(id as string, {
        lastUpdatedTime: metaData[0]?.calculated_at
      });
    }
  }, [otherKeyData]);

  const onSortChange = useCallback(
    (pagination: any, filters: any, sorter: any, extra: any) => {
      const sortEntry = { id: sorter.field, desc: sorter.order === "descend" };
      setFilters(props.id as string, sortEntry);
    },
    [props.id]
  );

  const onPageChange = useCallback(
    (page: number, pageSize: number) => {
      const paginationFilter = { page: page - 1, page_size: pageSize || 10 };
      setFilters(props.id as string, paginationFilter);
    },
    [props.id]
  );

  const onPageSizeChange = useCallback(
    (pageSize: number, page: number) => {
      const paginationFilter = { page: page, page_size: pageSize || 10 };
      setFilters(props.id as string, paginationFilter);
    },
    [props.id]
  );

  const getColumns = () => {
    if (columns && columns?.length === 0) {
      return [];
    }

    let _columns = columns;

    //@ts-ignore
    if (props?.reportType === "levelops_assessment_response_time__table_report") {
      const xunitkey = get(levelopsAcrossMap, [props.xUnit], props.xUnit);
      _columns = columns.map((column: any) => {
        if (column.dataIndex === "key") {
          const render = (item: any, record: any, index: any) => {
            let mappedLabel = item;
            let filters: any;
            if (xunitkey === "completed" || xunitkey === "submitted") {
              filters = { [xunitkey]: record.id };
              if (xunitkey === "completed") {
                mappedLabel = item === "true" ? "Completed" : "Not Completed";
              }
              if (xunitkey === "submitted") {
                mappedLabel = item === "true" ? "Submitted" : "Not Submitted";
              }
            } else {
              filters = { [xunitkey]: [record.id] };
            }
            const url = `${getReportsPage()}?tab=assessments&filters=${JSON.stringify(filters)}`;
            return (
              <AntText className={"pl-10"}>
                <Link className={"ellipsis"} to={url} target={"_blank"}>
                  {mappedLabel}
                </Link>
              </AntText>
            );
          };
          const modifiedColumn = { ...column, render: render };
          return modifiedColumn;
        }
        return column;
      });
    }
    return _columns?.length > 0 ? _columns : [];
  };

  const mappedColumns = () => {
    const columns = getColumns();
    const columnsWithInfo = get(widgetConstants, [props?.reportType, "drilldown", "columnsWithInfo"], undefined);
    return mapColumnsWithInfo(columns, columnsWithInfo);
  };

  const checkForPaginationTable = useMemo(() => {
    return get(widgetConstants, [props?.reportType, "hasPaginationTableOnWidget"], false);
  }, [reportType]);

  const onTableRowClickHandler = (record: any) => {
    const onChartClickPayload = getWidgetConstant(reportType, ["onChartClickPayload"]);
    if (onChartClickPayload) {
      onClick(onChartClickPayload(record));
    }
  };

  if ((data || []).length === 0) {
    return <EmptyWidget />;
  }

  return (
    <>
      {checkForPaginationTable ? (
        <AntTable
          page={metaData[0]?.page + 1 || 1}
          pageSize={metaData[0]?.page_size || 10}
          hasCustomPagination
          hasPagination
          hideTotal={false}
          pagination={false}
          totalPages={1}
          rowClassName={() => "propelo-table-row"}
          className="propelo-table-chart"
          showPageSizeOptions={true}
          bordered={true}
          scroll={{ y: "28rem" }}
          onPageChange={(page: number) => onPageChange(page, metaData[0]?.page_size || 10)}
          onPageSizeChange={(pageSize: any) => onPageSizeChange(pageSize, metaData[0]?.page)}
          dataSource={data || []}
          columns={mappedColumns()}
          totalRecords={metaData[0]?.total_count || 10}
          onRow={(record: any, rowIndex: number) => {
            return {
              onClick: (e: any) => onTableRowClickHandler(record || {})
            };
          }}
        />
      ) : (
        <div className={"table-chart"} style={{ margin: "2px", maxHeight: "20rem", overflowY: "scroll" }}>
          <AntTable
            size={size || "default"}
            dataSource={(data || []).slice(0, 10)}
            columns={mappedColumns()}
            pagination={false}
            onChange={onSortChange}
          />
        </div>
      )}

      {hasClickEvents && reportType !== JIRA_MANAGEMENT_TICKET_REPORT.JIRA_RELEASE_TABLE_REPORT && (
        <div style={{ textAlign: "right" }}>
          <Button type={"link"} onClick={e => onClick(getTableClick(reportType, props.xUnit || ""))}>
            More
          </Button>
        </div>
      )}
    </>
  );
};

export default TableChartComponent;
