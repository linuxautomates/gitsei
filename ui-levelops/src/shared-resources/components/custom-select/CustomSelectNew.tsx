import React, { CSSProperties } from "react";
import { AntButton } from "..";
import { Select } from "antd";

const { Option } = Select;

interface CustomSelectProps {
  options: Array<{ label: string; value: any }>;
  mode: "default" | "multiple" | "tags";
  value: any;
  onChange: (value: any) => void;
  showArrow?: boolean;
  createOption?: boolean;
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
  style?: CSSProperties;
  suffixIcon?: React.ReactNode;
  dropdownVisible?: boolean;
  allowClear?: boolean;
}

interface CustomSelectState {
  searchValue: string;
}

class CustomSelect extends React.Component<CustomSelectProps, CustomSelectState> {
  private selectRef: any;
  constructor(props: CustomSelectProps) {
    super(props);
    this.state = {
      searchValue: ""
    };
    this.selectRef = React.createRef();
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

  notFoundContent = () => {
    return (
      <div className={"custom-select-container__no-content-found-div"}>
        <div className="custom-select-container__no-content-found-div__label-div">{`"${this.state.searchValue}" not found`}</div>
        <AntButton
          type="secondary"
          className="custom-select-container__no-content-found-div__button"
          onClick={() => {
            this.selectRef?.current?.blur();
            this.onChange([...this.props.value, this.state.searchValue]);
          }}>
          Add Value
        </AntButton>
      </div>
    );
  };

  mapOptions = () => {
    const { searchValue } = this.state;
    let mappedOptions: Array<any> = this.props.options.filter((item: any) => {
      const value = item.value;
      const label = item.label;

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
    });
    return mappedOptions.map((item: any, index: number) => (
      <Option key={index} value={item.value}>
        {item.label}
      </Option>
    ));
  };

  render() {
    return (
      <Select
        ref={this.selectRef}
        allowClear={this.props.allowClear}
        autoClearSearchValue
        dropdownClassName={this.props.dropdownVisible === false ? "ant-select-dropdown-hidden" : ""}
        suffixIcon={this.props.suffixIcon}
        mode={this.props.mode}
        disabled={this.props.disabled}
        value={this.props.value}
        showArrow={this.props.showArrow}
        showSearch
        notFoundContent={
          !!this.state.searchValue && this.props.createOption
            ? this.props?.value?.includes(this.state.searchValue)
              ? ""
              : this.notFoundContent()
            : ""
        }
        loading={this.props.loading}
        filterOption={false}
        placeholder={this.props.placeholder}
        onSearch={this.handleSearch}
        onBlur={() => this.setState({ searchValue: "" })}
        onChange={this.onChange}>
        {this.mapOptions()}
      </Select>
    );
  }
}

export default CustomSelect;
