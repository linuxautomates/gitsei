import React, { useCallback, useMemo, useState, useEffect } from "react";
import { Row, Radio, Popover } from "antd";
import { cloneDeep, forEach, get, isEmpty, set } from "lodash";
import { useDispatch } from "react-redux";
import { EngineerTableProps } from "../chart-types";
import { EffortType } from "dashboard/constants/enums/jira-ba-reports.enum";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { RestWidget } from "classes/RestDashboards";
import { widgetUpdate } from "reduxConfigs/actions/restapi";
import { AntInput, AntTable } from "shared-resources/components";
import { engineerTableConfig } from "./engineerTableConfig";
import { basicMappingType, CustomPaginationConfig } from "dashboard/dashboard-types/common-types";
import { ColumnProps } from "antd/lib/table";
import NewLegendComponent from "../jira-effort-allocation-chart/components/LegendComponent/EffortInvestmentLegend";
import { effortInvestmentDonutBarColors } from "../chart-themes";
import { getColorMapping } from "../jira-effort-allocation-chart/helper";
import "./engineerTable.styles.scss";
import StackedProgressBar from "../jira-burndown/Components/StackedProgressBar";
import EffortInvestmentPopoverContent from "../effort-investment-team-chart/components/EffortInvestmentPopoverContent";
import {
  engineerTableGenericSortingHelper,
  engineerTableSortingHelper,
  getAllocationSummarySubColumn,
  getUriUnit
} from "./helper";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";

const defaultValue = { page: 1, pageSize: 10 };

const CATEGORIES_WITHOUT_TOTAL = ["Remaining Allocation"];

