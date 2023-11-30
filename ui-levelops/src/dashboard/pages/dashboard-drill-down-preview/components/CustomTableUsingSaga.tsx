import widgetConstants from "dashboard/constants/widgetConstants";
import { get } from "lodash";
import React, { useEffect, useState, useRef, useMemo } from "react";
import { useDispatch, useSelector } from "react-redux";
import { jiraSalesforceZendeskStagesWidgets } from "reduxConfigs/actions/restapi";
import { getCustomTableUsingSagaSelector } from "reduxConfigs/selectors/custom_table_using_saga.selector";
import { Badge, Button, Card, Tooltip } from "antd";
import { v1 as uuid } from "uuid";
import { convertChildKeysToSiblingKeys } from "shared-resources/containers/widget-api-wrapper/helper";
import { AntTable, AntText, SvgIcon } from "shared-resources/components";
import { mapColumnsWithInfo } from "dashboard/helpers/mapColumnsWithInfo.helper";
import DrillDownFilterContent from "shared-resources/containers/server-paginated-table/components/drilldown-filter-content/drilldown-filter-content";

interface CustomTableUsingSagaProps {
  filters: any;
  report: string;
  slicedColumns: boolean;
  drilldownHeaderProps?: any;
}

const CustomTableUsingSaga: React.FC<CustomTableUsingSagaProps> = (props: CustomTableUsingSagaProps) => {
  const { report, filters, slicedColumns, drilldownHeaderProps } = props;
  const { title, type, showTitle, onOpenReport } = drilldownHeaderProps || {};

  const [tableData, setTableData] = useState<any[]>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const [page, setPage] = useState<number>(1);
  const [pageSize, setPageSize] = useState<number>(10);
  const dataId = useRef<string>("");

  const getWidgetConstant = (key: string) => {
    return get(widgetConstants, [report, key], undefined);
  };

  const uri = getWidgetConstant("uri");

  const dispatch = useDispatch();
  const restState = useSelector(state => getCustomTableUsingSagaSelector(state, uri, dataId.current));

  useEffect(() => {
    setTableData([]);
    if (["jira_salesforce_aggs_list_salesforce", "jira_zendesk_aggs_list_zendesk"].includes(uri)) {
      const id = uuid();
      const newFilters = convertChildKeysToSiblingKeys(filters, "filter", ["sort"]);
      dispatch(jiraSalesforceZendeskStagesWidgets(uri, "list", newFilters, null, id));
      dataId.current = id;
      setLoading(true);
    }
  }, [report]); // eslint-disable-line react-hooks/exhaustive-deps

  useEffect(() => {
    const loading = get(restState, ["loading"], true);
    const error = get(restState, ["error"], true);
    if (!loading && !error) {
      const records = get(restState, ["data", "records"], []);
      setTableData(records);
      setLoading(false);
    }
  }, [restState]);

  const getDatasource = (curPage: number) => {
    let curData = tableData;
    const { high, low } = getRange(curPage);
    return curData.slice(low - 1, high);
  };

  const onPageSizeChange = (pageSize: number) => {
    setPageSize(pageSize);
    setPage(1);
  };

  const getRange = (page: number) => {
    const high = pageSize * page;
    const low = high - pageSize + 1;
    return { high, low };
  };

  const getColumns = () => {
    const columns = getWidgetConstant("columns");
    return columns ? (slicedColumns ? columns.slice(0, Math.min(columns.length, 4)) : columns) : [];
  };

  const mappedColumns = () => {
    const columns = getColumns();
    const columnsWithInfo = get(widgetConstants, [report, "drilldown", "columnsWithInfo"], undefined);
    return mapColumnsWithInfo(columns, columnsWithInfo);
  };

  const cardTitle = useMemo(() => {
    if (!drilldownHeaderProps || !Object.keys(drilldownHeaderProps || {}).length) {
      return null;
    }

    return (
      <div className="flex align-center">
        <div style={{ marginRight: ".3rem" }}>Drilldown Preview</div>
        <div style={{ marginRight: ".3rem" }}>
          <Badge style={{ backgroundColor: "var(--harness-blue)" }} count={tableData?.length || 0} />
        </div>
      </div>
    );
  }, [tableData, drilldownHeaderProps]);

  const cardActions = useMemo(() => {
    if (!drilldownHeaderProps || !Object.keys(drilldownHeaderProps || {}).length) {
      return null;
    }
    return <DrillDownFilterContent drilldownHeaderProps={drilldownHeaderProps} />;
  }, [drilldownHeaderProps]);

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
        loading={loading}
        columns={mappedColumns()}
        totalRecords={tableData.length}
      />
    </Card>
  );
};

export default CustomTableUsingSaga;
