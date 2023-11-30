import React from "react";
import { Form, Select } from "antd";
import { SelectRestapiHelperWrapper as SelectRestapi } from "shared-resources/helpers/select-restapi/select-restapi.helper.wrapper";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { isEqual } from "lodash";
import { getAssessmentCheckData } from "reduxConfigs/selectors/restapiSelector";

const { Option } = Select;

export class AssessmentCheckComponent extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      assessment_loading: false,
      sections_loading: false,
      value: {},
      additional_option: ""
    };
    this.handleAdditionalOption = this.handleAdditionalOption.bind(this);
  }

  static get defaultState() {
    return {
      sections_loading: false,
      assessment_loading: false,
      value: {},
      additional_option: ""
    };
  }

  componentWillUnmount() {
    console.log("this component is unmounting");
  }

  static getDerivedStateFromProps(props, state) {
    if (!isEqual(props.value, state.value)) {
      // new set of data, act accordingly
      const propAssessment = props.value ? props.value.assessment_template_id : undefined;
      const stateAssessment = state.value ? state.value.assessment_template_id : undefined;

      if (propAssessment) {
        if (propAssessment !== stateAssessment) {
          console.log("Assessment id has changed, get new questionnaire");
          props.genericGet("questionnaires", propAssessment);
        }
      }
      return {
        ...AssessmentCheckComponent.defaultState,
        value: { ...props.value },
        assessment_loading: propAssessment !== stateAssessment || props.questionnaire.loading
      };
    }

    if (state.assessment_loading) {
      if (!props.questionnaire.loading && !props.questionnaire.error) {
        const assessment = props.questionnaire.data || {};
        assessment.sections.forEach(section => props.genericGet("sections", section));
        return {
          ...state,
          assessment_select: { label: assessment.name, key: assessment.id },
          assessment_loading: false,
          sections_loading: true
        };
      }
    }

    if (state.sections_loading) {
      const sections = props.sections;
      const loading = props.sections.reduce((acc, obj) => {
        return acc || obj.loading;
      }, false);
      console.log(loading);
      if (!loading) {
        const question =
          sections
            .reduce((questions, section) => {
              questions.push(section.data.questions || []);
              return questions;
            }, [])
            .find(question => question.id === props.value.question_id) || {};
        return {
          ...state,
          question_select: { label: question.name, value: question.id },
          answers: props.value.answers,
          sections_loading: false
        };
      }
    }

    return null;
  }

  get questions() {
    const { sections } = this.props;
    const loading = sections.reduce((acc, obj) => {
      return acc || obj.loading;
    }, false);
    if (loading) {
      return [];
    }
    return sections.reduce((questions, section) => {
      const sectionData = section.data || {};
      if (sectionData.questions) {
        sectionData.questions.forEach(question => {
          questions.push({ label: question.name, key: question.id });
        });
      }
      return questions;
    }, []);
  }

  get questionOptions() {
    const { sections } = this.props;
    if (!this.props.value.question_id) {
      return [];
    }
    const loading = sections.reduce((acc, obj) => {
      return acc || obj.loading;
    }, false);
    if (loading) {
      return [];
    }
    const selectedQuestion =
      sections
        .reduce((questions, section) => {
          const newQuestions = section.data.questions || [];
          questions.push(...newQuestions);
          return questions;
        }, [])
        .find(q => q.id === this.props.value.question_id) || {};
    if (!selectedQuestion.options) {
      return [];
    }
    let returnOptions = selectedQuestion.options.map(opt => (opt.value === "" || !opt.value ? "true" : opt.value));
    if (
      this.state.additional_option !== "" &&
      this.state.additional_option !== undefined &&
      returnOptions.find(option => option.includes(this.state.additional_option)) === undefined
    ) {
      returnOptions.unshift(this.state.additional_option);
    }
    return returnOptions;
  }

  get assessmentSelect() {
    const questionnaire = this.props.questionnaire.data || {};
    return { label: questionnaire.name, key: questionnaire.id };
  }

  get questionSelect() {
    const question =
      this.props.sections
        .reduce((questions, section) => {
          if (section && !section.loading && section.data) {
            const q = section.data.questions || [];
            questions.push(...q);
          }
          return questions;
        }, [])
        .find(question => question.id === this.props.value.question_id) || {};
    return { label: question.name, key: question.id };
  }

  get answers() {
    return this.props.value.answers || [];
  }

  handleAdditionalOption(value) {
    console.log(value);
    this.setState({ additional_option: value });
  }

  render() {
    const { layout, onChange } = this.props;
    return (
      <Form layout={layout}>
        <Form.Item label={"Assessment"} required={true}>
          <SelectRestapi
            uri={"questionnaires"}
            method={"list"}
            mode={"single"}
            labelInValue={true}
            value={this.assessmentSelect}
            loading={this.props.questionnaire.loading}
            allowClear={false}
            onChange={value => {
              let propsVal = { ...this.props.value };
              // on template change, selected question is invalid
              propsVal.assessment_template_id = value.key;
              propsVal.question_id = undefined;
              onChange(propsVal);
            }}
          />
        </Form.Item>
        <Form.Item label={"Questions"} required={true}>
          <Select
            loading={this.state.sections_loading}
            disabled={this.assessmentSelect.key === undefined}
            mode={"default"}
            labelInValue={true}
            showSearch={true}
            allowClear={false}
            value={this.questionSelect}
            onChange={value => {
              let propsVal = this.props.value;
              // on question change, the answers selected have to be reset
              propsVal.question_id = value.key;
              propsVal.answers = [];
              onChange(propsVal);
            }}>
            {this.questions.map(question => (
              <Option key={question.key}>{question.label}</Option>
            ))}
          </Select>
        </Form.Item>
        <Form.Item label={"Answers"} required={true}>
          <Select
            loading={this.state.sections_loading || this.questionSelect.key === undefined}
            disabled={this.questionSelect.key === undefined}
            value={this.answers}
            onSearch={this.handleAdditionalOption}
            onChange={value => {
              let propsVal = this.props.value;
              propsVal.answers = value;
              onChange(propsVal);
            }}
            mode={"multiple"}>
            {this.questionOptions.map(option => (
              <Option key={option}>{option}</Option>
            ))}
          </Select>
        </Form.Item>
      </Form>
    );
  }
}

AssessmentCheckComponent.propTypes = {
  onChange: PropTypes.func,
  layout: PropTypes.string.isRequired,
  value: PropTypes.object.isRequired
};

AssessmentCheckComponent.defaultProps = {
  layout: "vertical",
  value: {
    assessment_template_id: undefined,
    question_id: undefined,
    answers: []
  }
};

const mapStateToProps = (state, ownProps) => ({
  ...getAssessmentCheckData(state, ownProps)
});

export default connect(mapStateToProps, mapGenericToProps)(AssessmentCheckComponent);
