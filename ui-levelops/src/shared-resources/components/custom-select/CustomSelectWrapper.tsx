import { lowerCase, pick } from "lodash";
import React, { CSSProperties } from "react";
import CustomSelectLabel from "./CustomSelectLabel";
import CustomSelect from "./CustomSelectNew";
import { convertStringCase, StringCaseType } from "./helper";

interface Props {
  selectLabel: string;
  options: Array<any>;
  mode: "default" | "multiple" | "tags";
  value: any;
  onChange: (value: any) => void;
  showArrow?: boolean;
  labelCase?: StringCaseType;
  valueKey?: string;
  labelKey?: string;
  truncateOptions?: boolean;
  truncateValue?: number;
  disabled?: boolean;
  loading?: boolean;
  placeholder?: string;
  style?: CSSProperties;
  suffixIcon?: React.ReactNode;
  dropdownVisible?: boolean;
  sortOptions?: boolean;
  createOption?: boolean;
  renderConditions?: React.ReactNode;
  allowClear?: boolean;
  required?: boolean;
}

class CustomSelectWrapper extends React.Component<Props, {}> {
  constructor(props: Props) {
    super(props);
  }

  convertCase = (value: string) => convertStringCase(value, this.props.labelCase ?? "none");

  getLabel = (item: any) => {
    const { labelKey, valueKey } = this.props;
    const value = typeof item === "string" ? item : item[labelKey ?? "label"] || item[valueKey || "value"] || "";

    // Edge case: Sometimes, value is a number.
    return typeof value === "string" ? this.convertCase(value) : value;
  };

  getValue = (item: any) => {
    const { valueKey } = this.props;
    return typeof item === "string" ? item : item[valueKey ?? "value"] || "";
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
    if (!options || !Array.isArray(options)) {
      return [];
    }
    let mappedOptions: Array<any> = options;

    if (truncateOptions) {
      mappedOptions = mappedOptions.slice(0, truncateValue || 10);
    }

    if (sortOptions) {
      mappedOptions = this.getSortedOptions(mappedOptions);
    }

    return mappedOptions.map((item: any) => ({ label: this.getLabel(item), value: this.getValue(item) }));
  };

  getProps = () => {
    return pick(this.props, [
      "mode",
      "value",
      "onChange",
      "showArrow",
      "createOption",
      "disabled",
      "loading",
      "placeholder",
      "style",
      "suffixIcon",
      "dropdownVisible",
      "allowClear"
    ]);
  };

  render() {
    return (
      <div className={"flex direction-column custom-universal-filter-item mb-10"}>
        <CustomSelectLabel
          required={this.props.required}
          label={this.props.selectLabel}
          renderConditions={this.props.renderConditions}
        />
        <CustomSelect options={this.mapOptions()} {...this.getProps()} />
      </div>
    );
  }
}

export default CustomSelectWrapper;
