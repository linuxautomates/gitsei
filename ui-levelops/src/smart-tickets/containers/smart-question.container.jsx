import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { Conditions } from "shared-resources/containers";
import { mapFormDispatchToPros, mapSTTFormStatetoProps } from "reduxConfigs/maps/formMap";
import { Tabs, Form } from "antd";
import { RestQuestion } from "../../classes/RestQuestionnaire";

const { TabPane } = Tabs;

export class SmartQuestionContainer extends React.Component {
  constructor(props) {
    super(props);
    this.handleFieldUpdate = this.handleFieldUpdate.bind(this);
  }

  handleFieldUpdate(field) {
    return e => {
      let question = this.props.question_form;
      question[field] = e.target.value;
      this.props.formUpdateObj("question_form", question);
    };
  }

  render() {
    return (
      <>
        <Tabs defaultActiveKey={"Details"} size={"small"}>
          <TabPane tab={"Details"} key={"Details"}></TabPane>
          <TabPane tab={"Branching"} key={"Branching"}>
            <Conditions />
          </TabPane>
        </Tabs>
      </>
    );
  }
}

SmartQuestionContainer.propTypes = {
  question: PropTypes.object.isRequired,
  onChange: PropTypes.func.isRequired
};

SmartQuestionContainer.defaultProps = {
  question: new RestQuestion(),
  onChange: () => {}
};

const QuestionForm = Form.create({ name: "smart_question_form" })(SmartQuestionContainer);

const mapStatetoProps = state => {
  return {
    ...mapSTTFormStatetoProps(state),
    ...mapRestapiStatetoProps(state)
  };
};

const mapDispatchtoProps = dispatch => {
  return {
    ...mapFormDispatchToPros(dispatch),
    ...mapRestapiDispatchtoProps(dispatch)
  };
};

export default connect(mapStatetoProps, mapDispatchtoProps)(QuestionForm);
