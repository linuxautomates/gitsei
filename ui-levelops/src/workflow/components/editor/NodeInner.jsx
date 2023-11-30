import React from "react";
import { capitalize } from "lodash";
import { Button, Icon, Menu, Popconfirm } from "antd";
import { AntButton, AntText, AntTitle, IntegrationIcon } from "shared-resources/components";
import PropTypes from "prop-types";
import { ICON_PATH } from "shared-resources/components/integration-icons/icon-path.config";
import { get } from "lodash";

export class NodeInner extends React.PureComponent {
  // eslint-disable-next-line no-useless-constructor
  constructor(props) {
    super(props);
  }

  connectorHorizontalClassNames = ports => {
    let classNames = "";
    for (let k in ports) {
      if (ports[k].type === "left") classNames = `${classNames} connector__horizontal-left`;
      if (ports[k].type === "right") classNames = `${classNames} connector__horizontal-right`;
    }
    return classNames;
  };

  connectorVerticalClassNames = ports => {
    let classNames = "";
    //console.log('ports = ',ports)
    for (let k in ports) {
      if (ports[k].type === "output") classNames = `${classNames} connector__vertical-bottom`;
      if (ports[k].type === "input") classNames = `${classNames} connector__vertical-top`;
    }
    return classNames;
  };

  getNodeSettingsMenu = node => {
    return (
      <Menu>
        <Menu.Item
          key="0"
          //onClick={e => node.properties.onSetting(node.id)}
          onClick={e => this.props.onSetting(node.id)}>
          <Icon type={"edit"} /> Edit
        </Menu.Item>
        {node.type !== "trigger" && (
          <Menu.Item
            key="1"
            //onClick={e => node.properties.onDelete(node.id)}
            onClick={e => this.props.onDelete(node.id)}>
            <Icon type={"delete"} />
            Delete
          </Menu.Item>
        )}
      </Menu>
    );
  };

  renderNodeType = type => {
    type = capitalize(type || "").replace(/_/g, " ");
    return type.length > 18 ? type.slice(0, 18).concat("...") : type;
  };

  render() {
    const { node, showErrors } = this.props;
    const integrationIcons = ICON_PATH.map(icon => icon.temp_props.type);
    const icon = get(node, ["properties", "icon"], "play-circle");
    let validNode = true;
    Object.keys(node.input).forEach(field => {
      if (node.input[field].required && !node.input[field].hidden) {
        validNode = validNode && node.input[field].values !== undefined && node.input[field].values[0] !== undefined;
      }
    });

    const name = node.name;
    let leftClassName =
      node.type === "trigger"
        ? "workflow__node-left-trigger"
        : integrationIcons.includes(icon)
        ? "workflow__node-left-integration"
        : "workflow__node-left";
    let rightClassName = "workflow__node-right";
    if (!validNode && showErrors) {
      leftClassName = `${leftClassName} ${leftClassName}__error`;
      rightClassName = `${rightClassName} ${rightClassName}__error`;
    }

    const RefButton = React.forwardRef((props, ref) => {
      return <Button ref={ref} {...props} />;
    });

    const buttonRef = React.createRef();

    return (
      <div
      //className={` ${this.connectorVerticalClassNames(node.ports)}`}
      >
        <div
          //className={`workflow__node ${this.connectorHorizontalClassNames(node.ports)}`}
          className={`workflow__node`}>
          <div
            //className="workflow__node-left"
            className={leftClassName}>
            {integrationIcons.includes(icon) && <IntegrationIcon type={icon} />}
            {!integrationIcons.includes(icon) && (
              <Icon
                type={node.properties.icon}
                style={{
                  fontSize: "28px",
                  //color: "#fff",
                  margin: "5px"
                }}
              />
            )}
          </div>
          <div className={rightClassName}>
            <div className="workflow__node-right__details">
              {/*<label className="title">TYPE</label>*/}
              <AntTitle level={4} ellipsis>
                {name}
              </AntTitle>
              <AntText type={"secondary"} style={{ fontSize: "12px", width: "100%" }} ellipsis>
                {this.renderNodeType(node.type)}
                {` #${node.id}`}
              </AntText>
            </div>
            <div className="workflow__node-right__extra">
              <AntButton
                style={{ zIndex: 3, pointerEvents: "auto" }}
                id={"edit"}
                icon={"edit"}
                type={"link"}
                size={"small"}
                ref={"edit"}
                onClick={e => {
                  e.stopPropagation();
                  this.props.onSetting(node.id);
                }}
              />
              {node.type !== "trigger" && (
                <Popconfirm
                  title={"Are you sure you want to delete this node?"}
                  onConfirm={e => {
                    e.stopPropagation();
                    this.props.onDelete(node.id);
                  }}>
                  <RefButton
                    ref={buttonRef}
                    id={"delete"}
                    icon={"delete"}
                    type={"link"}
                    size={"small"}
                    style={{ zIndex: 3, pointerEvents: "auto" }}
                    //onClick={e => this.props.onDelete(node.id)}
                  />
                </Popconfirm>
              )}

              {/*<Dropdown overlay={this.getNodeSettingsMenu(node)} trigger={["click"]}>*/}
              {/*  <a className="ant-dropdown-link" onClick={e => e.preventDefault()}>*/}
              {/*    <Icon type="more" />*/}
              {/*  </a>*/}
              {/*</Dropdown>*/}
            </div>
          </div>
        </div>
      </div>
    );
  }
}

NodeInner.propTypes = {
  node: PropTypes.object,
  config: PropTypes.object,
  actionButtons: PropTypes.bool.isRequired,
  showErrors: PropTypes.bool
};

NodeInner.defaultProps = {
  actionButtons: true,
  showErrors: false
};
