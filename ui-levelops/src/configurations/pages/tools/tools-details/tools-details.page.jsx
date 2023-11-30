import React from "react";
import { connect } from "react-redux";
import ErrorWrapper from "hoc/errorWrapper";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import queryString from "query-string";
import Loader from "components/Loader/Loader";
import { AntCard, SvgIcon, AntTitle, AntText } from "shared-resources/components";
import { getError, getLoading } from "../../../../utils/loadingUtils";

export class ToolsDetailsPage extends React.Component {
  constructor(props) {
    super(props);
    let toolId = undefined;
    let loading = false;
    if (this.props.location.pathname.includes("tools-page")) {
      const values = queryString.parse(this.props.location.search);
      toolId = values.tool;
      loading = true;
      this.props.toolsGet(toolId);
    }
    this.state = {
      loading: loading,
      tool_id: toolId,
      create_loading: false,
      created: false
    };
    this.handleDownload = this.handleDownload.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (
      !getLoading(props.rest_api, "tools", "get", state.tool_id) &&
      !getError(props.rest_api, "tools", "get", state.tool_id)
    ) {
      return {
        ...state,
        loading: false
      };
    }
  }

  handleDownload(e) {
    e.preventDefault();
    const tool = this.props.rest_api.tools.get[this.state.tool_id].data;
    this.props.filesGet(tool.file_id);
  }

  render() {
    if (this.state.loading) {
      return <Loader />;
    }
    const tool = this.props.rest_api.tools.get[this.state.tool_id].data;
    console.log(tool);
    return (
      <div className={`flex direction-column align-center`}>
        <div style={{ flexBasis: "100%", width: "60%", alignContent: "center" }}>
          <AntCard>
            <div className={`flex direction-column justify-start`}>
              <div className={`flex direction-row justify-start`} style={{ width: "100%" }}>
                <div style={{ flexBasis: "10%" }}>
                  <SvgIcon style={{ width: "2.4rem", height: "2.4rem" }} icon={"slack"} color={"primary"} />
                </div>
                <div className={`flex direction-column`} style={{ width: "100%" }}>
                  <AntTitle level={3}>{tool.name}</AntTitle>
                  <AntText>{tool.description}</AntText>
                </div>
                <a href={"#"} onClick={this.handleDownload}>
                  DOWNLOAD
                </a>
              </div>
              <div>
                <hr />
              </div>
              <AntText>{tool.description}</AntText>
            </div>
          </AntCard>
        </div>
      </div>
    );
  }
}

export default ErrorWrapper(connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(ToolsDetailsPage));
