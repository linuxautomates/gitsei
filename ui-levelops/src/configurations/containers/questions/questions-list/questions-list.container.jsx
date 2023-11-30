import React from "react";
import * as PropTypes from "prop-types";
import { List, Dropdown, Menu, Icon, Checkbox, Radio, Input, Upload, Modal } from "antd";
import { AntButton, AntText, AntRow, AntCol } from "shared-resources/components";
import { Question } from "configurations/containers/questions";
import { RestQuestion } from "classes/RestQuestionnaire";
import { ReorderableList } from "shared-resources/containers";
import { DragDrop } from "../sections-list/dragDrop";

const { TextArea } = Input;

export default class QuestionsListContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      display_edit_index: undefined,
      edit_question: undefined
    };
    this.previewQuestion = this.previewQuestion.bind(this);
    this.handleUpdateQuestion = this.handleUpdateQuestion.bind(this);
    this.editModal = this.editModal.bind(this);
    this.moveRow = this.moveRow.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (props.activeKey !== undefined) {
      if (props.activeKey !== state.display_edit_index) {
        let newQuestion = new RestQuestion(props.questions[props.activeKey].json());
        newQuestion.options = [...newQuestion.options];
        return {
          ...state,
          display_edit_index: props.activeKey,
          edit_question: newQuestion
        };
      }
    }

    if (props.addingNewQuestion && !state.edit_question) {
      return {
        ...state,
        edit_question: props.newQuestion
      };
    }

    return null;
  }

  handleUpdateQuestion() {
    return question => {
      let newQuestion = new RestQuestion(question.json());
      this.setState({ edit_question: newQuestion });
    };
  }

  editModal() {
    return (
      <Modal
        title="Edit Question"
        visible={this.state.display_edit_index !== undefined || this.props.addingNewQuestion}
        maskClosable={true}
        icon={"setting"}
        onOk={e => {
          this.props.onModalClose();
          if (this.state.edit_question !== undefined) {
            let questions = this.props.questions;
            if (this.props.addingNewQuestion) {
              questions.push(this.state.edit_question);
            } else {
              questions[this.state.display_edit_index] = this.state.edit_question;
            }
            this.props.onChange(questions);
          }
          this.setState({ display_edit_index: undefined, edit_question: undefined });
        }}
        onCancel={e => {
          this.props.onModalClose();
          this.setState({ display_edit_index: undefined, edit_question: undefined });
        }}
        okButtonProps={{
          disabled: this.state.edit_question === undefined || !this.state.edit_question.valid()
        }}
        cancelText={""}
        closable={false}
        width={"700px"}>
        {(this.state.display_edit_index !== undefined || this.props.addingNewQuestion) && (
          <Question
            question={this.state.edit_question}
            onChange={this.handleUpdateQuestion()}
            sectionType={this.props.sectionType}
            riskEnabled={this.props.riskEnabled}
          />
        )}
      </Modal>
    );
  }

  previewQuestion(question, index) {
    const style = {
      display: "block",
      //height: '30px',
      lineHeight: "30px",
      marginRight: "10px",
      whiteSpace: "pre-line",
      wordBreak: "break-word"
    };
    const questionName = (
      <AntCol span={24}>
        <DragDrop className="drag-icon" />
        <AntText
          style={{ whiteSpace: "pre-line", wordBreak: "break-word" }}
          //editable={{onChange: this.handleValueUpdate("name", index)}}
        >
          {question.name}
        </AntText>
        {question.required && <AntText style={{ color: "red", fontSize: "14px" }}> *</AntText>}
      </AntCol>
    );
    switch (question.type) {
      case "checklist":
        return (
          <AntRow gutter={[10, 10]}>
            <Checkbox.Group style={{ width: "100%", margin: "10px" }}>
              <Checkbox key={question.order} style={style}>
                <AntText style={{ whiteSpace: "pre-line", wordBreak: "break-word" }}>{question.name}</AntText>
              </Checkbox>
            </Checkbox.Group>
          </AntRow>
        );
      case "multi-select":
        return (
          <AntRow gutter={[10, 10]}>
            {questionName}
            <AntCol span={24}>
              <Checkbox.Group style={{ width: "100%" }} value={[]}>
                <AntRow gutter={[10, 10]}>
                  {question.options.map((option, index) => (
                    <AntCol span={24} key={index}>
                      <Checkbox style={style}>
                        {option.editable && <Input style={{ width: 250, marginLeft: 10 }} value={option.value} />}
                        {option.editable !== true && option.value}
                      </Checkbox>
                    </AntCol>
                  ))}
                </AntRow>
              </Checkbox.Group>
            </AntCol>
          </AntRow>
        );
      case "boolean":
      case "single-select":
        return (
          <AntRow gutter={[10, 10]}>
            {questionName}
            <AntCol span={24}>
              <Radio.Group style={{ width: "100%" }} value={""}>
                {question.options.map(option => (
                  <Radio style={style} value={option.value} key={option.value}>
                    {option.editable && <Input style={{ width: 250, marginLeft: 10 }} value={option.value} />}
                    {option.editable !== true && option.value}
                  </Radio>
                ))}
              </Radio.Group>
            </AntCol>
          </AntRow>
        );
      case "text":
        return (
          <AntRow gutter={[10, 10]} style={{ width: "100%" }}>
            {questionName}
            <AntCol span={24}>
              <TextArea
                type={"textarea"}
                style={{ width: "100%" }}
                disabled={true}
                placeholder="Respondents will type their answers here."
              />
            </AntCol>
          </AntRow>
        );
      case "file upload":
        return (
          <AntRow gutter={[10, 10]}>
            {questionName}
            <AntCol span={24}>
              <Upload>
                <AntButton disabled>
                  <Icon type="upload" />
                  Upload File
                </AntButton>
              </Upload>
            </AntCol>
          </AntRow>
        );
      default:
    }
  }

  moveRow(dragIndex, hoverIndex) {
    const { questions } = this.props;
    let element = questions[dragIndex];
    questions.splice(dragIndex, 1);
    questions.splice(hoverIndex, 0, element);
    questions.forEach((question, index) => {
      question.number = index;
    });
    this.props.onChange(questions);
  }

  render() {
    return (
      <>
        {this.editModal()}
        {this.props.questions.length > 0 && (
          <ReorderableList
            className="bg-white ant-list-custom template-question-list"
            style={{ margin: "5px" }}
            dataSource={this.props.questions}
            itemLayout="vertical"
            moveCard={this.moveRow}
            renderItem={(item, index) => {
              return (
                <div id={index} key={index}>
                  <List.Item
                    className={"ant-card-section-list-item"}
                    key={index}
                    actions={
                      this.props.preview === true
                        ? []
                        : [
                            <Dropdown
                              key={"dropdown-item-1"}
                              trigger={["click"]}
                              overlay={
                                <Menu>
                                  <Menu.Item key={"menu-item-1"} onClick={this.props.onDelete(index)}>
                                    <Icon type={"delete"} /> Delete
                                  </Menu.Item>
                                  <Menu.Item
                                    key={"menu-item-2"}
                                    onClick={e => {
                                      let newQuestion = new RestQuestion(this.props.questions[index].json());
                                      newQuestion.options = [...newQuestion.options];
                                      this.setState({
                                        display_edit_index: index,
                                        edit_question: newQuestion
                                      });
                                    }}>
                                    <Icon type={"edit"} /> Edit
                                  </Menu.Item>
                                </Menu>
                              }
                              placement="bottomRight">
                              <Icon type={"more"} style={{ fontSize: "20px" }} />
                            </Dropdown>
                          ]
                    }>
                    {this.previewQuestion(item, index)}
                  </List.Item>
                </div>
              );
            }}
          />
        )}
      </>
    );
  }
}

QuestionsListContainer.propTypes = {
  questions: PropTypes.array,
  onChange: PropTypes.func.isRequired,
  riskEnabled: PropTypes.bool,
  sectionType: PropTypes.string,
  onDelete: PropTypes.func.isRequired,
  activeKey: PropTypes.number,
  onModalClose: PropTypes.func.isRequired,
  addingNewQuestion: PropTypes.bool,
  newQuestion: PropTypes.any
};

QuestionsListContainer.defaultProps = {
  questions: []
};
