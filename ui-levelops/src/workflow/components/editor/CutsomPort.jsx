import React from "react";
import "workflow/pages/editor/workflow-editor.style.scss";

export class CustomPort extends React.PureComponent {
  render() {
    const { port } = this.props;
    let className = "workflow-editor__port-inner__io";
    switch (port.properties.action) {
      case "output":
        className = "workflow-editor__port-inner__io";
        break;
      case "input":
        className = "workflow-editor__port-inner__input";
        break;
      case "fail":
        className = "workflow-editor__port-inner__left";
        break;
      case "success":
        className = "workflow-editor__port-inner__right";
        break;
      default:
        className = "workflow-editor__port-inner__io";
        break;
    }
    return (
      <div className={`workflow-editor workflow-editor__port`}>
        <div className={`workflow-editor workflow-editor__port-inner ${className}`}>
          <span className={"circle"} />
          {/*{*/}
          {/*  port.properties.action === "output" &&*/}
          {/*  <Icon type="down-circle" theme="filled" style={{fontSize: "26px"}}/>*/}
          {/*}*/}
          {/*{*/}
          {/*  port.properties.action === "input" &&*/}
          {/*  <Icon type="up-circle" theme="filled" style={{fontSize: "26px"}}/>*/}
          {/*}*/}

          {/*{port.properties.action === "input" && <span className="workflow-editor__port-inner--dot"></span>}*/}
        </div>
      </div>
    );
  }
}
