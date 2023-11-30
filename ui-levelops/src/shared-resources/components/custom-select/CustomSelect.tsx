import { Select, Switch } from "antd";
import { lowerCase, upperCase } from "lodash";
import React, { Component, CSSProperties } from "react";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import { toTitleCase } from "../../../utils/stringUtils";
import "./CustomSelect.scss";

const { Option } = Select;

//Cases handled by the component
//1. the options is the array of string then the key and the label will be that string. the label depends upon the labelCase
//2. the options is the array of object. valueKey will be the object key that you want to use as value.
// You can also define the labelKey if you have a separate label key in the object else the valueKey value will be used as the label value

interface Props {
  options: Array<any>;
  mode: "default" | "multiple" | "tags";
  value: any;
  onChange: (value: any) => void;
  showArrow?: boolean;
  createOption: boolean;
  labelCase: "lower_case" | "upper_case" | "title_case" | "none";
  valueKey?: string;
  labelKey?: string;
  createPrefix?: string;
  truncateOptions?: boolean;
  truncateValue?: number;
  showSwitch?: boolean;
  switchValue?: boolean;
  onSwitchValueChange?: (value: boolean) => void;
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
  dataTestid?: string;
  style?: CSSProperties;
  suffixIcon?: React.ReactNode;
  dropdownVisible?: boolean;
  dataFilterNameDropdownKey?: string;
  sortOptions?: boolean;
  allowClear?: boolean;
  defaultValue?: any;
  className?: string;
  showSearch?: boolean;
  maxTagCount?: number;
}

interface State {
  searchValue: string;
}

class CustomSelect extends Component<Props, State> {
  private selectRef: any;
  constructor(props: Props) {
    super(props);
    this.state = {
      searchValue: ""
    };
    this.selectRef = React.createRef();
    this.handleSearch = this.handleSearch.bind(this);
    this.onChange = this.onChange.bind(this);
    this.mapOptions = this.mapOptions.bind(this);
    this.getLabel = this.getLabel.bind(this);
    this.convertCase = this.convertCase.bind(this);
    this.getValue = this.getValue.bind(this);
    this.onSwitchChange = this.onSwitchChange.bind(this);
    this.notFoundContent = this.notFoundContent.bind(this);
  }

  componentDidUpdate() {
    if (this.props.dataFilterNameDropdownKey) {
      const dropdownDiv: HTMLCollection = document.getElementsByClassName(this.props.dataFilterNameDropdownKey);
      if (
        dropdownDiv?.length &&
        !dropdownDiv.item(0)?.hasAttribute("data-filterselectornamekey") &&
        !dropdownDiv.item(0)?.hasAttribute("data-filtervaluesnamekey")
      ) {
        dropdownDiv.item(0)?.setAttribute("data-filterselectornamekey", this.props.dataFilterNameDropdownKey);
        dropdownDiv.item(0)?.setAttribute("data-filtervaluesnamekey", this.props.dataFilterNameDropdownKey);
      }
    }
  }

  componentWillUnmount() {
    this.setState({ searchValue: "" });
  }

  handleSearch = (value: string) => {
    this.setState({ searchValue: value });
  };

  onChange = (value: string[]) => {
    this.props.onChange(value);
    this.setState({ searchValue: "" });
  };

  convertCase = (value: string) => {
    switch (this.props.labelCase) {
      case "lower_case":
        return lowerCase(value);
      case "upper_case":
        return upperCase(value);
      case "title_case":
        return toTitleCase(value);
      default:
        return value;
    }
  };

  getLabel = (item: any) => {
    const { labelKey, valueKey } = this.props;
    const value =
      typeof item === "string"
        ? item
        : !labelKey
        ? item[valueKey || ""] || ""
        : item[labelKey] || item[valueKey || ""] || "";

    // Edge case: Sometimes, value is a number.
    return typeof value === "string" ? this.convertCase(value) : value;
  };

  getValue = (item: any) => {
    const { valueKey } = this.props;
    return typeof item === "string" ? item : item[valueKey || ""] ?? "";
  };

