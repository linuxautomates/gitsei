import React from "react";
import { Form, Button, Row, Col, Divider } from "antd";
import * as PropTypes from "prop-types";
import { AutosuggestContainer as AutoSuggest } from "../../autosuggest/autosuggest.container";
import "./kv.component.scss";

export class KVComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.handleKeyChange = this.handleKeyChange.bind(this);
    this.handleValueChange = this.handleValueChange.bind(this);
    this.handleAdd = this.handleAdd.bind(this);
    this.handleDelete = this.handleDelete.bind(this);
  }

  handleKeyChange(index) {
    const { onChange } = this.props;
    return e => {
      const { value } = this.props;
      value[index].key = e.currentTarget.value;
      console.log(value);
      onChange(value);
    };
  }

  handleValueChange(index) {
    const { onChange } = this.props;
    return e => {
      const { value } = this.props;
      value[index].value = e.currentTarget.value;
      onChange(value);
    };
  }

  handleKeyAutoChange(index) {
    const { onChange } = this.props;
    return val => {
      const { value } = this.props;
      value[index].key = val;
      console.log(value);
      onChange(value);
    };
  }

  handleValueAutoChange(index) {
    const { onChange } = this.props;
    return val => {
      const { value } = this.props;
      value[index].value = val;
      onChange(value);
    };
  }

  handleDelete(index) {
    const { onChange } = this.props;
    return e => {
      const { value } = this.props;
      value.splice(index, 1);
      onChange(value);
    };
  }

  handleAdd(e) {
    const { onChange } = this.props;
    const { value } = this.props;
    value.push({ key: "", value: "" });
    onChange(value);
  }

  render() {
    const { value } = this.props;
    return (
      <Form layout={"vertical"}>
        <Form.Item label={"Key Value Pairs"} style={{ marginTop: "5px" }}>
          {value.map((entry, index) => {
            return (
              <Row gutter={[10, 10]}>
                <Col span={22}>KV pair {index + 1}</Col>
                <Col span={2}>
                  <Button icon={"delete"} type={"link"} onClick={this.handleDelete(index)} />
                </Col>

                <Col span={24}>
                  <label className="key-value-label">Key</label>
                  <AutoSuggest
                    placeholder={"Key"}
                    key={`key-${index}`}
                    value={entry.key}
                    onChange={this.handleKeyAutoChange(index)}
                    suggestions={this.props.suggestions}
                  />
                </Col>
                <Col span={24}>
                  <label className="key-value-label">Value</label>
                  <AutoSuggest
                    placeholder={"Value"}
                    key={`value-${index}`}
                    value={entry.value}
                    onChange={this.handleValueAutoChange(index)}
                    suggestions={this.props.suggestions}
                  />
                </Col>

                <Divider />
              </Row>
            );
          })}
        </Form.Item>
        <Form.Item>
          <Button type={"link"} icon={"plus"} onClick={this.handleAdd}>
            Add Key Value Pair
          </Button>
        </Form.Item>
      </Form>
    );
  }
}

KVComponent.propTypes = {
  onChange: PropTypes.func,
  layout: PropTypes.string.isRequired,
  value: PropTypes.array.isRequired
};

KVComponent.defaultProps = {
  layout: "vertical",
  value: []
};
