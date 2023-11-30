import React from "react";
import { Tag, Input, Tooltip, Icon } from "antd";
import * as PropTypes from "prop-types";
import "./ant-editable-tag.style.scss";

class EditableTagComponent extends React.Component {
  state = {
    inputVisible: false,
    inputValue: "",
    editInputIndex: -1,
    editInputValue: ""
  };

  handleClose = removedTag => {
    const tags = this.props.tags.filter(tag => tag !== removedTag);
    console.log(tags);
    this.props.onTagsChange(tags);
  };

  showInput = () => {
    this.setState({ inputVisible: true }, () => this.input.focus());
  };

  handleInputChange = e => {
    this.setState({ inputValue: e.target.value });
  };

  handleInputConfirm = () => {
    const { inputValue } = this.state;
    let { tags } = this.props;
    if (inputValue && tags.indexOf(inputValue) === -1) {
      tags = [...tags, inputValue];
    }
    console.log(tags);
    this.setState({
      inputVisible: false,
      inputValue: ""
    });
    this.props.onTagsChange(tags);
  };

  handleEditInputChange = e => {
    this.setState({ editInputValue: e.target.value });
  };

  handleEditInputConfirm = () => {
    this.setState(({ editInputIndex, editInputValue }) => {
      const newTags = [...this.props.tags];
      newTags[editInputIndex] = editInputValue;

      this.props.onTagsChange(newTags);
      return {
        editInputIndex: -1,
        editInputValue: ""
      };
    });
  };

  saveInputRef = input => {
    this.input = input;
  };

  saveEditInputRef = input => {
    this.editInput = input;
  };

  render() {
    const { inputVisible, inputValue, editInputIndex, editInputValue } = this.state;
    const { tags } = this.props;
    return (
      <>
        {tags.map((tag, index) => {
          if (editInputIndex === index) {
            return (
              <Input
                ref={this.saveEditInputRef}
                key={tag}
                className="tag-input"
                value={editInputValue}
                onChange={this.handleEditInputChange}
                onBlur={this.handleEditInputConfirm}
                onPressEnter={this.handleEditInputConfirm}
              />
            );
          }

          const isLongTag = tag.length > 20;

          const tagElem = (
            <Tag
              className={`edit-tag ${this.props.className}`}
              key={tag}
              closable={true}
              onClose={() => this.handleClose(tag)}>
              <span
                onDoubleClick={e => {
                  if (index !== 0) {
                    this.setState({ editInputIndex: index, editInputValue: tag }, () => {
                      this.editInput.focus();
                    });
                    e.preventDefault();
                  }
                }}>
                {isLongTag ? `${tag.slice(0, 20)}...` : tag}
              </span>
            </Tag>
          );
          return isLongTag ? (
            <Tooltip title={tag} key={tag}>
              {tagElem}
            </Tooltip>
          ) : (
            tagElem
          );
        })}
        {inputVisible && (
          <Input
            ref={this.saveInputRef}
            type="text"
            className="tag-input"
            value={inputValue}
            onChange={this.handleInputChange}
            onBlur={this.handleInputConfirm}
            onPressEnter={this.handleInputConfirm}
          />
        )}
        {!inputVisible && (
          <Tag className="site-tag-plus" onClick={this.showInput}>
            <Icon type={"plus"} /> {this.props.tagLabel}
          </Tag>
        )}
      </>
    );
  }
}

EditableTagComponent.propTypes = {
  className: PropTypes.string,
  tags: PropTypes.array.isRequired,
  onTagsChange: PropTypes.func.isRequired,
  tagLabel: PropTypes.string
};

EditableTagComponent.defaultProps = {
  tags: [],
  tagLabel: "Add Tag"
};

export default EditableTagComponent;
