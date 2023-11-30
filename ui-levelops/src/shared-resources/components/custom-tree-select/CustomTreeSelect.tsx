import React, { CSSProperties } from "react";
import { TreeSelect, Switch } from "antd";
import { lowerCase, upperCase } from "lodash";
import { toTitleCase } from "utils/stringUtils";
import { AntButtonComponent as AntButton } from "../ant-button/ant-button.component";
import { AntTextComponent as AntText } from "../ant-text/ant-text.component";
import "./CustomTreeSelect.scss";
import { convertArrayToTree } from "./helper";

const { SHOW_CHILD } = TreeSelect;

interface Props {
  options: Array<any>;
  value: any;
  onChange: (value: any) => void;
  showArrow?: boolean;
  createOption: boolean;
  labelCase: "lower_case" | "upper_case" | "title_case" | "none";
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
  sortOptions?: boolean;
}

interface State {
  searchValue: string;
}

class CustomTreeSelect extends React.Component<Props, State> {
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
    this.convertCase = this.convertCase.bind(this);
    this.onSwitchChange = this.onSwitchChange.bind(this);
    this.notFoundContent = this.notFoundContent.bind(this);
  }

  componentWillUnmount() {
    this.setState({ searchValue: "" });
  }

  handleSearch = (value: string) => {
    this.setState({ searchValue: value });
  };

  onChange = (value: string[], label?: string, extra?: any) => {
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

  getSortedOptions = (options: any[]) => {
    return (options || []).sort((a: any, b: any) => {
      const label1 = lowerCase(a.key);
      const label2 = lowerCase(b.key);
      if (label1 < label2) return -1;
      if (label1 > label2) return 1;
      return 0;
    });
  };

  mapOptions = () => {
    const { options, truncateOptions, truncateValue, sortOptions } = this.props;

    const data = convertArrayToTree(options);

    const { searchValue } = this.state;
    if (!data) {
      return undefined;
    }
    let mappedOptions: Array<any> = Array.isArray(data)
      ? data.filter((item: any) => {
          const value = item.value;
          const label = `${this.convertCase(item.parent_key)}/${this.convertCase(item.value)}`;

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
      mappedOptions = mappedOptions.slice(0, truncateValue || 10);
    }

    if (sortOptions) {
      mappedOptions = this.getSortedOptions(mappedOptions);
    }

    return mappedOptions;
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
            this.onChange([...this.props.value, this.state.searchValue]);
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
        className="flex justify-content-between align-center custom-tree-select-container"
        style={this.props.style || {}}>
        <TreeSelect
          ref={this.selectRef}
          autoClearSearchValue
          treeCheckable
          showCheckedStrategy={SHOW_CHILD}
          treeData={this.mapOptions()}
          dropdownClassName={this.props.dropdownVisible === false ? "ant-select-dropdown-hidden" : ""}
          suffixIcon={this.props.suffixIcon}
          disabled={this.props.disabled}
          value={this.props.value}
          showArrow={this.props.showArrow}
          showSearch
          notFoundContent={
            !!this.state.searchValue
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
          {/*{this.mapOptions()}*/}
        </TreeSelect>
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

export default React.memo(CustomTreeSelect);
