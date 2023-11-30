import React, { useEffect, useState } from "react";
import { useGlobalFilters } from "../../../custom-hooks/useGlobalFilters";
import { genericPaginationData } from "reduxConfigs/selectors/restapiSelector";
import { connect } from "react-redux";
import widgetConstants from "../../../dashboard/constants/widgetConstants";
import ServerPaginatedTable from "../server-paginated-table/server-paginated-table.container";
import { getMappedSortValue } from "../../charts/helper";
import { Button } from "antd";

interface StoreProps {
  paginationState?: any;
}

interface ServerPaginatedTableGraphWrapperProps extends StoreProps {
  uuid: string;
  filters: any;
  globalFilters: any;
  setReload?: (reload: boolean) => void;
  reload: boolean;
  widgetFilters: any;
  uri: string;
  method: string;
  reportType: string;
  onClick: (value: string) => void;
}

const ServerPaginatedTableGraphWrapper: React.FC<ServerPaginatedTableGraphWrapperProps> = (
  props: ServerPaginatedTableGraphWrapperProps
) => {
  const [fetchData, setFetchData] = useState<number>(0);

  const globalFilters = useGlobalFilters(props.globalFilters);

  const filters = props.filters && Object.keys(props.filters).length ? props.filters : {};

  const widgetFilters = props.widgetFilters || {};

  const loading =
    props.paginationState && props.paginationState.loading !== undefined ? props.paginationState.loading : true;

  const getFilters = () => {
    let finalFilters = {
      filter: {
        ...filters,
        ...globalFilters,
        ...widgetFilters
      }
    };
    if (finalFilters.filter.hasOwnProperty("across")) {
      const across = finalFilters.filter.across;
      delete finalFilters.filter["across"];
      finalFilters = {
        ...finalFilters,
        // @ts-ignore
        across
      };
    }
    if (finalFilters.filter.hasOwnProperty("sort")) {
      const sort = finalFilters.filter.sort;
      delete finalFilters.filter["sort"];
      finalFilters = {
        ...finalFilters,
        // @ts-ignore
        sort
      };
    }
    return finalFilters;
  };

  useEffect(() => {
    if (!loading && props.reload) {
      props.setReload && props.setReload(false);
    }
  }, [loading]);

  useEffect(() => {
    setFetchData(state => ++state);
  }, [globalFilters]);

  useEffect(() => {
    if (props.reload) {
      setFetchData(state => ++state);
    }
  }, [props.reload]);

  const getWidgetConstant = (key: any) => {
    if (props.reportType) {
      // @ts-ignore
      return (widgetConstants[props.reportType] as any)[key];
    }
  };

  const getColumns = () => {
    const columns = getWidgetConstant("chart_props").columns || [];
    const filters = getFilters();
    // @ts-ignore
    const sort_value = filters.sort ? filters.sort[0].id || "num_commits" : "num_commits";
    if (columns.length === 0) {
      return [];
    } else if (props.reportType === "scm_files_report") {
      const fixedColumns = columns.slice(0, 2);
      const mappedColumn = columns.find((column: any) => column.key === getMappedSortValue(sort_value));
      if (mappedColumn) {
        return [...fixedColumns, mappedColumn];
      } else return fixedColumns;
    } else {
      return columns;
    }
  };

  const finalFilters = getFilters();

  return (
    <>
      <div style={{ maxHeight: "300px", overflowY: "scroll" }}>
        <ServerPaginatedTable
          uuid={props.uuid}
          showTitle={false}
          showExtra={false}
          uri={props.uri}
          method={props.method}
          derive={false}
          columns={getColumns()}
          moreFilters={finalFilters.filter}
          // @ts-ignore
          across={finalFilters.across && finalFilters.across}
          // @ts-ignore
          sort={finalFilters.sort && finalFilters.sort}
          hasSearch={false}
          hasFilters={false}
          reload={fetchData}
        />
      </div>
      {
        // @ts-ignore
        <div align={"right"}>
          <Button type={"link"} onClick={e => props.onClick("something")}>
            More
          </Button>
        </div>
      }
    </>
  );
};

const mapStateToProps = (state: any, ownProps: any) => ({
  // @ts-ignore
  paginationState: genericPaginationData(state, ownProps)
});

export default connect(mapStateToProps, null)(ServerPaginatedTableGraphWrapper);
