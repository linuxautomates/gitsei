import React, { useCallback, useContext, useMemo, useState } from "react";
import { DevProductivityTableChartProps } from "../chart-types";
import { Icon, Tooltip } from "antd";
import { AntBadge, AntPagination, AntTable, AntText, EmptyWidget } from "../../components";
import { get } from "lodash";
import { WidgetFilterContext } from "../../../dashboard/pages/context";
import "./table-chart.component.scss";
import { findRatingByScore } from "../../../dashboard/helpers/devProductivityRating.helper";
import { ratingToLegendColorMapping } from "../../../dashboard/constants/devProductivity.constant";
import NewLegendComponent from "../jira-effort-allocation-chart/components/LegendComponent/EffortInvestmentLegend";
import { useParams } from "react-router-dom";
import { ProjectPathProps } from "classes/routeInterface";
import { useParentProvider } from "contexts/ParentProvider";
import { removeLastSlash } from "utils/regexUtils";

const DevProdTableChartComponent: React.FC<DevProductivityTableChartProps> = (
  props: DevProductivityTableChartProps
) => {
  const {
    data,
    columns,
    size,
    apisMetaData,
    id,
    modifiedColumnInfo,
    showNameButton,
    onRowClick,
    interval,
    ou_id,
    dashboardId,
    ou_uuid,
    isDemo,
    onRowDemoTableClick
  } = props;
  const { setFilters, filters } = useContext(WidgetFilterContext);
  const [showName, setShowName] = useState<boolean>(false);
  const [legendFilters, setLegendFilters] = useState<Array<string>>(
    Object.keys(ratingToLegendColorMapping).reduce((acc: any, filter: string) => ({ ...acc, [filter]: true }), {})
  );
  const projectParams = useParams<ProjectPathProps>();
  const onSortChange = useCallback(
    (pagination: any, filter: any, sorter: any, extra: any) => {
      const sortEntry = { id: sorter.field, desc: sorter.order === "descend" };
      setFilters(props.id as string, { ...(filters as any)[id], sort: sortEntry });
    },
    [props.id]
  );
  const {
    utils: { getLocationPathName }
  } = useParentProvider();

  const onPagnationChange = useCallback(
    (page: number) => {
      setFilters(id, { ...(filters as any)[id], page: page - 1 });
    },
    [id]
  );

  const totalRecords = useMemo(() => {
    const metadata = get(apisMetaData, [id], {});
    return metadata?.total_count;
  }, [apisMetaData, id]);

  const getColumns = useMemo(() => {
    if (columns && columns?.length === 0) {
      return [];
    }
    const modifiedColumns = columns.map((column: any) => {
      if (column.key === modifiedColumnInfo?.key) {
        const title = (
          <div key={column.key}>
            <Tooltip title={"name"}>{modifiedColumnInfo?.name || "Name"}</Tooltip>
            <AntBadge
              style={{ margin: "0 0.5rem", backgroundColor: "var(--harness-blue)" }}
              overflowCount={1000}
              count={totalRecords}
            />
            {showNameButton && (
              <AntText className="engineer-profile-card-hide" onClick={(data: any) => setShowName(!showName)}>
                {showName ? "Hide " : "Show "}
                <Icon type={showName ? "eye-invisible" : "eye"} />
              </AntText>
            )}
          </div>
        );
        return {
          ...column,
          title: title
        };
      }
      return column;
    });
    return modifiedColumns;
  }, [columns, showName, modifiedColumnInfo]);

  const pageSize = useMemo(() => {
    const metadata = get(apisMetaData, [id], {});
    return metadata?.page_size;
  }, [apisMetaData, id]);

  const paginationCondition = useMemo(() => {
    return totalRecords > pageSize;
  }, [totalRecords, pageSize]);

  const page = useMemo(() => {
    const metadata = get(apisMetaData, [id], {});
    return metadata?.page + 1;
  }, [apisMetaData, id]);

  const scoreColumns = useMemo(
    () => columns.map((column: any) => column.dataIndex).filter((column: string) => !column.includes("name")),
    [columns]
  );

  const filteredData = useMemo(() => {
    return data.map((item: any) => {
      if (isDemo) item.ou_uuid = ou_uuid;
      const filteredItem = { ...item };
      scoreColumns.forEach((columnKey: string) => {
        filteredItem[columnKey] = legendFilters[findRatingByScore(item[columnKey]) as any]
          ? item[columnKey]
          : { hiddenValue: item[columnKey] };
      });

      if (showName || !showNameButton) {
        return filteredItem;
      }
      return {
        ...filteredItem,
        [modifiedColumnInfo?.key || "name"]: {
          ["name"]: filteredItem[modifiedColumnInfo?.key || "name"],
          hidden: true
        }
      };
    });
  }, [showName, data, modifiedColumnInfo, legendFilters]);

  if ((data || []).length === 0) {
    const message = get(apisMetaData, [id, "message"], "No Data");
    return <EmptyWidget description={message} />;
  }
  const handleRowClick = (record: any, index: number, event: Event) => {
    if (isDemo) {
      onRowDemoTableClick && onRowDemoTableClick(id, record, dashboardId as string, index, event, interval, ou_id);
      return;
    }
    if (onRowClick) {
      onRowClick(projectParams, record, index, event, interval, ou_id, removeLastSlash(getLocationPathName?.()));
    }
  };
  return (
    <div className="dev-prod-table-chart">
      <div className={"dev-table-chart"}>
        <AntTable
          size={size || "default"}
          dataSource={(filteredData || []).slice(0, 10)}
          columns={getColumns}
          pagination={false}
          onChange={onSortChange}
          onRowClick={handleRowClick}
        />
      </div>
      <div>
        {paginationCondition && (
          <AntPagination
            pageSize={pageSize}
            current={page}
            total={totalRecords}
            showPageSizeOptions={false}
            onPageChange={onPagnationChange}
          />
        )}
        <NewLegendComponent filters={legendFilters} setFilters={setLegendFilters} colors={ratingToLegendColorMapping} />
      </div>
    </div>
  );
};

export default DevProdTableChartComponent;
