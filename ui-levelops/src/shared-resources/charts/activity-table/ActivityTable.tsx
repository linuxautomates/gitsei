import React, { useContext, useMemo, useState } from "react";
import HtmlTable, { HtmlTableColumnProp } from "shared-resources/components/html-table/HtmlTable";
import { ACTIVITY_COLORS, PR_STATUS } from "./helper";
import NewLegendComponent from "../jira-effort-allocation-chart/components/LegendComponent/EffortInvestmentLegend";
import "./styles/active-table.style.scss";
import FirstColumnTitle from "./FirstColumnTitle";
import { AntPaginationComponent as AntPagination } from "shared-resources/components/ant-pagination/ant-pagination.component";
import { AntTextComponent as AntText } from "shared-resources/components/ant-text/ant-text.component";
import { SvgIconComponent as SvgIcon } from "shared-resources/components/svg-icon/svg-icon.component";
import { WidgetFilterContext } from "dashboard/pages/context";

const ActivityTable = (props: any) => {
  const [showName, setShowName] = useState<boolean>(false);
  const { data, columns, across, apisMetaData, id } = props;
  const { setFilters, filters } = useContext(WidgetFilterContext);
  const [legendFilters, setlegendFilters] = useState(
    Object.values(PR_STATUS).reduce((acc: any, legend: string) => ({ [legend]: true, ...acc }), {})
  );
  const selectedLegendsCount = Object.keys(legendFilters).reduce((acc, legend) => {
    acc += legendFilters[legend] ? 1 : 0;
    return acc;
  }, 0);

  const filteredData = useMemo(() => {
    let finalData: Array<any> = [];
    if (data) {
      Object.keys(data).forEach(groupKey => {
        finalData = finalData.concat(
          data[groupKey].filter((row: any) => {
            return legendFilters[row.type];
          })
        );
      });
    }
    return finalData;
  }, [data, legendFilters]);

  const getColumns = useMemo<Array<HtmlTableColumnProp>>(() => {
    const updatedColumns = columns?.map((column: any) => {
      if (column.key === "name") {
        return {
          ...column,
          hideData: !across?.toUpperCase().includes("REPO") && !showName,
          title: <FirstColumnTitle showName={showName} setShowName={setShowName} viewBy={across} />
        };
      }
      return column;
    });
    return updatedColumns;
  }, [columns, showName]);

  const onPagnationChange = (page: number) => {
    setFilters(id, {
      // @ts-ignore
      ...filters[id],
      page: page - 1
    });
  };

  const metadata = apisMetaData && apisMetaData[id];
  return (
    <div className="active-table">
      {metadata?.total_count > 25 && (
        <div className={"flex pl-10"}>
          <SvgIcon className={"pr-10"} icon="warning" />
          <AntText type={"secondary"}>Showing the first 25 results for the selected Collections and filters.</AntText>
        </div>
      )}
      <div className="activity-html-table">
        <HtmlTable columns={getColumns} dataSource={filteredData} firstColumnSpan={selectedLegendsCount} />
        {metadata && (
          <AntPagination
            pageSize={5}
            current={metadata.page + 1}
            total={metadata.total_count > 25 ? 25 : metadata.total_count}
            showPageSizeOptions={false}
            onPageChange={onPagnationChange}
          />
        )}
      </div>
      <NewLegendComponent
        filters={legendFilters}
        setFilters={setlegendFilters}
        colors={ACTIVITY_COLORS}
        wordToCapitalize="PR"
      />
    </div>
  );
};

export default ActivityTable;
