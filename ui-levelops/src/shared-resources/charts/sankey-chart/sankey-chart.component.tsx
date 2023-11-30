import React from "react";
// @ts-ignore
import Tree from "react-tree-graph";
import { ResponsiveContainer } from "recharts";
import { SankeyChartProps } from "../chart-types";
import { EmptyWidget } from "../../components";

const SankeyChartComponent: React.FC<SankeyChartProps> = (props: SankeyChartProps) => {
  const { previewOnly } = props;

  function onClick(event: any, nodeKey: any) {
    if (nodeKey && !previewOnly) {
      props.onClick(nodeKey);
    }
  }

  if ((props.data || []).length === 0) {
    return <EmptyWidget />;
  }

  // @ts-ignore
  return (
    <ResponsiveContainer>
      <Tree
        data={props.data}
        labelProp={previewOnly ? "" : "label"}
        nodeShape={"rect"}
        animate={true}
        duration={500}
        gProps={{ onClick: onClick, style: { cursor: "pointer" } }}
        margins={{ bottom: 5, left: 10, right: 200, top: 5 }}
      />
    </ResponsiveContainer>
  );
};

export default SankeyChartComponent;
