import React from "react";
import * as PropTypes from "prop-types";
import { Button, Drawer, Select, Popconfirm, Form } from "antd";
import { get } from "lodash";

const { Option } = Select;

export class LinkNodeComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      option_value: undefined
    };
  }

  componentDidMount() {
    const { link } = this.props;
    //const currentOption = link.properties.option || undefined;
    const currentOption = get(link, ["properties", "option"], undefined);
    this.setState({
      option_value: currentOption
    });
  }

  get linkOptions() {
    const { options } = this.props;
    return (
      <Form layout={"vertical"} style={{ paddingTop: "10px" }}>
        <Form.Item label={"link option"}>
          <Select
            style={{ width: "100%" }}
            mode={"default"}
            allowClear={false}
            showArrow={true}
            showSearch={false}
            labelInValue={false}
            value={this.state.option_value}
            onChange={value => {
              this.setState({ option_value: value });
            }}>
            {options.map(option => (
              <Option key={option}>{option}</Option>
            ))}
          </Select>
        </Form.Item>
      </Form>
    );
  }

  render() {
    const { onClose, visible, onUpdate, link, options, onDelete } = this.props;
    console.log(options);
    return (
      <Drawer
        //width="356px"
        title={`Link from #${link.from.nodeId}-${link.from.portId} to #${link.to.nodeId}-${link.to.portId}`}
        placement="right"
        closable
        onClose={onClose}
        visible={visible}
        getContainer={false}>
        <Popconfirm onConfirm={e => onDelete(link)} title={"Deleting links may affect node configurations. Proceed?"}>
          <Button
            icon={"delete"}
            type={"danger"}
            block
            //onClick={e => onDelete(link)}
          >
            Delete Link
          </Button>
        </Popconfirm>
        {options.length > 0 && this.linkOptions}
        {options.length > 0 && (
          <div className="mt-10 pt-10">
            <Button type="primary" block onClick={e => onUpdate(link, this.state.option_value)}>
              Done
            </Button>
          </div>
        )}
      </Drawer>
    );
  }
}

LinkNodeComponent.propTypes = {
  visible: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onDelete: PropTypes.func.isRequired,
  onUpdate: PropTypes.func.isRequired,
  link: PropTypes.array.isRequired,
  options: PropTypes.array
};

LinkNodeComponent.defaultProps = {
  visible: false,
  options: []
};
