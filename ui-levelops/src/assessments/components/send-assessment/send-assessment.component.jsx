import React from "react";
import { connect } from "react-redux";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import * as PropTypes from "prop-types";
import { Input, Select } from "antd";
import { AntButton, AntForm, AntFormItem, AntModal } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { validateEmail } from "utils/stringUtils";
import { EMAIL_WARNING } from "constants/formWarnings";
import { RestCommTemplate } from "classes/RestCommTemplate";

const { TextArea } = Input;
const { Option } = Select;

export class SendAssessmentComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = this.defaultStates();
  }

  defaultStates = () => ({
    questionnaire_select: {},
    questionnaire_comms_select: {},
    template_select: {},
    questionnaire_to: {},
    template_type_select: {},
    additional_info: "",
    send_checked: false
  });

  handleToChange = option => {
    if (option && Array.isArray(option.label)) {
      option.label = option.label[1];
    }
    this.setState({
      questionnaire_to: option || {}
    });
  };

  handleAssessmentChange = option =>
    this.setState({
      questionnaire_select: option || {}
    });

  handleTemplateTypeChange = option =>
    this.setState({
      template_type_select: option,
      template_select: {}
    });

  handleTemplateChange = option =>
    this.setState({
      template_select: option || {}
    });

  handleAdditionalInformationChange = e =>
    this.setState({
      additional_info: e.target.value
    });

  handleCancel = () => {
    this.resetData(this.props.onCancel);
  };

  handleSend = () => {
    const { questionnaire_select, questionnaire_to, questionnaire_comms_select, template_select, additional_info } =
      this.state;
    let sendData = {
      questionnaire_select: questionnaire_select.key,
      product_id: this.props.product_id
    };
    if (this.props.canSend) {
      sendData.questionnaire_to = questionnaire_to.label;
      sendData.template_select = template_select.key;
      sendData.additional_info = additional_info;
      sendData.questionnaire_comms_select = questionnaire_comms_select.key;
    }
    this.resetData(() => {
      this.props.onSend(sendData);
    });
  };

  resetData = callback => {
    this.setState(this.defaultStates(), callback());
  };

  get assessment() {
    const { qsList, rest_api } = this.props;
    const { questionnaire_select } = this.state;
    return (
      <AntFormItem required label="Assessment">
        <SelectRestapi
          searchField="name"
          uri="questionnaires"
          fetchData={qsList}
          method="list"
          rest_api={rest_api}
          isMulti={false}
          closeMenuOnSelect
          value={questionnaire_select || {}}
          creatable={false}
          mode={"single"}
          labelInValue
          onChange={this.handleAssessmentChange}
        />
      </AntFormItem>
    );
  }

  get to() {
    const { usersList, rest_api } = this.props;
    const { questionnaire_to } = this.state;

    return (
      <AntFormItem label="To" help={validateEmail(questionnaire_to.label) ? "" : EMAIL_WARNING}>
        <SelectRestapi
          searchField="email"
          uri="users"
          fetchData={usersList}
          method="list"
          rest_api={rest_api}
          isMulti={false}
          closeMenuOnSelect
          value={questionnaire_to}
          createOption
          mode={"single"}
          labelInValue
          onChange={this.handleToChange}
        />
      </AntFormItem>
    );
  }

  get templateType() {
    const { template_type_select } = this.state;
    return (
      <AntFormItem label="Template Type">
        <Select value={template_type_select} onChange={this.handleTemplateTypeChange} labelInValue mode={"single"}>
          {RestCommTemplate.OPTIONS.map(type => (
            <Option key={type}>{type}</Option>
          ))}
        </Select>
      </AntFormItem>
    );
  }

  get template() {
    const { cTemplatesList, rest_api } = this.props;
    const { template_type_select, template_select } = this.state;
    return (
      <AntFormItem label="Template" help="Selected template will be added as message with questionnaire link">
        <SelectRestapi
          disabled={template_type_select.key === undefined}
          moreFilters={{ type: template_type_select.key }}
          searchField="name"
          uri="ctemplates"
          fetchData={cTemplatesList}
          method="list"
          rest_api={rest_api}
          isMulti={false}
          closeMenuOnSelect
          value={template_select}
          creatable={false}
          mode={"single"}
          labelInValue
          onChange={this.handleTemplateChange}
        />
      </AntFormItem>
    );
  }

  get additionalInformation() {
    const { additional_info } = this.state;
    return (
      <AntFormItem label="Additional Information">
        <TextArea
          rows={4}
          id="additional-info"
          value={additional_info}
          onChange={this.handleAdditionalInformationChange}
        />
      </AntFormItem>
    );
  }

  get form() {
    return (
      <AntForm layout="vertical">
        {this.assessment}
        {this.props.canSend && this.to}
        {this.props.canSend && this.templateType}
        {this.props.canSend && this.template}
        {this.props.canSend && this.additionalInformation}
      </AntForm>
    );
  }

  get footer() {
    let sendEnabled =
      this.state.questionnaire_select.key &&
      (this.state.questionnaire_to.label ? validateEmail(this.state.questionnaire_to.label) : true);
    if (this.props.canSend) {
      sendEnabled = sendEnabled && this.state.template_select.key !== undefined;
      sendEnabled = sendEnabled && this.state.template_type_select.key !== undefined;
    }
    return (
      <div className="flex direction-row justify-end">
        <AntButton style={{ margin: "5px" }} onClick={this.handleCancel}>
          Cancel
        </AntButton>
        <AntButton type="primary" style={{ margin: "5px" }} disabled={!sendEnabled} onClick={this.handleSend}>
          {this.props.canSend ? "Send" : "Create"}
        </AntButton>
      </div>
    );
  }

  render() {
    const { show } = this.props;
    if (!show) {
      return null;
    }
    return (
      <AntModal
        visible={show}
        title={`${this.props.canSend ? "Send" : "Create"} Assessment`}
        footer={this.footer}
        width={640}
        centered
        onCancel={this.handleCancel}>
        {this.form}
      </AntModal>
    );
  }
}

SendAssessmentComponent.propTypes = {
  show: PropTypes.bool.isRequired,
  onCancel: PropTypes.func.isRequired,
  onSend: PropTypes.func.isRequired,
  canSend: PropTypes.bool.isRequired,
  product_id: PropTypes.string.isRequired
};

SendAssessmentComponent.defaultProps = {
  canSend: false
};

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(SendAssessmentComponent);