const EngineerTable: React.FC<EngineerTableProps> = (props: EngineerTableProps) => {
  const { widgetId, data: effortConfig, onClick } = props;
  const { apidata, categories, categoryColorMapping } = effortConfig;
  console.log(effortConfig, "effortConfig");
  const [searchValue, setSearchvalue] = useState<string>("");
  const dispatch = useDispatch();
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const [pageFilters, setPageFilters] = useState<CustomPaginationConfig>(defaultValue);
  const [legendMapping, setLegendMapping] = useState<basicMappingType<boolean>>({});
  const [sortValue, setSortValue] = useState<
    { column: string; order: "ascend" | "descend" | null | undefined } | undefined
  >();
  const colorMapping = categoryColorMapping ?? getColorMapping(categories, effortInvestmentDonutBarColors);
  const filteredApiData = useMemo(() => {
    if (searchValue.length) {
      return apidata?.filter((item: any) => item?.engineer?.toLowerCase()?.includes(searchValue?.toLowerCase()));
    }
    return apidata;
  }, [searchValue, apidata]);
  useEffect(() => {
    if (isEmpty(legendMapping) && !isEmpty(categories)) {
      const newLegendMapping: basicMappingType<boolean> = {};
      forEach(categories, category => {
        newLegendMapping[category] = true;
      });
      setLegendMapping(newLegendMapping);
    }
  }, [categories]);

  const onCategories = useMemo(() => {
    if (Object.keys(legendMapping).length) {
      let newCategories: string[] = [];
      forEach(categories, category => {
        if (category && legendMapping[category]) {
          newCategories.push(category);
        }
      });
      return newCategories;
    }
    return categories;
  }, [legendMapping, categories]);

  const handleModeChange = useCallback(
    (e: any) => {
      const value = e.target.value;
      if (widget && widget.effort_type !== value) {
        widget.effort_type = value;
        dispatch(
          widgetUpdate(widgetId, {
            metadata: {
              effort_type: value
            }
          })
        );
      }
    },
    [widget]
  );

  const uriUnit = useMemo(() => {
    return getUriUnit(widget);
  }, [widget]);

  const getDataSource = useMemo(() => {
    const _apiData = cloneDeep(filteredApiData);
    if (sortValue && sortValue.order) {
      let column: any,
        subColumn = "",
        targetColumnLabel = "";
      [subColumn, column, targetColumnLabel] = sortValue?.column?.split("_");
      _apiData.sort((a: any, b: any) => {
        // @ts-ignore
        return CATEGORIES_WITHOUT_TOTAL.includes(targetColumnLabel)
          ? engineerTableGenericSortingHelper(a, b, targetColumnLabel)
          : engineerTableSortingHelper(a, b, sortValue.order, targetColumnLabel, subColumn);
      });
    }
    const startIndex = (pageFilters.page - 1) * pageFilters.pageSize;
    const totalItems = (_apiData ?? []).length;
    const endIndex = Math.min(startIndex + pageFilters.pageSize - 1, totalItems - 1);
    return (_apiData ?? []).slice(startIndex, endIndex + 1);
  }, [filteredApiData, pageFilters, sortValue]);

  const engineersColumns = useMemo(() => {
    let columns: ColumnProps<any>[] = [];
    if (onCategories?.length) {
      // optional chaining added was getting errors
      forEach(onCategories ?? [], (category: string, index: number) => {
        const hasTotalCount = !CATEGORIES_WITHOUT_TOTAL.includes(category);

        const newAllocationSummaryColumn = {
          title: category,
          dataIndex: "allocation_summary",
          key: `${index}_${index}_${category}`,
          width: hasTotalCount ? 280 : 240,
          className: "category-header"
        };

        if (hasTotalCount) {
          const childColumns = [
            getAllocationSummarySubColumn(category, uriUnit, index),
            getAllocationSummarySubColumn(category, uriUnit, index, "percentage")
          ];
          set(newAllocationSummaryColumn, "children", childColumns);
        } else {
          set(newAllocationSummaryColumn, "render", (item: basicMappingType<number>) => {
            return (
              <div className="effort-score-text">
                {parseFloat((item?.[category] || "0.00").toString()).toFixed(2) ?? "0.00"} %
              </div>
            );
          });
          set(
            newAllocationSummaryColumn,
            "sorter",
            (mapping1: basicMappingType<number>, mapping2: basicMappingType<number>) => {
              return engineerTableGenericSortingHelper(mapping1, mapping2, category);
            }
          );
        }

        columns.push(newAllocationSummaryColumn);
      });
    }
    forEach(engineerTableConfig, column => {
      if (column?.dataIndex === "category_columns") {
        set(column, ["children"], columns);
      }

      if (column.key === "effort-bar") {
        set(column, ["render"], (item: basicMappingType<string>, record: basicMappingType<any>) => {
          const updatedItem: any = {};
          Object.keys(item).forEach(key => {
            const value = item[key]?.includes("|") ? item[key]?.split("|")?.[1]?.trim() : item[key];
            updatedItem[key] = +value;
          });
          return (
            <Popover
              content={
                <EffortInvestmentPopoverContent
                  assigneeName={record?.engineer ?? ""}
                  payload={item}
                  suffix="%"
                  showTotal={false}
                  showViewReport={false}
                  colorMapping={colorMapping}
                />
              }
              placement="right"
              overlayClassName="engineer-table-popover">
              <div className="stack-container">
                <StackedProgressBar
                  showPopOver={false}
                  records={[updatedItem]}
                  dataKeys={onCategories}
                  mapping={colorMapping}
                  metaData={{
                    radiusFactor: 4,
                    width: 225,
                    height: 50,
                    orientation: "vertical",
                    xAxisType: "number",
                    yAxisType: "category",
                    xHide: true,
                    yHide: true,
                    rectangleHeight: 32
                  }}
                />
              </div>
            </Popover>
          );
        });
      }
    });
    return engineerTableConfig;
  }, [onCategories, legendMapping, colorMapping, uriUnit, widget]);

  return (
    <div className="engineer-table-container">
      <div className="effort-type-switch">
        <Row>
          <Radio.Group
            onChange={handleModeChange}
            value={widget?.effort_type ?? EffortType.ACTIVE_EFFORT}
            buttonStyle="outline">
            <Radio.Button value={EffortType.COMPLETED_EFFORT}>Completed Effort</Radio.Button>
            <Radio.Button value={EffortType.ACTIVE_EFFORT}>Current Allocation</Radio.Button>
          </Radio.Group>
          <span className="uri-unit">By {uriUnit}</span>
        </Row>
        <AntInput
          id={`developer-search`}
          placeholder={"Search Engineer ..."}
          type="search"
          onChange={(e: any) => setSearchvalue(e.target.value)}
          name="engineer-search"
          className="search-engineer"
          value={searchValue}
        />
      </div>
      <Row className="engineer-table-row">
        <AntTable
          rowKey="engineer"
          size="middle"
          scroll={{ x: 1500 }}
          className="engineer-table"
          columns={engineersColumns}
          hasCustomPagination
          dataSource={getDataSource}
          page={pageFilters.page || 1}
          pageSize={pageFilters.pageSize || 10}
          onPageSizeChange={(pageSize: any) => setPageFilters({ ...pageFilters, pageSize, page: 1 })}
          onPageChange={(page: number) => setPageFilters({ ...pageFilters, page })}
          totalRecords={(filteredApiData ?? []).length}
          onRowClick={(record: any) => {
            const onChartClickPayload = getWidgetConstant(props.reportType, ["onChartClickPayload"]);
            if (onChartClickPayload) {
              onClick && onClick(onChartClickPayload({ record }));
            }
          }}
          style={{ cursor: "pointer" }}
          onChange={(_: any, __: any, sorter: any) => {
            setSortValue({ column: sorter.columnKey, order: sorter.order });
          }}
        />
      </Row>
      <Row className="engineer-legend-container">
        <NewLegendComponent
          setFilters={setLegendMapping}
          filters={legendMapping}
          colors={colorMapping}
          data={filteredApiData || []}
        />
      </Row>
    </div>
  );
};

export default EngineerTable;
