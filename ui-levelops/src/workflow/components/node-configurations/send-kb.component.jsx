import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { Drawer, Form, Select, Input, Typography, Divider, Icon } from "antd";
import React from "react";
import { SelectRestapi } from "shared-resources/helpers";
import { AntButton } from "shared-resources/components";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { getError, getLoading } from "../../../utils/loadingUtils";

const { Option } = Select;
const { TextArea } = Input;
const { Text } = Typography;

export class SendKBComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      node_id: undefined,
      best_practices: undefined,
      comm_channel: undefined,
      message_template: undefined,
      additional_info: undefined,
      target_emails: [],
      bps_loading: false,
      message_template_loading: false
    };
    this.buildNodeProperties = this.buildNodeProperties.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (props.node.id !== state.node_id) {
      let bpsLoading = false;
      let messageLoading = false;
      const targetEmails = props.node.properties.configurations.target_emails || [];
      if (props.node.properties.configurations.best_practices_id !== undefined) {
        bpsLoading = true;
        props.bpsGet(props.node.properties.configurations.best_practices_id);
      }
      if (props.node.properties.configurations.message_template_id !== undefined) {
        messageLoading = true;
        props.cTemplatesGet(props.node.properties.configurations.message_template_id);
      }
      return {
        ...state,
        node_id: props.node.id,
        bps_loading: bpsLoading,
        message_template_loading: messageLoading,
        target_emails: targetEmails.map(email => ({ key: email, label: email })),
        additional_info: props.node.properties.configurations.additional_info,
        comm_channel: {
          key: props.node.properties.configurations.comm_channel,
          label: props.node.properties.configurations.comm_channel
        }
      };
    }

    let newState = { ...state };
    if (state.bps_loading) {
      const bpsId = props.node.properties.configurations.best_practices_id;
      if (
        !getLoading(props.rest_api, "bestpractices", "get", bpsId) &&
        !getError(props.rest_api, "bestpractices", "get", bpsId)
      ) {
        newState.bps_loading = false;
        newState.best_practices = { key: bpsId, label: props.rest_api.bestpractices.get[bpsId].data.name };
      }
    }

    if (state.message_template_loading) {
      const messageId = props.node.properties.configurations.message_template_id;
      if (
        !getLoading(props.rest_api, "ctemplates", "get", messageId) &&
        !getError(props.rest_api, "ctemplates", "get", messageId)
      ) {
        newState.message_template_loading = false;
        newState.message_template = { key: messageId, label: props.rest_api.ctemplates.get[messageId].data.name };
      }
    }

    return newState;
  }

  buildNodeProperties() {
    return {
      ...this.props.node.properties,
      configurations: {
        ...this.props.node.configurations,
        best_practices_id: this.state.best_practices.key,
        comm_channel: this.state.comm_channel.key,
        message_template_id: this.state.message_template.key,
        target_emails: this.state.target_emails.map(user => user.label),
        additional_info: this.state.additional_info
      }
    };
  }

  render() {
    const { visible, onClose, node } = this.props;
    return (
      <Drawer
        //width={"356px"}
        title={
          <div className="flex align-center">
            <Icon className="mr-5" type={node.properties.icon} />
            <Text editable={true}>{node.properties.name}</Text>
          </div>
        }
        placement={"right"}
        closable={true}
        onClose={onClose}
        visible={visible}
        getContainer={false}
        destroyOnClose={true}>
        <Text className="mb-20" type={"secondary"}>
          Send Knowledge Base write-ups to Contributors via your chosen communication platform
        </Text>
        <Divider />
        <Form layout={"vertical"}>
          <Form.Item label={"Knowledge Base"} colon={false}>
            <SelectRestapi
              mode={"single"}
              allowClear={true}
              placeholder={"Select Knowledge Base"}
              //onSearch={this.handleSearch}
              //onChange={this.handleChange}
              notFoundContent={null}
              searchField="name"
              uri="bestpractices"
              fetchData={this.props.bpsList}
              rest_api={this.props.rest_api}
              value={this.state.best_practices}
              onChange={value => this.setState({ best_practices: value })}
            />
          </Form.Item>
          <Divider />
          <Text className="ant-drawer-title mb-10 d-block">Notification Settings</Text>
          <Form.Item label={"Medium"} colon={false}>
            <Select
              showSearch={true}
              filterOption={true}
              //mode={"multiple"}
              allowClear={true}
              showArrow={true}
              placeholder={"Select Communication Medium"}
              //onSearch={this.handleSearch}
              //onChange={this.handleChange}
              notFoundContent={null}
              labelInValue={true}
              value={this.state.comm_channel}
              onChange={value => this.setState({ comm_channel: value })}>
              <Option key={"EMAIL"}>EMAIL</Option>
              <Option key={"SLACK"}>SLACK</Option>
            </Select>
          </Form.Item>
          <Form.Item label={"Message Template"} colon={false}>
            <SelectRestapi
              showSearch={true}
              filterOption={true}
              mode={"single"}
              allowClear={true}
              showArrow={true}
              placeholder={"Select Message Template"}
              //onSearch={this.handleSearch}
              //onChange={this.handleChange}
              notFoundContent={null}
              rest_api={this.props.rest_api}
              uri={"ctemplates"}
              fetchData={this.props.cTemplatesList}
              value={this.state.message_template}
              onChange={value => this.setState({ message_template: value })}
            />
          </Form.Item>
          <Form.Item label={"Additional comments"} colon={false}>
            <TextArea
              placeholder="Any additional comments to include in the message"
              autoSize={{ minRows: 3, maxRows: 5 }}
              value={this.state.additional_info}
              onChange={e => this.setState({ additional_info: e.currentTarget.value })}
            />
          </Form.Item>
          <Divider />
          <Form.Item label={"Who should this knowledge base article be sent to?"} colon={false}>
            <SelectRestapi
              showSearch={true}
              filterOption={true}
              allowClear={true}
              showArrow={true}
              mode={"multiple"}
              placeholder={"Select user or enter email"}
              //rest_api={this.props.rest_api}
              uri={"users"}
              fetchData={this.props.usersList}
              searchField={"email"}
              createOption={true}
              value={this.state.target_emails}
              onChange={value => this.setState({ target_emails: value })}
            />
          </Form.Item>
        </Form>
        <div>
          <AntButton type={"primary"} block onClick={e => this.props.onUpdate(node.id, this.buildNodeProperties())}>
            Done
          </AntButton>
        </div>
      </Drawer>
    );
  }
}

SendKBComponent.propTypes = {
  className: PropTypes.string.isRequired,
  node: PropTypes.object.isRequired,
  visible: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired
};

SendKBComponent.defaultProps = {
  className: "send-kb-drawer",
  visible: false
};

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(SendKBComponent);
