import React from "react";
import { Resizable } from "react-resizable";

const PluginResultReportTableCol: React.FC = (props: any) => {
  const { onResize, width } = props;
  return (
    <Resizable
      width={width}
      className={"col-resize"}
      height={0}
      handle={
        <span
          className="react-resizable-handle"
          onClick={e => {
            e.stopPropagation();
          }}
        />
      }
      onResize={onResize}>
      <th id={props.dataIndex}>{props.title}</th>
    </Resizable>
  );
};

export default PluginResultReportTableCol;
