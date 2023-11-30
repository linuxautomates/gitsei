import React from "react";
import { Form, Typography } from "antd";
import * as PropTypes from "prop-types";
import { GenericElementComponent } from "./generic-element.component";
import { DATE, ASSESSMENT_CHECK_TYPE, KV_TYPE } from "constants/fieldTypes";

const { Text } = Typography;

const getValueType = fieldType => {
  switch (fieldType) {
    case DATE:
      return "date";
    case KV_TYPE:
    case ASSESSMENT_CHECK_TYPE:
      return "json";
    default:
      return "string";
  }
};

export class GenericFormComponent extends React.Component {
  values = {};
  hiddenOptions = [];

  constructor(props) {
    super(props);

    this.elements.forEach(element => {
      this.values[element.key] = element.values
        ? element.values
        : {
            value: undefined,
            type: getValueType(element.type)
          };
      // setting options default value
      if (element.key === "options") {
        this.values[element.key]["value"] = [
          element.options.filter(option => option.hasOwnProperty("defaultValue")).map(option => option.value)
        ];
        this.hiddenOptions = element?.hiddenOptions || [];
        element.options = element.options.filter(option => !option?.isParentChecked);
        if (this.hiddenOptions.length) {
          const hiddenOptionsValues = [];
          this.hiddenOptions.forEach(hOption => {
            const options = hOption?.options ?? [];
            const parentValues = this.values.options?.value?.[0];
            const parentValueChecked = parentValues.includes(hOption.isParentChecked);
            options.forEach(op => {
              if (parentValueChecked) {
                hiddenOptionsValues.push(op.value);
              }
            });
          });
          this.hiddenOptions["value"] = hiddenOptionsValues;
        }
      }
    });
    this.state = {
      ...this.values,
      elements: props.elements,
      hiddenOptions: this.hiddenOptions
    };
  }

  static getDerivedStateFromProps(props, state) {
    if (state.elements !== props.elements) {
      let values = {};
      props.elements.forEach(element => {
        values[element.key] = element.values
          ? element.values
          : {
              value: undefined,
              type: getValueType(element.type)
            };
      });
      return {
        ...values,
        elements: props.elements
      };
    }
    const values = { ...state };
    props.elements.forEach(element => {
      if (values[element.key] === undefined) {
        values[element.key] = element.values
          ? element.values
          : {
              value: undefined,
              type: getValueType(element.type)
            };
      }
    });
    return {
      ...state,
      ...values
    };
  }

  // componentDidUpdate(prevProps, prevState, snapshot) {
  //   this.props.onChange(this.state);
  // }

  onChange = (key, value) => {
    let item = this.state[key] || {};
    item.value = value;
    this.setState({ [key]: item }, () => this.props.onChange(this.state, key));
  };

  get elements() {
    return this.props.elements;
  }

  get desc() {
    const { desc } = this.props;
    if (!desc) {
      return null;
    }
    return (
      <Text className="mb-20 d-block" type="secondary">
        {desc}
      </Text>
    );
  }

  get renderElements() {
    return this.elements.map(element => {
      if (element.hidden) {
        return null;
      }
      return (
        <GenericElementComponent
          {...{
            key: element.key,
            element,
            value: this.state[element.key].value,
            onChange: this.onChange,
            triggerNode: this.props.triggerNode
          }}
        />
      );
    });
  }

  get renderHiddenElements() {
    if (this.state?.hiddenOptions?.length > 0) {
      const options = this?.state?.options?.value?.[0] ?? "";
      return this.state.hiddenOptions.map(element => {
        if (options.includes(element?.isParentChecked)) {
          return (
            <GenericElementComponent
              {...{
                key: element.key,
                element,
                value: this.state[element.key]?.value,
                onChange: this.onChange
              }}
            />
          );
        }
      });
    }
  }

  get form() {
    const { layout } = this.props;
    return <Form layout={layout}>{this.renderElements}</Form>;
  }

  render() {
    return (
      <>
        {this.desc}
        {this.form}
        {this.renderHiddenElements}
      </>
    );
  }
}
GenericFormComponent.propTypes = {
  elements: PropTypes.array.isRequired,
  desc: PropTypes.string,
  onChange: PropTypes.func,
  layout: PropTypes.string.isRequired
};

GenericFormComponent.defaultProps = {
  layout: "vertical"
};
