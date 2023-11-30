import { Badge, Button, Card, Tooltip } from "antd";
import React, { useMemo, useState } from "react";
import {
  getLevelOpsTableColumns,
  levelopsTableReportFilteredRows
} from "reduxConfigs/selectors/levelopsTableReportDrilldownSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntTable, AntText, SvgIcon } from "shared-resources/components";
import DrillDownFilterContent from "shared-resources/containers/server-paginated-table/components/drilldown-filter-content/drilldown-filter-content";
import { baseColumnConfig } from "utils/base-table-config";
import { validateURL, validMarkdownLink } from "utils/stringUtils";
import { LevelopsTableReportColumnType } from "../../drilldown-types/levelopsTableReportTypes";
import { extractLinkAndTitleFromMarkdownUrl } from "./helper";
import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";

interface LevelopsTableReportDrilldownProps {
  filters: any;
  tableId: string;
  drilldownHeaderProps?: any;
  widgetId?: string;
  widgetDrilldownColumns?: Array<string>;
}

const LevelopsTableReportDrilldownComponent: React.FC<LevelopsTableReportDrilldownProps> = (
  props: LevelopsTableReportDrilldownProps
) => {
  const { filters, tableId, drilldownHeaderProps, widgetDrilldownColumns = [] } = props;
  const { title, type, showTitle, onOpenReport } = drilldownHeaderProps || {};

  const [page, setPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);

  const levelopsTableColumns: Array<LevelopsTableReportColumnType> = useParamSelector(getLevelOpsTableColumns, {
    tableId: `${tableId}?expand=schema,rows,history`
  });

  const levelOpsTableRow: Array<{ [x: string]: any }> = useParamSelector(levelopsTableReportFilteredRows, {
    tableFilters: filters,
    tableId: `${tableId}?expand=schema,rows,history`
  });
  const [selectedColumns, setSelectedColumns] = useState<any>(
    widgetDrilldownColumns?.length > 0
      ? widgetDrilldownColumns
      : (levelopsTableColumns || [])?.map(clm => clm?.dataIndex)?.slice(0, 5)
  );
  const mappedColumns = useMemo(() => {
    const columns = levelopsTableColumns.filter((clm: any) => selectedColumns.indexOf(clm?.dataIndex) !== -1);
    return columns.map((column: any) => {
      return {
        ...baseColumnConfig(column?.title, column?.dataIndex),
        render: (item: string | number | boolean) => {
          if (typeof item === "string" && validMarkdownLink(item)) {
            let { title, url, isValidUrl } = extractLinkAndTitleFromMarkdownUrl(item);
            if (isValidUrl) {
              return (
                <a href={url} target="_blank" rel="noopener noreferrer" style={{ color: "var(--link-and-actions)" }}>
                  {title}
                </a>
              );
            }
          }
          return item;
        }
      };
    });
  }, [levelopsTableColumns, selectedColumns, widgetDrilldownColumns]);

  const getRange = (page: number) => {
    const high = pageSize * page;
    const low = high - pageSize + 1;
    return { high, low };
  };

  const getDatasource = (curPage: number) => {
    const { high, low } = getRange(curPage);
    return levelOpsTableRow.slice(low - 1, high);
  };

  const onPageSizeChange = (pageSize: number) => {
    setPageSize(pageSize);
    setPage(1);
  };

  const cardTitle = useMemo(() => {
    if (!drilldownHeaderProps || !Object.keys(drilldownHeaderProps || {}).length) {
      return null;
    }

    return (
      <div className="flex align-center">
        <div className="mr-10">Drilldown Preview</div>
        <div>
          <Badge style={{ backgroundColor: "var(--harness-blue)" }} count={levelOpsTableRow?.length || 0} />
        </div>
      </div>
    );
  }, [levelOpsTableRow, drilldownHeaderProps]);

  const cardActions = useMemo(() => {
    if (!drilldownHeaderProps || !Object.keys(drilldownHeaderProps || {}).length) {
      return null;
    }

    const xAxisColumn = levelopsTableColumns.find((column: any) => column.id === (type as string).toLowerCase());
    const updatedDrilldownHeaderProps = {
      ...drilldownHeaderProps,
      type: xAxisColumn ? xAxisColumn?.title : type
    };
    return (
      <DrillDownFilterContent
        drilldownHeaderProps={updatedDrilldownHeaderProps}
        setSelectedColumns={(e: Array<string>) => setSelectedColumns(e)}
        displayColumnSelector={{
          availableColumns: levelopsTableColumns,
          visibleColumns: mappedColumns,
          widgetId: props.widgetId || "",
          defaultColumns: (levelopsTableColumns || [])?.slice(0, 5)
        }}
      />
    );
  }, [drilldownHeaderProps, levelopsTableColumns, widgetDrilldownColumns]);

  return (
    <Card
      title={cardTitle}
      extra={cardActions}
      bodyStyle={{ padding: "2px 0px 10px" }}
      bordered={false}
      headStyle={drilldownHeaderProps && { paddingRight: "0" }}>
      <AntTable
        page={page}
        pageSize={pageSize}
        hasCustomPagination
        onPageSizeChange={onPageSizeChange}
        onPageChange={(page: number) => setPage(page)}
        dataSource={getDatasource(page)}
        columns={mappedColumns}
        totalRecords={levelOpsTableRow?.length}
      />
    </Card>
  );
};

export default LevelopsTableReportDrilldownComponent;