  getSortedOptions = (options: any[]) => {
    return (options || []).sort((a: any, b: any) => {
      const label1 = lowerCase(this.getLabel(a));
      const label2 = lowerCase(this.getLabel(b));
      if (label1 < label2) return -1;
      if (label1 > label2) return 1;
      return 0;
    });
  };

  mapOptions = () => {
    const { options, truncateOptions, truncateValue, sortOptions } = this.props;
    const { searchValue } = this.state;
    if (!options) {
      return null;
    }
    let mappedOptions: Array<any> = Array.isArray(options)
      ? options.filter((item: any) => {
          const value = this.getValue(item);
          const label = this.getLabel(item);

          let isValueMatch = false;
          let isLabelMatch = false;

          if (typeof value === "string") {
            isValueMatch = value.toLowerCase().includes(searchValue.toLowerCase());
          }
          if (typeof label === "string") {
            isLabelMatch = !!label && label.toLowerCase().includes(searchValue.toLowerCase());
          }
          if (typeof value === "string" || typeof label === "string") {
            return isValueMatch || isLabelMatch;
          }

          return true;
        })
      : [];

    if (truncateOptions) {
      mappedOptions = mappedOptions.slice(0, truncateValue);
    }

    if (sortOptions) {
      mappedOptions = this.getSortedOptions(mappedOptions);
    }

    return mappedOptions.map((item: any, index: number) => (
      <Option key={index} value={this.getValue(item)}>
        {this.getLabel(item)}
      </Option>
    ));
  };

  onSwitchChange = (value: boolean) => {
    const { onSwitchValueChange } = this.props;
    onSwitchValueChange && onSwitchValueChange(value);
  };

  notFoundContent = () => {
    return (
      <div className={"custom-select-container__no-content-found-div"}>
        <div className="custom-select-container__no-content-found-div__label-div">{`"${this.state.searchValue}" not found`}</div>
        <AntButton
          type="secondary"
          className="custom-select-container__no-content-found-div__button"
          onClick={() => {
            this.selectRef?.current?.blur();
            this.onChange([...(this.props.value || []), this.state.searchValue]);
          }}>
          Add Value
        </AntButton>
      </div>
    );
  };

  render() {
    return (
      <div
        data-testid={this.props.dataTestid || "custom-select-container-div"}
        className="flex justify-content-between align-center custom-select-container"
        style={this.props.style || {}}>
        <Select
          ref={this.selectRef}
          className={`test_class ${this.props.className}`}
          autoClearSearchValue
          dropdownClassName={
            this.props.dropdownVisible === false
              ? "ant-select-dropdown-hidden"
              : this.props.dataFilterNameDropdownKey || ""
          }
          defaultValue={this.props.defaultValue}
          suffixIcon={this.props.suffixIcon}
          mode={this.props.mode}
          disabled={this.props.disabled}
          value={this.props.value}
          showArrow={this.props.showArrow}
          showSearch={this.props.showSearch === undefined ? true : this.props.showSearch}
          notFoundContent={
            !!this.state.searchValue && this.props.createOption
              ? this.props?.value?.includes(this.state.searchValue)
                ? ""
                : this.notFoundContent()
              : ""
          }
          allowClear={this.props.allowClear}
          loading={this.props.loading}
          filterOption={false}
          placeholder={this.props.placeholder}
          onSearch={this.handleSearch}
          onBlur={() => this.setState({ searchValue: "" })}
          style={this.props.style}
          onChange={this.onChange}
          maxTagCount={this.props.maxTagCount}>
          {this.mapOptions()}
        </Select>
        {this.props.showSwitch && (
          <div style={{ display: "flex", flexDirection: "column", marginLeft: "0.5rem", fontSize: "0.6rem" }}>
            <Switch title={"Exclude"} onChange={this.onSwitchChange} checked={this.props.switchValue} size={"small"} />
            <AntText>Exclude</AntText>
          </div>
        )}
      </div>
    );
  }
}

export default CustomSelect;
