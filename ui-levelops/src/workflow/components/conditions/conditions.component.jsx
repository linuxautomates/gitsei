import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { update, get, unset } from "lodash";
import { Row, Col, Button, Select, Input, Icon, Tree, Tag } from "antd";
import { SelectRestapi } from "shared-resources/helpers";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import "./conditions.style.scss";
import uuidv1 from "uuid/v1";

const { Option } = Select;
const { TreeNode } = Tree;

const DEFAULT_DATA = {
  type: "condition",
  name: "or",
  id: "1",
  path: "",
  children: [
    {
      type: "rule",
      id: "2",
      signature: { key: "3", name: "Signature 3" },
      operator: { key: "==", value: "==" },
      value: "true"
    },
    {
      type: "condition",
      name: "and",
      id: "77",
      children: [
        {
          type: "rule",
          id: "3",
          signature: { key: "1", name: "Signature 1" },
          operator: { key: "==", value: "==" },
          value: "true"
        },
        {
          type: "rule",
          id: "4",
          signature: { key: "2", name: "Signature 2" },
          operator: { key: "==", value: "==" },
          value: "true"
        }
      ]
    }
  ]
};

export class ConditionsComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      data: DEFAULT_DATA
    };

    this.buildConditions = this.buildConditions.bind(this);
    this.buildRule = this.buildRule.bind(this);
    this.addRule = this.addRule.bind(this);
    this.deleteCondition = this.deleteCondition.bind(this);
    this.addCondition = this.addCondition.bind(this);
    this.updateSignature = this.updateSignature.bind(this);
  }

  buildRule(item, path) {
    return (
      <TreeNode
        key={path.concat(`.id=${item.id}`)}
        isLeaf={true}
        selectable={false}
        title={
          <Row gutter={[5, 10]} type={"flex"} justify={"center"} align={"middle"}>
            <Col span={12}>
              <SelectRestapi
                mode={"single"}
                style={{ width: "100%" }}
                placeholder={"Select Signature"}
                uri="signatures"
                fetchData={this.props.signaturesList}
                rest_api={this.props.rest_api}
                searchField={"signature"}
                onChange={value => this.updateSignature(path, value)}
                value={item.signature}
              />
            </Col>
            <Col span={6}>
              <Select
                style={{ width: "100%" }}
                placeholder={"Operator"}
                onChange={value => this.updateOperator(path, value)}
                labelInValue={true}
                value={item.operator}>
                <Option key={"=="} value={"=="}>
                  ==
                </Option>
                <Option key={"!="} value={"!="}>
                  !=
                </Option>
                <Option key={">"} value={">"}>
                  {">"}
                </Option>
                <Option key={"<"} value={"<"}>
                  {"<"}
                </Option>
                <Option key={"DIFF"} value={"DIFF"}>
                  DIFF
                </Option>
              </Select>
            </Col>
            <Col span={4}>
              <Input onChange={e => this.updateValue(path, e.currentTarget.value)} value={item.value} />
            </Col>
            <Col span={2}>
              <Button.Group>
                <Button icon={"delete"} type={"link"} onClick={e => this.deleteCondition(path)} />
              </Button.Group>
            </Col>
          </Row>
        }
      />
    );
  }

  updateSignature(nodePath, value) {
    nodePath = nodePath.replace(/^\./, "");
    //let data = this.state.data;
    let data = this.props.conditions;
    let item = get(data, nodePath);
    item.signature = value;
    data = update(data, nodePath, item);
    this.setState({ data: data }, () => this.props.updateRules(data));
  }

  updateOperator(nodePath, value) {
    nodePath = nodePath.replace(/^\./, "");
    //let data = this.state.data;
    let data = this.props.conditions;
    let item = get(data, nodePath);
    item.operator = value;
    data = update(data, nodePath, item);
    this.setState({ data: data }, () => this.props.updateRules(data));
  }

  updateValue(nodePath, value) {
    console.log(value);
    nodePath = nodePath.replace(/^\./, "");
    //let data = this.state.data;
    let data = this.props.conditions;
    let item = get(data, nodePath);
    item.value = value;
    data = update(data, nodePath, item);
    this.setState({ data: data }, () => this.props.updateRules(data));
  }

  addRule(nodePath) {
    nodePath = nodePath.replace(/^\./, "");
    //let data = this.state.data;
    let data = this.props.conditions;
    let children = get(data, nodePath.concat("children"));
    children = children.push({ type: "rule", id: uuidv1() });
    data = update(data, nodePath.concat(".children"), children);
    this.setState({ data: data }, () => this.props.updateRules(data));
  }

  addCondition(nodePath, condition = "or") {
    nodePath = nodePath.replace(/^\./, "");
    //let data = this.state.data;
    let data = this.props.conditions;
    let children = get(data, nodePath.concat("children"));
    children = children.push({
      type: "condition",
      id: uuidv1(),
      name: condition,
      children: [{ type: "rule", id: uuidv1() }]
    });
    data = update(data, nodePath.concat(".children"), children);
    this.setState({ data: data }, () => this.props.updateRules(data));
  }

  deleteCondition(nodePath) {
    nodePath = nodePath.replace(/^\./, "");
    //let data = this.state.data;
    let data = this.props.conditions;
    unset(data, nodePath);
    console.log(data);
    this.setState({ data: data }, () => this.props.updateRules(data));
  }

  buildConditions(node, canDelete = true, parent = "") {
    if (node === null) {
      return "";
    }
    return (
      <TreeNode
        title={
          <Row type={"flex"} justify={"start"} align={"middle"}>
            <Col>
              <Tag color={"red"}>{node.name.toUpperCase()}</Tag>
            </Col>
            <Col>
              <Button.Group>
                <Button type={"link"} onClick={e => this.addRule(parent)}>
                  + RULE
                </Button>
                {parent === "" && (
                  <Button.Group>
                    <Button type={"link"} onClick={e => this.addCondition(parent, "and")}>
                      + AND
                    </Button>
                    <Button type={"link"} onClick={e => this.addCondition(parent, "or")}>
                      + OR
                    </Button>
                  </Button.Group>
                )}

                {canDelete && <Button icon={"delete"} type={"link"} onClick={e => this.deleteCondition(parent)} />}
              </Button.Group>
            </Col>
          </Row>
        }
        key={parent.concat(`.id=${node.id}`)}>
        {node.children.map((child, index) => {
          if (child === null) {
            return "";
          }
          if (child.type === "rule") {
            return this.buildRule(child, parent.concat(`.children[${index}]`));
          } else {
            return this.buildConditions(child, true, parent.concat(`.children[${index}]`));
          }
        })}
      </TreeNode>
    );
  }

  render() {
    let data = JSON.parse(JSON.stringify(this.props.conditions));
    return (
      <Tree
        showLine={true}
        showIcon={true}
        defaultExpandAll={true}
        switcherIcon={<Icon type="down" width={"2rem"} height={"2rem"} />}
        onFocus={() => {}}
        filterTreeNode={() => {
          return false;
        }}>
        {this.buildConditions(data, false, "")}
      </Tree>
    );
  }
}

ConditionsComponent.propTypes = {
  className: PropTypes.string,
  conditions: PropTypes.object.isRequired,
  updateRules: PropTypes.func.isRequired
  //onAdd: PropTypes.func.isRequired
};

ConditionsComponent.defaultProps = {
  className: "conditions-component",
  conditions: DEFAULT_DATA
};

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(ConditionsComponent);
