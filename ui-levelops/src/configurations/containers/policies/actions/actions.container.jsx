import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { Card, Label } from "shared-resources/components";
import { Title } from "shared-resources/helpers";
import { RestPolicy } from "classes/RestPolicy";
import { RestCommTemplate } from "classes/RestCommTemplate";
import { mapPolicyFormStatetoProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import CustomCheckbox from "components/CustomCheckbox/CustomCheckbox";
import Select from "react-select";
import { SelectWrapper } from "shared-resources/helpers";
import SearchableSelect from "components/SearchableSelect/SearchableSelect";
import { PolicyAssigned } from "records/policy";

const ACTION_DESCRIPTIONS = {
  workflow: "Create a workflow item for follow up",
  communication: "Send notification when the policy is triggered",
  assessment: "Send an assessment when the policy is triggered",
  knowledgebase: "Send a knowledge base article when the policy is triggered",
  log: "Log the policy trigger in event logs"
};

export class ActionsContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.buildActionForm = this.buildActionForm.bind(this);
    this.buildCommunicationSelect = this.buildCommunicationSelect.bind(this);
    this.handleActionSelect = this.handleActionSelect.bind(this);
  }

  handleActionSelect(target) {
    console.log(target.id);
    let policy = this.props.policy_form;
    console.log(policy);
    let actions = policy.actions;
    actions[target.id].selected = actions[target.id].selected === false;
    //actions.resetAction(target.id);
    //actions[target.id] = {};
    policy.actions = actions;
    this.props.formUpdateObj("policy_form", policy);
  }

  buildCommunicationSelect(action) {
    return (
      <div className={`flex direction-row justify-start`} key={action}>
        <div style={{ flexBasis: "30%", marginRight: "20px" }} key={`select-comm-template-${action}`}>
          <SelectWrapper label={"Template Type"}>
            <Select
              id={`select-comm-template`}
              options={RestCommTemplate.OPTIONS.map(type => ({ label: type, value: type }))}
              value={{
                label: this.props.policy_form.actions[action].template_type,
                value: this.props.policy_form.actions[action].template_type
              }}
              onChange={option => {
                let policy = this.props.policy_form;
                let actions = policy.actions;
                actions[action].template_type = option.value;
                actions[action].template_id = undefined;
                actions[action].template_select = {};
                policy.actions = actions;
                this.props.formUpdateObj("policy_form", policy);
              }}
              isLoading={!this.props.policy_form.actions[action].template_type}
              placeholder={"Select Template Type"}
            />
          </SelectWrapper>
        </div>
        {this.props.policy_form.actions[action].template_type && (
          <div style={{ flexBasis: "30%", marginRight: "20px" }} key={`select-template-${action}`}>
            <SelectWrapper label={"template"}>
              <SearchableSelect
                searchField="name"
                moreFilters={{ type: this.props.policy_form.actions[action].template_type }}
                uri="ctemplates"
                fetchData={this.props.cTemplatesList}
                method="list"
                rest_api={this.props.rest_api}
                isMulti={false}
                closeMenuOnSelect={true}
                value={this.props.policy_form.actions[action].template_select}
                creatable={false}
                onChange={option => {
                  let policy = this.props.policy_form;
                  let actions = policy.actions;
                  actions[action].template_select = option;
                  actions[action].template_id = option.value;
                  policy.actions = actions;
                  this.props.formUpdateObj("policy_form", policy);
                }}
                placeholder={"Select Communication Template"}
              />
            </SelectWrapper>
          </div>
        )}
        {this.props.policy_form.actions[action].template_id && (
          <div style={{ flexBasis: "30%" }} key={`select-to-${action}`}>
            <SelectWrapper label={"To"}>
              <SearchableSelect
                searchField="email"
                uri="users"
                fetchData={this.props.usersList}
                method="list"
                rest_api={this.props.rest_api}
                isMulti={true}
                closeMenuOnSelect={true}
                value={this.props.policy_form.actions[action].to.map(to => ({ label: to, value: to }))}
                creatable={true}
                createOptionPosition={"first"}
                onChange={options => {
                  let policy = this.props.policy_form;
                  let actions = policy.actions;
                  actions[action].to = options.map(option => option.label);
                  policy.actions = actions;
                  this.props.formUpdateObj("policy_form", policy);
                }}
                placeholder={"Select Recipient"}
              />
            </SelectWrapper>
          </div>
        )}
      </div>
    );
  }

  buildActionForm(action) {
    switch (action) {
      case "workflow":
        return (
          <div className={`flex direction-row justify-start`}>
            <div style={{ width: "30%", marginRight: "20px" }} key={`select-assigned`}>
              <SelectWrapper label={"assigned"}>
                <Select
                  id={"select-assigned"}
                  options={[{ label: PolicyAssigned.toUpperCase().replace("_", " "), value: PolicyAssigned }]}
                  value={{
                    label: this.props.policy_form.actions[action].assigning_process.toUpperCase().replace("_", " "),
                    value: this.props.policy_form.actions[action].assigning_process
                  }}
                  isMulti={false}
                  closeMenuOnSelect={true}
                  isDisabled={true}
                />
              </SelectWrapper>
            </div>
            <div style={{ width: "100%", flexBasis: "30%" }} key={`assignee-pool`}>
              <SelectWrapper label={"assignee Pool"}>
                <SearchableSelect
                  method={`list`}
                  uri={`users`}
                  searchField="email"
                  rest_api={this.props.rest_api}
                  fetchData={this.props.usersList}
                  id={"select-type"}
                  value={this.props.policy_form.actions[action].assignee_select}
                  isLoading={!this.props.policy_form.actions[action].assignee_select}
                  isMulti={true}
                  closeMenuOnSelect={true}
                  creatable={false}
                  placeholder={"Select Assignees for Policy"}
                  onChange={options => {
                    let policy = this.props.policy_form;
                    let actions = policy.actions;
                    actions[action].assignee_ids = options.map(option => option.value);
                    actions[action].assignee_select = options;
                    policy.actions = actions;
                    this.props.formUpdateObj("policy_form", policy);
                  }}
                />
              </SelectWrapper>
            </div>
          </div>
        );
      case "communication":
        return this.buildCommunicationSelect(action);
      case "assessment":
        return (
          <div className={`flex direction-column justify-start`}>
            {this.buildCommunicationSelect(action)}
            <div style={{ width: "30%", marginTop: "10px" }} key={`${action}-container`}>
              <SelectWrapper label={"Assessments"}>
                <SearchableSelect
                  searchField="name"
                  uri="questionnaires"
                  fetchData={this.props.qsList}
                  method="list"
                  rest_api={this.props.rest_api}
                  isMulti={true}
                  closeMenuOnSelect={true}
                  value={this.props.policy_form.actions[action].assessment_select}
                  placeholder={"Select Assessments"}
                  creatable={false}
                  isLoading={!this.props.policy_form.actions[action].assessment_select}
                  onChange={options => {
                    let policy = this.props.policy_form;
                    let actions = policy.actions;
                    actions[action].assessment_select = options;
                    actions[action].assessment_ids = options.map(option => option.value);
                    policy.actions = actions;
                    this.props.formUpdateObj("policy_form", policy);
                  }}
                />
              </SelectWrapper>
            </div>
          </div>
        );
      case "knowledgebase":
        return (
          <div className={`flex direction-column justify-start`}>
            {this.buildCommunicationSelect(action)}
            <div style={{ width: "30%", marginTop: "10px" }} key={`${action}-container`}>
              <SelectWrapper label={"Knowledge Bases"}>
                <SearchableSelect
                  searchField="name"
                  uri="bestpractices"
                  fetchData={this.props.bpsList}
                  method="list"
                  rest_api={this.props.rest_api}
                  isMulti={true}
                  closeMenuOnSelect={true}
                  placeholder={"Select Knowledge Base"}
                  value={this.props.policy_form.actions[action].kb_select}
                  isLoading={!this.props.policy_form.actions[action].kb_select}
                  creatable={false}
                  onChange={options => {
                    let policy = this.props.policy_form;
                    let actions = policy.actions;
                    actions[action].kb_select = options;
                    actions[action].kb_ids = options.map(option => option.value);
                    policy.actions = actions;
                    this.props.formUpdateObj("policy_form", policy);
                  }}
                />
              </SelectWrapper>
            </div>
          </div>
        );
      case "log":
        return null;
      default:
        return null;
    }
  }

  render() {
    return (
      <Card>
        <Title title={"Actions"} button={false} />
        <div className={`flex direction-column justify-start`}>
          {RestPolicy.ACTIONS.map((action, index) => (
            <div className={`flex direction-column justify-start`}>
              <CustomCheckbox
                number={action}
                label={<Label type={"item-label"} text={action} />}
                checked={this.props.policy_form.actions[action].selected !== false}
                customhandler={this.handleActionSelect}
              />
              <div style={{ marginBottom: "20px" }}>
                <Label type={"description"} text={ACTION_DESCRIPTIONS[action]} />
              </div>
              {this.props.policy_form.actions[action].selected !== false && this.buildActionForm(action)}
              <div>
                <hr />
              </div>
            </div>
          ))}
        </div>
      </Card>
    );
  }
}

ActionsContainer.propTypes = {
  policy_form: PropTypes.any.isRequired,
  rest_api: PropTypes.any.isRequired
};

ActionsContainer.defaultProps = {};

const mapStatetoProps = state => {
  return {
    ...mapPolicyFormStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapFormDispatchToPros(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default connect(mapStatetoProps, mapDispatchtoProps)(ActionsContainer);
