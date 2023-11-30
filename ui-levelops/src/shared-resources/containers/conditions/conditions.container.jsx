import React from "react";
import * as PropTypes from "prop-types";
import { get, unset, update } from "lodash";
import { Button, Col, Icon, Input, Row, Select, Tag, Tree } from "antd";
import "./conditions.style.scss";
import uuidv1 from "uuid/v1";

const { Option } = Select;
const { TreeNode } = Tree;

export class ConditionsContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      data: {}
    };

    this.buildConditions = this.buildConditions.bind(this);
    this.buildRule = this.buildRule.bind(this);
    this.addRule = this.addRule.bind(this);
    this.deleteCondition = this.deleteCondition.bind(this);
    this.addCondition = this.addCondition.bind(this);
    this.updatePredicate = this.updatePredicate.bind(this);
    this.predicateOptions = this.predicateOptions.bind(this);
  }

  componentDidMount() {
    this.setState({ data: this.props.conditions });
  }

  buildRule(item, path) {
    return (
      <TreeNode
        key={path.concat(`.id=${item.id}`)}
        isLeaf={true}
        selectable={false}
        title={
          <Row gutter={[5, 10]} type={"flex"} justify={"start"} align={"middle"}>
            <Col span={9}>
              <Select
                mode={"single"}
                style={{ width: "100%" }}
                labelInValue={true}
                placeholder={"Select Predicate"}
                onChange={value => this.updatePredicate(path, value)}
                value={item.predicate}>
                {this.props.predicates.map(predicate => (
                  <Option key={predicate.key}>{predicate.label}</Option>
                ))}
              </Select>
            </Col>
            <Col span={5}>
              <Select
                style={{ width: "100%" }}
                placeholder={"Operator"}
                onChange={value => this.updateOperator(path, value)}
                labelInValue={true}
                value={item.operator}>
                {this.props.operators.map(operator => (
                  <Option key={operator}>{operator}</Option>
                ))}
              </Select>
            </Col>
            {this.predicateOptions(path).length > 0 && (
              <Col span={6}>
                <Select
                  style={{ width: "100%" }}
                  placeholder={"Operator"}
                  onChange={value => this.updateValue(path, value)}
                  labelInValue={true}
                  value={item.value}>
                  {this.predicateOptions(path).map(val => (
                    <Option key={val}>{val}</Option>
                  ))}
                </Select>
                {/*<Input onChange={(e) => this.updateValue(path,e.currentTarget.value)} value={item.value}/>*/}
              </Col>
            )}
            {this.predicateOptions(path).length === 0 && (
              <Col span={6}>
                <Input onChange={e => this.updateValue(path, e.currentTarget.value)} value={item.value} />
              </Col>
            )}

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

  updatePredicate(nodePath, value) {
    nodePath = nodePath.replace(/^\./, "");
    //let data = this.state.data;
    let data = this.props.conditions;
    let item = get(data, nodePath);
    item.predicate = value;
    data = update(data, nodePath, item);
    this.setState({ data: data }, () => this.props.updateRules(data));
  }

  predicateOptions(nodePath) {
    nodePath = nodePath.replace(/^\./, "");
    let data = this.props.conditions;
    let item = get(data, nodePath);
    if (!item) {
      return [];
    }
    if (item.predicate !== undefined && item.predicate.key !== undefined) {
      const predicate = this.props.predicates.filter(pred => pred.key === item.predicate.key)[0];
      if (predicate.options) {
        return predicate.options.map(option => option.value);
      }
    }
    return [];
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
      // add action here , one action if all the rules match
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

  handleQuestionState(e) {}

  render() {
    let data = JSON.parse(JSON.stringify(this.props.conditions));
    return (
      <>
        {/*<Form layout={"inline"}>*/}
        {/*  <Form.Item label={"On Condition Match"}>*/}
        {/*    <Radio.Group options={stateOptions} onChange={this.handleQuestionState} />*/}
        {/*  </Form.Item>*/}
        {/*</Form>*/}
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
      </>
    );
  }
}

ConditionsContainer.propTypes = {
  className: PropTypes.string,
  conditions: PropTypes.object.isRequired,
  predicates: PropTypes.array.isRequired,
  operators: PropTypes.array.isRequired,
  updateRules: PropTypes.func.isRequired
  //onAdd: PropTypes.func.isRequired
};

ConditionsContainer.defaultProps = {
  className: "conditions-component",
  updateRules: () => {},
  conditions: {
    type: "condition",
    name: "or",
    id: "1",
    path: "",
    children: [
      {
        type: "rule",
        id: "2",
        predicate: {},
        operator: {},
        value: undefined
      }
    ]
  },
  predicates: [
    {
      key: "s1.q1",
      label: "section1/question1",
      options: [
        { value: "yes", score: 1 },
        { value: "no", score: 2 }
      ]
    },
    {
      key: "s1.q2",
      label: "section2/question2",
      options: [
        { value: "yes", score: 1 },
        { value: "no", score: 2 }
      ]
    },
    {
      key: "s1.q3",
      label: "section3/question3",
      options: [
        { value: "yes", score: 1 },
        { value: "no", score: 2 }
      ]
    }
  ],
  operators: ["==", "!=", "in", "nin", ">", "<"]
};

export default ConditionsContainer;
