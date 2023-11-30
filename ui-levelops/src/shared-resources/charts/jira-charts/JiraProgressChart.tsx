import React, { useCallback, useMemo, useState } from "react";
import { AntPagination, AntTable } from "shared-resources/components";
import { JiraProgressChartProps } from "../chart-types";
import { JiraProgressTableConfig } from "./jira-progress.table-config";
import "./jira-progress-chart.scss";
import { cloneDeep, get } from "lodash";
import widgetConstants from "dashboard/constants/widgetConstants";
import { REPORT_HEADER_INFO, WORKITEM_PARENT_KEY } from "dashboard/reports/azure/program-progress-report/constant";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { RestWidget } from "classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { ColumnProps } from "antd/lib/table";
import { SetSortConfigFuncType } from "model/report/azure/program-progress-report/program-progress-report.model";
import { numberSortingComparator } from "dashboard/graph-filters/components/sort.helper";
import { AVAILABLE_COLUMNS, DEFAULT_COLUMNS } from "dashboard/constants/filter-key.mapping";

const LIMIT_PER_PAGE = 6;

const JiraProgressChart: React.FC<JiraProgressChartProps> = (props: JiraProgressChartProps) => {
  let { data, across, onClick, reportType, headerData, widgetId } = props;
  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const [currentIndex, setCurrentIndex] = useState<number>(0);
  const [sortingConfig, setSortingConfig] = useState<{ sortOrder: string; key: string }>();
  const onChange = (page: number) => setCurrentIndex(page - 1);

  const sortedData = useMemo(() => {
    const nData = cloneDeep(data);
    if (sortingConfig)
      nData.sort(numberSortingComparator(sortingConfig?.key, sortingConfig?.sortOrder === "ascend" ? "asc" : "desc"));
    return nData;
  }, [sortingConfig, data]);

  const ReportHeader = useMemo(() => {
    let ReportHeaderInfo = get(widgetConstants, [reportType as any, REPORT_HEADER_INFO], undefined);
    if (typeof ReportHeaderInfo === "function" && headerData?.length) {
      ReportHeaderInfo = ReportHeaderInfo({ reportHeaderInfoData: headerData });
    }
    return ReportHeaderInfo;
  }, [reportType]);

  const range = useMemo(() => {
    if (!(sortedData || []).length) return { high: 0, low: 0 };
    const page = currentIndex + 1;
    const high = LIMIT_PER_PAGE * page;
    const low = high - LIMIT_PER_PAGE + 1;
    return { high: Math.min(high, sortedData.length), low };
  }, [currentIndex, sortedData]);

  const getFiltereddata = useMemo(() => {
    const { high, low } = range;
    if (low === 0) return [];
    return sortedData.slice(low - 1, high);
  }, [currentIndex, range]);

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

  const getColumns = useMemo(() => {
    if (across === "ticket_category") {
      return JiraProgressTableConfig.filter(config => config.dataIndex !== "priority");
    }
    if (across === WORKITEM_PARENT_KEY || reportType === "azure_program_progress_report") {
      const selected_columns = get(widget, ["metadata", "selected_columns"], undefined);
      /**  This function returns report chart columns of type Array<ColumnProps<any>>.
       * It takes parameters and uses them to create columns array
       */
      const availableColumnsFunc: (setSortConfig: SetSortConfigFuncType) => Array<ColumnProps<any>> | undefined = get(
        widgetConstants,
        [reportType as string, "available_columns_func"]
      );

      let availabeColumns: Array<ColumnProps<any>> = get(
        widgetConstants,
        [reportType as string, AVAILABLE_COLUMNS],
        []
      );
      if (!!availableColumnsFunc) {
        availabeColumns = availableColumnsFunc(handleSetSortConfig) ?? [];
      }

      if (!selected_columns) {
        const defaultColumns: Array<ColumnProps<any>> = get(
          widgetConstants,
          [reportType as string, DEFAULT_COLUMNS],
          []
        );
        const defaultColumnKeys = defaultColumns.map(c => c.dataIndex);
        return (availabeColumns || []).filter(cl => defaultColumnKeys.includes(cl?.dataIndex ?? ""));
      }

      return (availabeColumns || []).filter((column: any) => (selected_columns || []).includes(column?.dataIndex));
    }
    return JiraProgressTableConfig;
  }, [across, widget]);

  const handleRowClick = useCallback(
    (record: any) => {
      const onChartClick = get(widgetConstants, [reportType as string, "onChartClickPayload"], undefined);
      if (onChartClick) {
        onClick && onClick(onChartClick(record));
      } else {
        onClick && onClick(record?.id);
      }
    },
    [onClick, reportType]
  );

  const setRowProps = useCallback(
    (record: any, index: number) => ({
      onClick: (e: any) => {
        handleRowClick(record);
      }
    }),
    [handleRowClick]
  );

  return (
    <>
      {ReportHeader ? ReportHeader : null}
      <div className="jira_progress_chart_container">
        <AntTable
          className="progress-programm-chart"
          dataSource={getFiltereddata}
          columns={getColumns}
          pagination={false}
          onRow={setRowProps}
          scroll={{ x: "fit-content", scrollToFirstRowOnChange: true }}
        />
        <div className="progress-pagination-div">
          <AntPagination
            pageSize={LIMIT_PER_PAGE}
            current={currentIndex + 1}
            onPageChange={onChange}
            total={(data || []).length}
            showPageSizeOptions={false}
          />
        </div>
      </div>
    </>
  );
};

export default JiraProgressChart;
