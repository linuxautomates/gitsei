import React from "react";
import * as PropTypes from "prop-types";
import { AntButton, AntCard, AntCol, AntRow, AntEditableInput } from "shared-resources/components";
import { QuestionsList } from "configurations/containers/questions";
import { RestQuestion } from "../../../../classes/RestQuestionnaire";
import { Dropdown, Form, Icon, Menu, Select, Spin, Typography } from "antd";
import uuidv1 from "uuid/v1";
import classnames from "classnames";
import "./section.style.scss";
import { isString } from "lodash";

const { Option } = Select;
const { Text, Paragraph } = Typography;

export class SectionContainer extends React.Component {
  constructor(props) {
    super(props);
    let loading = false;

    this.state = {
      loading: loading,
      expand_panel: 0,
      active_key: undefined,
      isEditingName: false,
      isEditingDescription: false,
      addingNewQuestion: false,
      newQuestion: undefined
    };

    this.handleFieldUpdate = this.handleFieldUpdate.bind(this);
    this.handleValueUpdate = this.handleValueUpdate.bind(this);
    this.addQuestion = this.addQuestion.bind(this);
    this.addQuestionType = this.addQuestionType.bind(this);
    this.handleQuestionDelete = this.handleQuestionDelete.bind(this);
    this.handleUpdateQuestion = this.handleUpdateQuestion.bind(this);
    this.handleUpdateQuestions = this.handleUpdateQuestions.bind(this);
  }

  get questions() {
    return this.props.section.questions.sort((a, b) => {
      return a.number - b.number;
    });
  }

  handleFieldUpdate(field) {
    return e => {
      let section = this.props.section;
      section[field] = e.target.value;
      this.props.onChange(section);
    };
  }

  handleValueUpdate(field) {
    return value => {
      let section = this.props.section;
      section[field] = field === "name" && isString(value) ? value.trim() : value;
      if (field === "description") {
        this.setState({ isEditingDescription: false });
      }
      if (field === "name") {
        this.setState({ isEditingName: false });
      }
      this.props.onChange(section);
    };
  }

  addQuestion() {
    let assertions = this.questions;
    let newQuestion = new RestQuestion();
    newQuestion.name = "";
    newQuestion.options = [];
    newQuestion.training = [];
    newQuestion.verification_assets = [];
    newQuestion.verifiable = true;
    newQuestion.verification_mode = RestQuestion.VERIFICATION_MODES[0];
    newQuestion.type =
      this.props.section.type === "checklist" || this.props.section.type === "CHECKLIST"
        ? "checklist"
        : RestQuestion.TYPES[0];
    newQuestion.custom = true;
    newQuestion.number = assertions.length + 1;
    if (newQuestion.type === "checklist") {
      newQuestion.options = [{ score: RestQuestion.SCORES[0], value: "" }];
    }
    assertions.push(newQuestion);
    let section = this.props.section;
    section.questions = assertions;
    this.setState({ active_key: assertions.length - 1 }, this.props.onChange(section));
  }

  addQuestionType(type) {
    return e => {
      let assertions = this.questions;
      let newQuestion = new RestQuestion();
      newQuestion.name = `Question ${assertions.length + 1}`;
      newQuestion.options = [];
      newQuestion.training = [];
      newQuestion.verification_assets = [];
      newQuestion.verifiable = true;
      newQuestion.verification_mode = RestQuestion.VERIFICATION_MODES[0];
      newQuestion.type =
        this.props.section.type === "checklist" || this.props.section.type === "CHECKLIST" ? "checklist" : type;
      newQuestion.custom = true;
      newQuestion.number = assertions.length + 1;
      if (newQuestion.type === "checklist") {
        newQuestion.options = [{ score: RestQuestion.SCORES[0], value: "" }];
      } else if (newQuestion.type.includes("select")) {
        newQuestion.options = [
          { score: RestQuestion.SCORES[0], value: "option 1" },
          { score: RestQuestion.SCORES[0], value: "option 2" }
        ];
      } else if (type === "boolean") {
        newQuestion.options = [
          { value: "yes", score: 1 },
          { value: "no", score: 3 }
        ];
      } else {
        newQuestion.options = [{ value: "", score: 1 }];
      }
      this.setState({ addingNewQuestion: true, newQuestion });
    };
  }

  handleQuestionDelete(index) {
    return e => {
      let questions = this.questions;
      questions.splice(index, 1);
      questions.forEach((question, index) => {
        question.order = index + 1;
      });
      let section = this.props.section;
      section.questions = questions;
      this.setState({ expand_panel: undefined }, () => this.props.onChange(section));
    };
  }

  handleUpdateQuestions(questions) {
    let section = this.props.section;
    section.questions = questions;
    this.props.onChange(section);
  }

