import React from "react";
import { withRouter } from "react-router-dom";
import { connect } from "react-redux";
import { PaginatedGrid } from "shared-resources/helpers";
import { IntegrationIcon, AntText, AntTitle, AntIcon } from "shared-resources/components";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { Trigger } from "./trigger";
import { ICON_PATH } from "shared-resources/components/integration-icons/icon-path.config";
import { get } from "lodash";
import { getBaseUrl } from 'constants/routePaths'

export class TriggersListContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      more_filters: {},
      partial_filters: {},
      reload: 1
    };
    this.buildCard = this.buildCard.bind(this);
    this.handleTriggerClick = this.handleTriggerClick.bind(this);
  }

  handleTriggerClick(record, index) {
    return e => {
      this.props.history.push(`${getBaseUrl()}/propels/propels-editor?trigger=${record.type}`);
    };
  }

  buildCard(record, index) {
    const integrationIcons = ICON_PATH.map(icon => icon.temp_props.type);
    const icon = get(record, ["ui_data", "icon"], "play-circle");

    return (
      <Trigger
        onClick={this.handleTriggerClick(record, index)}
        icon={
          integrationIcons.includes(icon) ? (
            <IntegrationIcon type={record.ui_data.icon} size={"large"} />
          ) : (
            <AntIcon type={icon} style={{ fontSize: "24px", color: "var(--harness-blue)" }} />
          )
        }
        description={record.description}
      />
    );
  }

  render() {
    return (
      <>
        <div className="mb-35">
          <AntTitle style={{ marginBottom: "5px" }} level={4}>
            Select a trigger to get started
          </AntTitle>
          <AntText className="d-block mb-15 w-60">
            Pick up from one of the most commonly used triggers, then orchestrate any number of actions using the rich
            collection of connectors
          </AntText>
        </div>
        <PaginatedGrid
          restCall={"getIntegrations"}
          mapData={this.buildCard}
          restAction={this.props.propelTriggerTemplatesList}
          uri={"propel_trigger_templates"}
          itemsPerRow={4}
          pageSize={100}
          showPagination={false}
          partialFilters={this.state.partial_filters}
          moreFilters={this.state.more_filters}
          reload={this.state.reload}
        />
      </>
    );
  }
}

export default withRouter(connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(TriggersListContainer));
