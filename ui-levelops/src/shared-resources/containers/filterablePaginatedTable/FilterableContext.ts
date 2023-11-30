import React from "react";

export const FilterableContext = React.createContext({
  onCellClick: (record: any, columnName: any) => {}
});