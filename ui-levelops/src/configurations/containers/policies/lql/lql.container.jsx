import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { AntCard, Modal } from "shared-resources/components";
import { mapPolicyFormStatetoProps, mapFormDispatchToPros } from "reduxConfigs/maps/formMap";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import RegularTable from "components/Table/RegularTable";
import { LQLArray } from "shared-resources/helpers";

export class LQLContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      show_lql_guide: false
    };
    this.handleLQLAdd = this.handleLQLAdd.bind(this);
    this.handleLQLDelete = this.handleLQLDelete.bind(this);
    this.handleLQLChange = this.handleLQLChange.bind(this);
    this.handleShowLQLGuide = this.handleShowLQLGuide.bind(this);
    this.buildLQLInfoModal = this.buildLQLInfoModal.bind(this);
  }

  handleLQLDelete(index) {
    console.log(index);
    let policy = this.props.policy_form;
    let lqls = policy.lqls;
    lqls[index] = { query: undefined, valid: true };
    //lqls.splice(index,1);
    policy.lqls = lqls;
    this.props.formUpdateObj("policy_form", policy);
  }

  handleLQLAdd() {
    let policy = this.props.policy_form;
    let lqls = policy.lqls;
    lqls.push({ query: "", valid: false });
    policy.lqls = lqls;
    this.props.formUpdateObj("policy_form", policy);
  }

  handleLQLChange(query, index, result) {
    let policy = this.props.policy_form;
    let lqls = policy.lqls;
    lqls[index] = { query: query, valid: !result.isError };
    policy.lqls = lqls;
    this.props.formUpdateObj("policy_form", policy);
  }

  handleShowLQLGuide(e) {
    e.preventDefault();
    this.setState({ show_lql_guide: true });
  }

  buildLQLInfoModal() {
    return (
      <Modal
        title={"LQL Guide"}
        onCloseEvent={() => {
          this.setState({ show_lql_guide: false });
        }}>
        <div>
          <RegularTable
            th={["Predicates", "Description"]}
            tds={[
              [
                <div align={"left"}>issue.type</div>,
                <div align={"left"}>Bug, Epic, Task or any other Jira Issue type</div>
              ],
              [<div align={"left"}>issue.title</div>, <div align={"left"}>Jira Issue title</div>],
              [<div align={"left"}>issue.status</div>, <div align={"left"}>Jira Issue status - open, closed, etc</div>],
              [<div align={"left"}>issue.assignee</div>, <div align={"left"}>Jira Issue Assignee</div>],
              [<div align={"left"}>issue.reporter</div>, <div align={"left"}>Jira Issue Reporter</div>],
              [<div align={"left"}>issue.fix_version</div>, <div align={"left"}>Jira Issue Fix Version string</div>],
              [<div align={"left"}>issue.component</div>, <div align={"left"}>Jira Issue Component</div>],
              [<div align={"left"}>issue.label</div>, <div align={"left"}>Jira Issue Label</div>],
              [
                <div align={"left"}>issue.created_at</div>,
                <div align={"left"}>Needs to be a string of format "2020-01-01T10:11:12Z"</div>
              ],
              [
                <div align={"left"}>issue.updated_at</div>,
                <div align={"left"}>Needs to be a string of format "2020-01-01T10:11:12Z"</div>
              ]
            ]}
          />
          <RegularTable
            th={["Operators", "Description"]}
            tds={[
              [<div align={"left"}>=</div>, <div align={"left"}>equals. Exact Match</div>],
              [<div align={"left"}>!=</div>, <div align={"left"}>does not equal</div>],
              [<div align={"left"}>~</div>, <div align={"left"}>partial name. Used to match substrings</div>],
              [
                <div align={"left"}>in</div>,
                <div align={"left"}>values matches one of the array values. Ex: issue.status in [Bug,Epic]</div>
              ],
              [
                <div align={"left"}>nin</div>,
                <div align={"left"}>value is not present in the array. Ex: issue.status nin [Bug,Task]</div>
              ]
            ]}
          />
        </div>
      </Modal>
    );
  }

  render() {
    return (
      <AntCard title="LQLs">
        {this.state.show_lql_guide && this.buildLQLInfoModal()}
        <div className={`flex direction-column align-left`} style={{ width: "100%" }}>
          <div>
            <a href={"#"} onClick={this.handleShowLQLGuide}>
              LQL Guide
            </a>
          </div>

          <LQLArray
            lqls={this.props.policy_form.lqls.map(lql => lql.query)}
            onChange={this.handleLQLChange}
            onAdd={this.handleLQLAdd}
            onDelete={this.handleLQLDelete}
          />
        </div>
      </AntCard>
    );
  }
}

LQLContainer.propTypes = {
  policy_form: PropTypes.any.isRequired,
  rest_api: PropTypes.any.isRequired
};

LQLContainer.defaultProps = {};

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

export default connect(mapStatetoProps, mapDispatchtoProps)(LQLContainer);
