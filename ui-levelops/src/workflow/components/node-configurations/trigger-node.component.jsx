import React from "react";
import { Button, Drawer, Typography } from "antd";
import { GenericFormComponent } from "shared-resources/containers";

const { Text } = Typography;

export class TriggerNodeComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      trigger_type: { label: "manual", key: "manual" },
      trigger_event: {},
      values: {}
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
      id: this.props.propelId,
      trigger_type: { label: this.props.node.trigger_type, key: this.props.node.trigger_type },
      trigger_event: { label: this.props.node.trigger_event, key: this.props.node.trigger_event }
    });
  }

  static getDerivedStateFromProps(props, state) {
    if (props.propelId !== state.id) {
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
        id: props.propelId,
        trigger_type: { label: props.node.trigger_type, key: props.node.trigger_type },
        trigger_event: { label: props.node.trigger_event, key: props.node.trigger_event }
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

  get fields() {
    const {
      node: { input }
    } = this.props;
    let inputArray = [];
    Object.keys(input).forEach(field => {
      if (!input[field].hidden) {
        const inputJson = input[field].json;
        //console.log(inputJson);
        inputJson.label = inputJson.display_name || field;
        inputJson.values = this.state.values[field];

        inputArray.push(inputJson);
      }
    });
    //const input = this.input;
    return <GenericFormComponent elements={inputArray} onChange={this.handleChanges} triggerNode />;
  }

  get input() {
    // this is mostly going to be a key value pair of components that will be updated

    return { ...this.state.values };
  }

  render() {
    const { onClose, visible, onUpdate, node } = this.props;

    return (
      <Drawer
        width={500}
        title={node.name}
        placement="right"
        closable
        onClose={e => onUpdate(node.id, this.input)}
        //onClose={onClose}
        visible={visible}
        destroyOnClose={true}
        getContainer={false}>
        <div style={{ marginBottom: "10px" }}>
          <Text type="secondary" style={{ fontSize: "12px" }}>
            {node.description}
          </Text>
        </div>
        {this.fields}
        <div className="mt-10">
          <Button type="primary" block onClick={e => onUpdate(node.id, this.input)}>
            Done
          </Button>
        </div>
      </Drawer>
    );
  }
}

export default TriggerNodeComponent;

//export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(TriggerNodeComponent);
