import React from "react";
import * as PropTypes from "prop-types";
import { AntInput, AntButton } from "shared-resources/components";
import { RestQuestion } from "classes/RestQuestionnaire";
import { Typography, Form, Row, Col, Select, Input, Switch } from "antd";

const { Text } = Typography;
const { Option } = Select;
const { TextArea } = Input;

export class QuestionContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.addOption = this.addOption.bind(this);
    this.addEditableOption = this.addEditableOption.bind(this);
    this.onDeleteOption = this.onDeleteOption.bind(this);
    this.handleExpand = this.handleExpand.bind(this);
    this.handleFieldUpdate = this.handleFieldUpdate.bind(this);
    this.handleValueUpdate = this.handleValueUpdate.bind(this);
  }

  handleFieldUpdate(field) {
    return e => {
      let question = this.props.question;
      question[field] = e.target.value;
      this.props.onChange(question);
    };
  }

  handleValueUpdate(field) {
    return value => {
      let question = this.props.question;
      question[field] = value;
      this.props.onChange(question);
    };
  }

  handleOptionUpdate(index) {
    return e => {
      let question = this.props.question;
      let options = question.options;
      options[index] = {
        ...options[index],
        value: e.target.value
      };
      question.options = options;
      this.props.onChange(question);
    };
  }

  addOption(e) {
    e.preventDefault();
    let question = this.props.question;
    let options = question.options;
    if (options.find(option => option.editable === true)) {
      options.splice(Math.max(options.length - 1, 0), 0, { value: "", score: RestQuestion.SCORES[0] });
    } else {
      options.push({ value: "", score: RestQuestion.SCORES[0] });
    }
    question.options = options;
    this.props.onChange(question);
  }

  addEditableOption(e) {
    e.preventDefault();
    let question = this.props.question;
    let options = question.options;
    options.push({ value: "Other", score: RestQuestion.SCORES[0], editable: true });
    question.options = options;
    this.props.onChange(question);
  }

  onDeleteOption(index) {
    let question = this.props.question;
    let options = question.options;
    options.splice(index, 1);
    question.options = options;
    this.props.onChange(question);
  }

  buildOptions() {
    const options = this.props.question.options;
    const question = this.props.question;
    switch (question.type.toLowerCase()) {
      case "text":
      case "file upload":
      case "checklist":
        if (this.props.riskEnabled) {
          return (
            <Row gutter={[10, 10]}>
              <Form layout={"vertical"}>
                <Col span={4}>
                  <Form.Item label={"risk score"} colon={false} required={true}>
                    <Select
                      placeholder="score"
                      showSearch={false}
                      filterOption={true}
                      allowClear={false}
                      showArrow={true}
                      notFoundContent={null}
                      labelInValue={true}
                      disabled={!this.props.riskEnabled}
                      value={
                        options.length === 0
                          ? {}
                          : {
                              label: question.options[0].score,
                              key: question.options[0].score
                            }
                      }
                      onChange={option => {
                        // let question = this.props.section_form;
                        // let questions = question.questions;
                        question.options[0] = {
                          ...question.options[0],
                          score: option.key
                        };
                        this.props.onChange(question);
                      }}>
                      {RestQuestion.SCORES.map(score => (
                        <Option key={score}>{score === 0 ? "N/A" : score}</Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
              </Form>
            </Row>
          );
        } else {
          return "";
        }
      case "boolean":
      case "single-select":
      case "multi-select":
        console.log("building options");
        console.log(options);
        return options.map((option, index) => (
          <Row gutter={[10, 10]} align={"bottom"}>
            <Form layout={"vertical"}>
              <Col span={14}>
                <Form.Item label={option.editable ? `Other` : `Option ${index + 1}`} colon={false} required={true}>
                  <AntInput
                    disabled={question.type === "boolean" || option.editable === true}
                    onChange={this.handleOptionUpdate(index)}
                    value={question.options[index].value}
                  />
                </Form.Item>
              </Col>
              {this.props.riskEnabled && (
                <Col span={4}>
                  <Form.Item label={"risk score"} colon={false} required={true}>
                    <Select
                      placeholder="score"
                      showSearch={false}
                      filterOption={true}
                      allowClear={false}
                      showArrow={true}
                      notFoundContent={null}
                      labelInValue={true}
                      disabled={!this.props.riskEnabled}
                      value={
                        options.length === 0
                          ? {}
                          : {
                              label: question.options[index].score === 0 ? "N/A" : question.options[index].score,
                              key: question.options[index].score
                            }
                      }
                      onChange={option => {
                        question.options[index] = {
                          ...question.options[index],
                          score: option.key
                        };
                        this.props.onChange(question);
                      }}>
                      {RestQuestion.SCORES.map(score => (
                        <Option key={score}>{score === 0 ? "N/A" : score}</Option>
                      ))}
                    </Select>
                  </Form.Item>
                </Col>
              )}

              {question.type !== "boolean" && (
                <Col span={4}>
                  <Form.Item colon={false} label={""}>
                    <div style={{ marginTop: "20px" }}>
                      <AntButton
                        icon={"delete"}
                        onClick={e => {
                          this.onDeleteOption(index);
                        }}
                      />
                    </div>
                  </Form.Item>
                </Col>
              )}
            </Form>
          </Row>
        ));
    }
  }

  handleExpand(e) {
    e.preventDefault();
    this.props.onExpand();
  }

  render() {
    const { question, onDelete, isOpen, onExpand } = this.props;

    return (
      <>
        <Row gutter={[10, 10]}>
          <Form layout={"vertical"}>
            <Col span={24}>
              <Form.Item label={"Required"} key={"required"} colon={false}>
                <Switch checked={question.required} onChange={this.handleValueUpdate("required")} />
              </Form.Item>
            </Col>
            <Col span={24}>
              <Form.Item label={"Title"} key={"name"} colon={false} required={true}>
                <TextArea autoSize value={question.name} onChange={this.handleFieldUpdate("name")} />
              </Form.Item>
            </Col>
          </Form>
        </Row>
        {this.buildOptions()}
        {(question.type === "single-select" || question.type === "multi-select") && (
          <Row gutter={[10, 10]} align={"middle"}>
            <Col span={12}>
              <Row justify={"start"} type={"flex"} gutter={[10, 10]}>
                <Col>
                  <AntButton type={"dashed"} onClick={this.addOption} icon={"plus"}>
                    Option
                  </AntButton>
                </Col>
                <Col>
                  <AntButton
                    type={"dashed"}
                    onClick={this.addEditableOption}
                    icon={"plus"}
                    disabled={question.options.filter(opt => opt.editable === true).length > 0}>
                    Other
                  </AntButton>
                </Col>
                <Col>{question.options.length === 0 && <Text type={"danger"}>Need at least one option</Text>}</Col>
              </Row>
            </Col>
          </Row>
        )}
      </>
    );
  }
}

QuestionContainer.propTypes = {
  className: PropTypes.string,
  question: PropTypes.object.isRequired,
  index: PropTypes.number.isRequired,
  onDelete: PropTypes.func,
  onChange: PropTypes.func,
  onExpand: PropTypes.func,
  isOpen: PropTypes.bool.isRequired,
  sectionType: PropTypes.string
};

QuestionContainer.defaultProps = {
  className: "assertion",
  isOpen: false
};
