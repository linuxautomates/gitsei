import { Badge, Button, Dropdown, Icon, Tooltip } from "antd";
import { RestWidget } from "classes/RestDashboards";
import widgetConstants from "dashboard/constants/widgetConstants";
import { AVAILABLE_COLUMNS, DEFAULT_COLUMNS } from "dashboard/constants/filter-key.mapping";
import { get } from "lodash";
import React, { useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { widgetSelectedColumnsUpdate } from "reduxConfigs/actions/restapi";
import { SvgIcon } from "shared-resources/components";
import ColumnSelector from "shared-resources/components/column-selector/ColumnSelector";
import widget from "./widget";

interface WidgetColumnSelecorProps {
  widget: RestWidget;
}

const WidgetColumnSelecor = (props: WidgetColumnSelecorProps) => {
  const { type, id, selected_columns } = props.widget;
  const [showColumnList, setShowColumnList] = useState<boolean>(false);
  const dispatch = useDispatch();

  const availableColumns = useMemo(() => get(widgetConstants, [type, AVAILABLE_COLUMNS], []), [type]);

  const defaultColumns = useMemo(() => get(widgetConstants, [type, DEFAULT_COLUMNS], []), [type]);

  const visibleColumns = useMemo(() => {
    let columnsToDisplay = selected_columns;
    if (!columnsToDisplay) {
      columnsToDisplay = defaultColumns.reduce((acc: Array<string>, column: any) => {
        acc.push(column.dataIndex);
        return acc;
      }, []);
    }
    return availableColumns
      .filter((column: any) => columnsToDisplay?.includes(column.dataIndex))
      .map((column: any) => ({
        dataIndex: column.dataIndex,
        title: column.title
      }));
  }, [selected_columns, availableColumns]);

  const saveHandler = (selectedColumns: Array<string>) => {
    if (id) {
      dispatch(widgetSelectedColumnsUpdate(id, selectedColumns));
    }
    setShowColumnList(false);
  };

  return (
    <Dropdown
      overlay={
        <ColumnSelector
          saveSelectedColumns={saveHandler}
          availableColumns={availableColumns}
          visibleColumns={visibleColumns}
          defaultColumns={defaultColumns}
        />
      }
      trigger={["click"]}
      placement="bottomRight"
      visible={showColumnList}
      onVisibleChange={setShowColumnList}>
      <Tooltip title="Select Columns">
        <Button className="dev-prod-widget-icon">
          <div className="icon-wrapper">
            <span>Columns</span>
            <Badge count={selected_columns?.length || defaultColumns?.length} showZero />
            <Icon type={showColumnList ? "up" : "down"} />
          </div>
        </Button>
      </Tooltip>
    </Dropdown>
  );
};

export default WidgetColumnSelecor;
