import React from "react";
import * as PropTypes from "prop-types";
import { Col, Dropdown, Icon, Input, Mentions, Menu, Row } from "antd";
import { insertAt } from "utils/stringUtils";

const { Search } = Input;
const { Option } = Mentions;

export class AutosuggestContainer extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      value: props.value,
      searchFilter: ""
    };
    this.onInputChange = this.onInputChange.bind(this);
    this.onMentionChange = this.onMentionChange.bind(this);
    this.onOptionSelect = this.onOptionSelect.bind(this);
    this.autoSuggestOptions = this.autoSuggestOptions.bind(this);
  }

  autoSuggestOptions() {
    const { suggestions, content_type } = this.props;
    const nodes = suggestions.reduce((arr, obj) => {
      if (!arr.includes(obj.node)) {
        arr.push(obj.node);
        return arr;
      }
      return arr;
    }, []);

    console.log(nodes);

    return (
      <>
        <Search
          style={{ marginLeft: "2px", marginRight: "2px", paddingLeft: "2px", paddingRight: "2px" }}
          placeholder="search available variables"
          size="middle"
          onChange={e => this.setState({ searchFilter: e.currentTarget.value })}
        />
        <Menu onClick={this.onOptionSelect} mode="inline">
          {nodes.map(node => (
            <Menu.ItemGroup key={node} title={node}>
              {suggestions
                .filter(
                  suggestion =>
                    suggestion.node === node &&
                    suggestion.key.includes(this.state.searchFilter) &&
                    (content_type === undefined || suggestion.content_type === content_type)
                )
                .map(menu => (
                  <Menu.Item key={menu.key}>{menu.value}</Menu.Item>
                ))}
            </Menu.ItemGroup>
          ))}
          {/*{suggestions.map((option, index) => (*/}
          {/*  <Menu.Item key={option}>{option}</Menu.Item>*/}
          {/*))}*/}
        </Menu>
      </>
    );
  }

  onInputChange(e) {
    this.setState(
      {
        value: e.target.value
      },
      () => this.props.onChange(this.state.value)
    );
  }

  onMentionChange(value) {
    this.setState(
      {
        value: value
      },
      () => this.props.onChange(this.state.value)
    );
  }

  onOptionSelect(e) {
    console.log(this.inputRef);
    const insertPosition = this.inputRef.rcMentions.textarea.selectionStart;
    const { value } = this.state;
    this.setState(
      {
        value: insertAt(value, e.key, insertPosition)
      },
      () => {
        const newCursor = insertPosition + e.key.length;
        this.inputRef.rcMentions.textarea.setSelectionRange(newCursor, newCursor);
        this.inputRef.focus();
        this.props.onChange(this.state.value);
      }
    );
  }

  render() {
    const { suggestions, text_type, content_type } = this.props;

    const inputSpan = suggestions.length > 0 ? 23 : 24;
    const inputType = text_type || "text";

    if (content_type !== undefined) {
      //console.log(`this is the content type for the field ${content_type}`);
      //console.log(suggestions);
    }

    const style = inputType === "text" ? { whiteSpace: "nowrap", boxShadow: "none" } : {};
    const additionalProps = {};
    if (this.props.onBlur) {
      additionalProps.onBlur = this.props.onBlur;
    }

    return (
      <>
        <Row align={"middle"} type={"flex"} justify={"space-between"} gutter={[2, 2]}>
          <Col span={inputSpan}>
            <Mentions
              placeholder={this.props.placeholder}
              className="ant-input"
              rows={inputType === "text" ? 1 : 6}
              style={{ width: "100%", ...style }}
              onChange={this.onMentionChange}
              value={this.state.value}
              ref={input => {
                this.inputRef = input;
              }}
              prefix={"$"}
              {...additionalProps}>
              {suggestions
                .filter(suggestion => content_type === undefined || suggestion.content_type === content_type)
                .map((suggestion, index) => (
                  <Option
                    key={index}
                    value={suggestion.key.replace("$", "")}>{`${suggestion.node} - ${suggestion.value}`}</Option>
                ))}
            </Mentions>
          </Col>
          {suggestions.length > 0 && (
            <Col span={1}>
              <Dropdown
                overlay={this.autoSuggestOptions}
                overlayStyle={{ maxHeight: "400px", overflow: "hidden", overflowY: "scroll" }}>
                <Icon type="plus-circle" theme="filled" style={{ fontSize: "20px" }} />
                {/*<Button icon={"plus"} />*/}
              </Dropdown>
            </Col>
          )}
        </Row>
      </>
    );
  }
}

AutosuggestContainer.propTypes = {
  onChange: PropTypes.func,
  suggestions: PropTypes.array.isRequired,
  value: PropTypes.string.isRequired
};

AutosuggestContainer.defaultProps = {
  suggestions: [],
  value: ""
};
