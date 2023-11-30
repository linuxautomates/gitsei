import { ColumnProps } from "antd/lib/table";
import React from "react";
import { useDispatch } from "react-redux";
import { widgetDrilldownColumnsUpdate } from "reduxConfigs/actions/restapi";
import ColumnSelector from "shared-resources/components/column-selector/ColumnSelector";

interface DrilldownColumnSelecorProps {
  availableColumns: Array<{ title: string; dataIndex: string }>;
  visibleColumns: Array<ColumnProps<any>>;
  widgetId?: string;
  closeDropdown: () => void;
  defaultColumns: Array<ColumnProps<any>>;
  onColumnsChange?: (savedColumns: Array<string>) => void;
}

const DrilldownColumnSelecor = (props: DrilldownColumnSelecorProps) => {
  const { widgetId, availableColumns, closeDropdown, defaultColumns, onColumnsChange, visibleColumns } = props;

  const dispatch = useDispatch();
  const saveHandler = (selectedColumns: Array<string>) => {
    if (widgetId) {
      dispatch(widgetDrilldownColumnsUpdate(widgetId, selectedColumns));
    }
    if (onColumnsChange) {
      onColumnsChange(selectedColumns);
    }
    closeDropdown();
  };

  return (
    <ColumnSelector
      saveSelectedColumns={saveHandler}
      availableColumns={availableColumns}
      visibleColumns={visibleColumns}
      defaultColumns={defaultColumns}
    />
  );
};

export default DrilldownColumnSelecor;
