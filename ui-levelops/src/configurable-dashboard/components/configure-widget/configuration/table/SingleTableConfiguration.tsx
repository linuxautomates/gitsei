import React, { useCallback } from "react";
import { get } from "lodash";
import { useDispatch, useSelector } from "react-redux";
import { useHistory } from "react-router-dom";

import "./SingleTableConfiguration.scss";
import { AntText } from "shared-resources/components";
import { RestWidget } from "classes/RestDashboards";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import TablesDropdown from "./../dropdown/TablesDropdown";
import { widgetUpdate } from "reduxConfigs/actions/restapi";
import TableConfigurationTabs from "./../tabs/TableConfigurationTabs";
import { WidgetType } from "../../../../../dashboard/helpers/helper";
import { getWidget } from "reduxConfigs/selectors/widgetSelector";
import { RestConfigTable } from "../../../../../classes/RestConfigTable";
import { configTablesListState } from "reduxConfigs/selectors/restapiSelector";
import { WebRoutes } from "../../../../../routes/WebRoutes";
import widgetConstants from "dashboard/constants/widgetConstants";

interface TableConfigurationProps {
  widgetId: string;
  dashboardId: string;
}

const SingleTableConfiguration: React.FC<TableConfigurationProps> = ({ widgetId, dashboardId }) => {
  const dispatch = useDispatch();
  const history = useHistory();

  const widget: RestWidget = useParamSelector(getWidget, { widget_id: widgetId });
  const tableListState = useSelector(configTablesListState);

  const isStat = widget.widget_type === WidgetType.CONFIGURE_WIDGET_STATS;

  const selectedTableId = get(widget, ["metadata", "tableId"], undefined);
  const allTables: RestConfigTable[] = get(tableListState, ["table-dd-list", "data", "records"], []);
  const selectedTable: RestConfigTable | undefined = (allTables || []).find(
    (table: RestConfigTable) => table.id === selectedTableId
  );

  const handleTableChange = useCallback(
    (table: RestConfigTable) => {
      let data = widget;
      data.metadata = {
        ...(data.metadata || {}),
        tableId: table.id,
        xAxis: "",
        yAxis: isStat ? [{ key: "" }] : [],
        yUnit: "",
        groupBy: false
      };
      const defaultQuery = get(widgetConstants, [widget?.type, "default_query"], {});
      data.query = { ...defaultQuery };
      dispatch(widgetUpdate(widgetId, { ...data.json }));
    },
    [widget]
  );

  const redirectToCreateTable = () => {
    history.push(WebRoutes.table.create);
  };

  return (
    <div>
      <div className="table-dropdown-container">
        <TablesDropdown selectedTable={selectedTableId} onTableChange={handleTableChange} />
        <span className={"note"}>
          Note: You can configure data sources by creating a Table in platform. You can create tables manually or import
          using CSV. Visit
          <a className={"link"} onClick={redirectToCreateTable}>
            this link
          </a>
          to configure tables.
        </span>
      </div>

      <div className="table-label-container mt-3">
        <span>
          <AntText strong className="label">
            Table
          </AntText>
        </span>
        <span>
          <AntText className="table-name">{selectedTable?.name}</AntText>
        </span>
      </div>

      <TableConfigurationTabs widgetId={widgetId} dashboardId={dashboardId} />
    </div>
  );
};

export default SingleTableConfiguration;
