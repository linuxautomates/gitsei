import React from "react";
import { capitalize, cloneDeep } from "lodash";
import * as PropTypes from "prop-types";
import { Button, Drawer, Form, Input, Typography } from "antd";
import { GenericFormComponent } from "shared-resources/containers";
import { nodeInputTransformer } from "./helper";
import NodeDryRunModal from "../dry-run-modal/NodeDryRunModal";

const { Text } = Typography;

export class GenericNodeComponent extends React.PureComponent {
  values = {};

  constructor(props) {
    super(props);
    this.state = {
      values: {},
      nodeDryRunModal: false
    };
  }

  componentDidMount() {
    const {
      node: { input }
    } = this.props;
    let values = {};
    Object.keys(input).forEach(field => {
      values[field] = { value: input[field].values };
    });
    this.setState({
      values: values,
      id: this.props.node.id
    });
  }

  static getDerivedStateFromProps(props, state) {
    if (props.node.id !== state.id) {
      const {
        node: { input }
      } = props;
      let values = {};
      Object.keys(input).forEach(field => {
        values[field] = { value: input[field].values };
      });
      return {
        ...state,
        values: values,
        id: props.node.id
      };
    }
    return null;
  }

  handleChanges = values => {
    this.setState({
      values: {
        ...this.state.values,
        ...values
      }
    });
  };

  get input() {
    return { ...this.state.values };
  }

  get fields() {
    const { node, predicates } = this.props;
    const inputArray = nodeInputTransformer({ node, predicates, values: this.state.values });
    return (
      <GenericFormComponent
        elements={inputArray.sort((a, b) => {
          return a.index - b.index;
        })}
        onChange={this.handleChanges}
      />
    );
  }

  get valid() {
    const { node } = this.props;
    const input = node.input || {};
    let valid = true;
    Object.keys(input).forEach(field => {
      if (field.required) {
      }
    });
  }

  get tabs() {
    const { node, predicates, onUpdateName } = this.props;
    const input = node.input || {};
    return (
      <>
        <Form layout="vertical">
          <Form.Item label="name" colon={false}>
            <Input value={node.name} onChange={e => onUpdateName(node.id, e.target.value)} />
          </Form.Item>
        </Form>
        {this.fields}
      </>
    );
  }

  updatedNode = () => {
    let newNode = cloneDeep(this.props.node);
    Object.keys(newNode.input).forEach(field => {
      if (this.input[field]) {
        newNode.input[field].values = this.input[field].value;
      }
    });
    return newNode;
  };

  render() {
    const { onClose, visible, onUpdate, node, predicates } = this.props;
    const title = capitalize(node.name || "").replace(/_/g, " ");
    return (
      <Drawer
        width={500}
        title={
          <div className={"flex justify-content-between"} style={{ alignItems: "center" }}>
            {title}
            {node?.type === "script" && (
              <Button
                type="primary"
                onClick={() => this.setState({ nodeDryRunModal: true })}
                style={{ marginRight: "2.5rem" }}>
                Dry Run
              </Button>
            )}
          </div>
        }
        placement="right"
        closable
        onClose={e => {
          onUpdate(node.id, this.input);
        }}
        visible={visible}
        destroyOnClose={true}
        getContainer={false}>
        {this.state.nodeDryRunModal && (
          <NodeDryRunModal
            node={this.updatedNode()}
            predicates={predicates}
            visible={this.state.nodeDryRunModal}
            onCancel={() => this.setState({ nodeDryRunModal: false })}
          />
        )}
        <>
          <Text type="secondary">{node.description}</Text>
          <br />
          <Text type={"secondary"} style={{ fontSize: "11px" }} className={"mb-10 mt-10"}>
            Pro Tip: To access variables from previous nodes, just type '$' in the text box
          </Text>
        </>
        {this.tabs}
        <div className="mt-10">
          <Button type="primary" block onClick={e => onUpdate(node.id, this.input)}>
            Done
          </Button>
        </div>
      </Drawer>
    );
  }
}

GenericNodeComponent.propTypes = {
  visible: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onUpdate: PropTypes.func.isRequired,
  node: PropTypes.array.isRequired,
  predicates: PropTypes.array
};

GenericNodeComponent.defaultProps = {
  visible: false,
  node: {}
};
