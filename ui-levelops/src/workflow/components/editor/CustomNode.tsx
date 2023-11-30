import * as React from "react";
import { INodeDefaultProps } from "@mrblenny/react-flow-chart";
import "../../pages/editor/workflow-editor.style.scss";

export const NodeCustom = React.forwardRef(
  ({ node, children, ...otherProps }: INodeDefaultProps, ref: React.Ref<HTMLDivElement>) => {
    let nodeClassName = "workflow-editor__node";
    //let absClassName = "workflow-editor__node__absolute";
    const name = node.properties.name || node.properties.title;
    if (node.type) {
      switch (node.type) {
        case "start":
          nodeClassName = nodeClassName.concat("__start");
          break;
        case "condition":
          nodeClassName = nodeClassName.concat("__condition");
          break;
        case "end":
          nodeClassName = nodeClassName.concat("__end");
          break;
        case "action":
          nodeClassName = nodeClassName.concat("__action");
          break;
        case "wait":
          nodeClassName = nodeClassName.concat("__wait");
          break;
        default:
          break;
      }
    }
    return (
      <div ref={ref} {...otherProps} className={`workflow-editor workflow-editor__node ${nodeClassName}`}>
        {children}
      </div>
    );
  }
);