  handleUpdateQuestion(index) {
    return question => {
      let section = this.props.section;
      let questions = section.questions;
      questions[index] = question;
      section.questions = questions;
      this.props.onChange(section);
    };
  }

  render() {
    const { section, cardClassNames } = this.props;
    if (section === undefined) {
      return <Spin />;
    }

    const menu = (
      <Menu>
        {RestQuestion.TYPES.map(
          (type, index) =>
            !(
              (section.type.toLowerCase() === "checklist" && type !== "checklist") ||
              (section.type.toLowerCase() !== "checklist" && type === "checklist")
            ) && (
              <Menu.Item key={index} onClick={this.addQuestionType(type)}>
                <Icon type={RestQuestion.ICON_MAP[type].icon} />
                <span>
                  {/*{type.replace("_", " ").toUpperCase()}*/}
                  {RestQuestion.ICON_MAP[type].name}
                </span>
              </Menu.Item>
            )
        )}
      </Menu>
    );

    return (
      <AntCard
        className={classnames("template-section-container", cardClassNames)}
        title={
          this.props.preview !== true && (
            <>
              {(section.name === "" || section.questions.length === 0) && (
                <React.Fragment>
                  <Icon
                    type={"exclamation-circle"}
                    theme={"filled"}
                    style={{ color: "red", fontSize: "16px", marginRight: "10px" }}
                  />
                  <Text type={"danger"} style={{ fontSize: "14px" }}>
                    {`Add a name and some questions`}
                  </Text>
                </React.Fragment>
              )}

              {!section.name && (
                <div>
                  <Text type={"secondary"}>Section Name</Text>
                </div>
              )}
              <AntEditableInput
                onPressEnter={() => this.setState({ isEditingName: false })}
                editMode={!section.name || this.state.isEditingName}
                onChange={this.handleValueUpdate("name")}
                onStart={() => this.setState({ isEditingName: true })}
                value={section.name}
              />

              {section.description === "" && (
                <>
                  <div style={{ minHeight: "13px", minWidth: "1px" }} />
                  <Text type={"secondary"}>Description</Text>
                </>
              )}
              <Paragraph
                type={"secondary"}
                className="section-description"
                ellipsis={true}
                // onPressEnter={() => {
                //   console.log("Setting edit false on enter");
                //   this.setState({ isEditingDescription: false });
                // }}
                editable={{
                  editing: !section.description || this.state.isEditingDescription,
                  onChange: this.handleValueUpdate("description"),
                  onStart: () => {
                    console.log("setting edit true on start");
                    this.setState({ isEditingDescription: true });
                  }
                }}>
                {section.description}
              </Paragraph>
            </>
          )
        }
        extra={
          this.props.preview !== true && (
            <Select
              style={{ width: "150px" }}
              placeholder="type"
              showSearch={true}
              filterOption={true}
              allowClear={false}
              showArrow={true}
              notFoundContent={null}
              labelInValue={true}
              value={{
                label: section.type.toUpperCase(),
                key: section.type
              }}
              onChange={option => {
                section.type = option.key;
                section.questions = [];
                this.props.onChange(section);
              }}>
              <Option key={"default"}>DEFAULT</Option>
              <Option key={"checklist"}>CHECKLIST</Option>
            </Select>
          )
        }
        style={{ marginBottom: "10px" }}>
        <QuestionsList
          questions={[...(this.questions || [])]}
          onDelete={this.handleQuestionDelete}
          onChange={this.handleUpdateQuestions}
          sectionType={this.props.section.type}
          riskEnabled={this.props.section.risk_enabled}
          activeKey={this.state.active_key}
          preview={this.props.preview}
          onModalClose={() =>
            this.setState({ active_key: undefined, addingNewQuestion: false, newQuestion: undefined })
          }
          addingNewQuestion={this.state.addingNewQuestion}
          newQuestion={this.state.newQuestion}
        />
        {this.props.preview !== true && (
          <AntRow gutter={[5, 5]}>
            <AntCol span={24}>
              <Dropdown trigger={["click"]} overlay={menu}>
                <AntButton icon={"plus"} autoFocus={true} size={"large"} type={"dashed"} block>
                  Add question to your section
                </AntButton>
              </Dropdown>
            </AntCol>
          </AntRow>
        )}
      </AntCard>
    );
  }
}

SectionContainer.propTypes = {
  className: PropTypes.string,
  section: PropTypes.object,
  onChange: PropTypes.func.isRequired,
  riskEnabled: PropTypes.bool,
  preview: PropTypes.bool
};

const Section = Form.create({ name: `section_form-${uuidv1()}` })(SectionContainer);

export default Section;
