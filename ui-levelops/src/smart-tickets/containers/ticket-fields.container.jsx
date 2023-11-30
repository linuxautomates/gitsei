import React from "react";
import { Form, Select, Checkbox } from "antd";
import { AntInput } from "shared-resources/components";
import { FieldTypes } from "../../classes/FieldTypes";
import {
  DYNAMIC_MULTI_SELECT_TYPE,
  DYNAMIC_SINGLE_SELECT_TYPE,
  MULTI_SELECT_TYPE,
  RADIO_GROUP_TYPE,
  SINGLE_SELECT_TYPE
} from "constants/fieldTypes";

const { Option } = Select;

export class TicketFieldsContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.onChangeHandler = this.onChangeHandler.bind(this);
    this.onSelectChange = this.onSelectChange.bind(this);
    this.onRequiredChange = this.onRequiredChange.bind(this);
    this.onHiddenChange = this.onHiddenChange.bind(this);
  }

  onChangeHandler(name) {
    return e => {
      let field = this.props.field;
      field[name] = e.target.value;
      this.props.onChange(field);
    };
  }

  onSelectChange(name) {
    return value => {
      let field = this.props.field;
      field[name] = value;
      if (name === "dynamic_option") {
        field[name] = FieldTypes.DYNAMIC_SELECT[value].uri;
        if (FieldTypes.DYNAMIC_SELECT[value].searchField !== "name") {
          field.search_field = FieldTypes.DYNAMIC_SELECT[value].searchField;
        }
      }
      this.props.onChange(field);
    };
  }

  onRequiredChange(e) {
    let field = this.props.field;
    field.required = e.target.checked;
    this.props.onChange(field);
  }

  onHiddenChange(e) {
    let field = this.props.field;
    field.hidden = e.target.checked;
    this.props.onChange(field);
  }

  render() {
    const { field } = this.props;
    const dynamicOption = Object.keys(FieldTypes.DYNAMIC_SELECT).find(
      option => FieldTypes.DYNAMIC_SELECT[option].uri === field.dynamic_option
    );
    if (!field) {
      return "";
    }
    return (
      <Form layout={"vertical"}>
        <Form.Item label={"Required"} colon={false}>
          <Checkbox checked={field.required} onChange={this.onRequiredChange} />
        </Form.Item>
        <Form.Item label={"Hidden"} colon={false}>
          <Checkbox checked={field.hidden} onChange={this.onHiddenChange} />
        </Form.Item>
        <Form.Item label={"Label"} colon={false} required={true}>
          <AntInput value={field.display_name} onChange={this.onChangeHandler("display_name")} />
        </Form.Item>
        <Form.Item label={"Type"} colon={false}>
          <Select
            disabled={field.id !== undefined}
            value={field.type}
            style={{ width: "200px" }}
            mode={"single"}
            onChange={this.onSelectChange("type")}>
            {Object.keys(FieldTypes.TYPES).map(type => (
              <Option key={type}>{FieldTypes.TYPES[type].name}</Option>
            ))}
          </Select>
        </Form.Item>
        {[SINGLE_SELECT_TYPE, MULTI_SELECT_TYPE, RADIO_GROUP_TYPE].includes(field.type) && (
          <Form.Item label={"Options"} colon={false} required={true}>
            <Select
              value={field.options}
              style={{ width: "300px" }}
              mode={"tags"}
              showArrow={true}
              onChange={this.onSelectChange("options")}>
              {field.options.map(type => (
                <Option key={type}>{type}</Option>
              ))}
            </Select>
          </Form.Item>
        )}
        {field.type === "text" && (
          <Form.Item label={"Validation"} colon={false}>
            <Select
              value={field.validation}
              style={{ width: "200px" }}
              mode={"single"}
              onChange={this.onSelectChange("validation")}>
              {FieldTypes.VALIDATIONS.map(type => (
                <Option key={type}>{type.replace("_", " ").toUpperCase()}</Option>
              ))}
            </Select>
          </Form.Item>
        )}
        {[DYNAMIC_SINGLE_SELECT_TYPE, DYNAMIC_MULTI_SELECT_TYPE].includes(field.type) && (
          <Form.Item label={"Dynamic List"} colon={false} required={true}>
            <Select
              //value={field.dynamic_option}
              value={dynamicOption}
              disabled={field.id !== undefined}
              style={{ width: "300px" }}
              mode={"single"}
              showArrow={true}
              onChange={this.onSelectChange("dynamic_option")}>
              {Object.keys(FieldTypes.DYNAMIC_SELECT).map(type => (
                <Option key={type}>{type}</Option>
              ))}
            </Select>
          </Form.Item>
        )}
      </Form>
    );
  }
}
