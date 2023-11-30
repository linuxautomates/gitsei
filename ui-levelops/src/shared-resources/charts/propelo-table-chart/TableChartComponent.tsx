import { Tooltip, Typography } from "antd";
import { TableStateFilters, SortOrder, ColumnProps } from "antd/lib/table";
import { genericSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { extractLinkAndTitleFromMarkdownUrl } from "dashboard/pages/dashboard-drill-down-preview/components/levelops-table-report-drilldown-component/helper";
import { LevelopsTableReportColumnType } from "dashboard/pages/dashboard-drill-down-preview/drilldown-types/levelopsTableReportTypes";
import { cloneDeep, get } from "lodash";
import React, { createRef, useCallback, useEffect, useMemo, useState } from "react";
import { Legend } from "recharts";
import {
  getLevelOpsTableColumns,
  levelopsTableReportFilteredRows
} from "reduxConfigs/selectors/levelopsTableReportDrilldownSelector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntTable } from "shared-resources/components";
import { arrayMove } from "utils/arrayUtils";
import { baseColumnConfig } from "utils/base-table-config";
import { validMarkdownLink, validateURL } from "utils/stringUtils";
import { PropeloTableChartProps } from "../chart-types";
import CustomLegendComponent from "./CustomLegendComponent";
import "./propeloTableChartComponent.styles.scss";

const TableChartComponent: React.FC<PropeloTableChartProps> = ({
  tableId,
  columns,
  tableFilters,
  validOUIds,
  showOUSpecificData
}: PropeloTableChartProps) => {
  const [page, setPage] = useState<number>(1);
  const [sortingConfig, setSortingConfig] = useState<{ sortOrder: string; key: string }>();
  const parentRef = createRef<HTMLDivElement>();
  const [parentWidth, setParentWidth] = useState(0);
  const COLOR_CODES: any = {
    elite: "#BDEAB7",
    high: "#CDF4FE",
    medium: "#FEE89D",
    low: "#EE7067"
  };

  const levelopsTableColumns: Array<LevelopsTableReportColumnType> = useParamSelector(getLevelOpsTableColumns, {
    tableId: `${tableId}?expand=schema,rows,history`
  });
  const levelOpsTableRow: Array<{ [x: string]: any }> = useParamSelector(levelopsTableReportFilteredRows, {
    tableFilters,
    tableId: `${tableId}?expand=schema,rows,history`
  });

  const updateWidthAndHeight = () => {
    if (parentWidth !== parentRef?.current?.offsetWidth) {
      setParentWidth(parentRef?.current?.offsetWidth || 0);
    }
  };

  useEffect(() => {
    window.addEventListener("resize", updateWidthAndHeight);
    return () => window.removeEventListener("resize", updateWidthAndHeight);
  }, []);

  useEffect(() => {
    if (parentRef.current?.offsetWidth && parentWidth !== parentRef?.current?.offsetWidth) {
      setParentWidth(parentRef.current?.offsetWidth);
    }
  }, [parentRef, parentWidth]);

  const sortedData = useMemo(() => {
    const nData = cloneDeep(levelOpsTableRow);
    if (sortingConfig) {
      const findDataType = (levelopsTableColumns || []).find(col => col.dataIndex === sortingConfig?.key);
      nData.sort(
        genericSortingComparator(
          sortingConfig?.key,
          sortingConfig?.sortOrder === "ascend" ? "asc" : "desc",
          findDataType?.inputType
        )
      );
    }
    return nData;
  }, [sortingConfig, levelOpsTableRow]);

  const handleSetSortConfig = (key: string, order: string, noSorting = false) => {
    if (!noSorting) {
      setSortingConfig(prev => {
        if (prev?.key !== key || prev?.sortOrder !== order) return { key, sortOrder: order };
        return prev;
      });
    } else {
      setSortingConfig(undefined);
    }
  };

  const getColumnTitleFunc =
    (title: string) =>
    ({
      sortOrder,
      sortColumn
    }: {
      filters: TableStateFilters;
      sortOrder?: SortOrder | undefined;
      sortColumn?: ColumnProps<any> | null | undefined;
    }) => {
      if (sortOrder && sortColumn?.dataIndex) {
        handleSetSortConfig(sortColumn?.dataIndex, sortOrder);
      } else {
        handleSetSortConfig("", "", true);
      }
      return title;
    };

  const ouColumn: any = useMemo(() => {
    return levelopsTableColumns.find(col => col?.key?.toLowerCase() === "ou_id");
  }, [levelopsTableColumns]);

  const showLegend = useMemo(() => {
    return levelopsTableColumns.find(col => col?.key?.toLowerCase()?.includes("_color_code"));
  }, []);
  const mappedColumns = useMemo(() => {
    let nColumns = levelopsTableColumns;
    if (columns?.length && !columns.includes("all")) {
      nColumns = levelopsTableColumns.filter(col => columns.includes(col?.id ?? ""));
    }
    const width = nColumns?.length >= 8 ? (parentWidth + 120) / 8 : (parentWidth - 120) / nColumns?.length;
    return nColumns.map((column, index) => {
      return {
        ...baseColumnConfig(column?.title as string, column?.dataIndex ?? "", { sorter: true, ellipsis: false }),
        title: getColumnTitleFunc(column?.title as string),
        fixed: index === 0 ? "left" : false,
        width: width,
        minWidth: width,
        align: index === 0 ? "left" : "center",
        render: (item: string | number | boolean, record: any, colIndex: number) => {
          const firstColumn = column?.dataIndex === levelopsTableColumns?.[0]?.dataIndex;
          const showOuTag = firstColumn && record?.[ouColumn?.dataIndex] === validOUIds?.[0];
          const foundColor: any = levelopsTableColumns?.find((clm: any) => clm.title === column.title + "_color_code");
          if (typeof item === "string" && validMarkdownLink(item)) {
            let { title, url, isValidUrl } = extractLinkAndTitleFromMarkdownUrl(item);
            if (isValidUrl) {
              return {
                props: {
                  style: {
                    background: `${COLOR_CODES?.[record[foundColor?.dataIndex]?.trim()?.toLowerCase()] || "#FFF"}`
                  }
                },
                children: (
                  <div>
                    <a
                      href={url}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ color: "var(--link-and-actions)" }}>
                      {title}
                    </a>
                  </div>
                )
              };
            }
          }
          return {
            props: {
              style: { background: `${COLOR_CODES?.[record[foundColor?.dataIndex]?.trim()?.toLowerCase()] || "#FFF"}` }
            },
            children: (
              <div
                style={{ width: width }}
                className={`${showOuTag ? "custom-ou-cell" : "custom-cell"} ${
                  firstColumn && !showOuTag ? "ou-column" : ""
                }`}>
                <span className={`flex custom-tooltip-wrapper`}>
                  <Tooltip placement="top" title={item}>
                    <span
                      style={{
                        display: "block",
                        width: firstColumn && showOuTag ? width - 120 : width,
                        whiteSpace: "nowrap",
                        overflow: "hidden",
                        textOverflow: "ellipsis"
                      }}>
                      {item}
                    </span>
                  </Tooltip>
                </span>
                {showOuTag ? <span className="current-ou">CURRENT COLLECTION</span> : <></>}
              </div>
            )
          };
        }
      };
    });
  }, [columns, parentWidth, COLOR_CODES, validOUIds, ouColumn, page]);

  const getRange = (page: number) => {
    const high = 10 * page;
    const low = high - 9;
    return { high, low };
  };

  const getOUBasedData = useMemo(() => {
    let selectedOu: any = [];
    let data: any = sortedData;
    const ouCol: any = levelopsTableColumns.find(col => col?.key?.toLowerCase() === "ou_id");
    if (showOUSpecificData) {
      if (ouCol) {
        data = sortedData?.filter((row: any) => {
          const rowOUId = get(row, [ouCol.dataIndex]);
          return validOUIds?.includes(rowOUId);
        });
      }
    }
    if (!sortingConfig) {
      const filteredRows = data?.reduce((acc: any, current: any) => {
        const rowOUId = current[ouCol?.dataIndex];
        if (rowOUId === validOUIds?.[0]) {
          selectedOu.push(current);
        } else {
          acc.push(current);
        }
        return acc;
      }, []);
      if (selectedOu?.length) {
        data = [...selectedOu, ...filteredRows];
      }
    }
    return data;
  }, [showOUSpecificData, validOUIds, sortedData, levelopsTableColumns, sortingConfig]);

  const getDatasource = useMemo(() => {
    const { high, low } = getRange(page);
    const data = getOUBasedData.slice(low - 1, high);
    if (data.length < 10) {
      let diff = 10 - data?.length;
      while (diff > 0) {
        // commnet this line because it add blank row to table
        // data.push({});
        diff -= 1;
      }
    }
    return data;
  }, [getOUBasedData, getRange, page]);
  return (
    <span ref={parentRef}>
      <AntTable
        page={page}
        pageSize={10}
        hasCustomPagination
        rowClassName={(record: any) =>
          record?.[ouColumn?.dataIndex || ""] === validOUIds?.[0] ? "first-row propelo-table-row" : "propelo-table-row"
        }
        className="propelo-table-chart"
        showPageSizeOptions={false}
        hideOnSinglePage={false}
        bordered={true}
        scroll={{ x: "max-content" }}
        onPageChange={(page: number) => setPage(page)}
        dataSource={getDatasource}
        columns={mappedColumns}
        totalRecords={getOUBasedData?.length}
      />
      {showLegend && (
        <CustomLegendComponent
          legends={[
            { label: "Elite", color: COLOR_CODES?.elite },
            { label: "High", color: COLOR_CODES?.high },
            { label: "Medium", color: COLOR_CODES?.medium },
            { label: "Low", color: COLOR_CODES?.low }
          ]}
        />
      )}
    </span>
  );
};

export default TableChartComponent;
