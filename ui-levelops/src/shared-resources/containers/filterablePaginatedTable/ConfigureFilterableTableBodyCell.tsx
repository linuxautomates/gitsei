import React, { useContext } from "react";
import { FilterableContext } from "./FilterableContext";

interface ConfigureFilterableTableBodyCellProps {
  isNumeric?: boolean;
  render?: any;
  value?: any;
  key?: any;
  type?: string;
  columnName?: string;
  record: any;
  isCellClickable?: boolean;
  children: any;
}
const ConfigureFilterableTableBodyCell = (props: ConfigureFilterableTableBodyCellProps) => {
  const { onCellClick } = useContext(FilterableContext);
  const onClick = () => onCellClick(props.record, props.columnName);
  return <td onClick={props.isCellClickable ? onClick : undefined}>{props.children}</td>;
};

export default ConfigureFilterableTableBodyCell;
