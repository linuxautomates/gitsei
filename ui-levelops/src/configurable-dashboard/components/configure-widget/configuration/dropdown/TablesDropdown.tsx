import React, { useCallback } from "react";

import { CustomSelect } from "../../../../../shared-resources/components";
import { useTables } from "../../../../../custom-hooks";
import { RestConfigTable } from "../../../../../classes/RestConfigTable";

interface TablesDropdownProps {
  selectedTable: string;
  onTableChange: (table: RestConfigTable) => void;
}

const TablesDropdown: React.FC<TablesDropdownProps> = ({ selectedTable, onTableChange }) => {
  const [tableListLoading, tableListData] = useTables(
    "list",
    undefined,
    undefined,
    undefined,
    undefined,
    "table-dd-list"
  );

  const handleTableChange = useCallback(
    (tableId: string) => {
      const table = (tableListData || []).find((table: RestConfigTable) => table.id === tableId);
      table && onTableChange(table);
    },
    [tableListData, onTableChange]
  );

  return (
    <CustomSelect
      placeholder="Select a Table From Dropdown"
      valueKey="id"
      labelKey="name"
      labelCase="none"
      mode="default"
      createOption={false}
      onChange={handleTableChange}
      value={selectedTable}
      options={tableListData || []}
      loading={tableListLoading}
      sortOptions
    />
  );
};

export default React.memo(TablesDropdown);
