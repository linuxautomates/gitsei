import { DatePicker, Icon, InputNumber, Radio, Tag } from "antd";
import { FILTER_TYPE } from "constants/filters";
import { get } from "lodash";
import * as PropTypes from "prop-types";
import React from "react";
import { AntCol, AntInput, AntRow, AntSelect, CustomSelect, EditableTag } from "shared-resources/components";
import { CascadeRestapi, SelectRestapi } from "shared-resources/helpers";
import { getKeyForFilter } from "../../../../../dashboard/constants/helper";
import { getEndOfDayFromDate, getStartOfDayFromDate } from "../../../../../utils/dateUtils";
import { CustomFormItemLabel, InputRangeFilter } from "../../../../components";
import "./filters.style.scss";
import PartialMatchFilter from "./partialMatchFilter";

const { RangePicker } = DatePicker;
import moment from "moment";

export class FiltersContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      search_value: ""
    };
    this.onSearchHandler = this.onSearchHandler.bind(this);
    this.onOptionSelect = this.onOptionSelect.bind(this);
    this.onRestOptionSelect = this.onRestOptionSelect.bind(this);
    this.onRestCascadeOptionSelect = this.onRestCascadeOptionSelect.bind(this);
    this.onRangeSelect = this.onRangeSelect.bind(this);
    this.onInputChange = this.onInputChange.bind(this);
    this.onChangehandler = this.onChangehandler.bind(this);
    this.onPartialRadioChange = this.onPartialRadioChange.bind(this);
  }
  isFilterPresentInMissingFields = field => {
    const missingFields = this.props?.more_filters?.missing_fields || {};
    return Object.keys(missingFields).includes(field);
  };

  filterCheckboxValue = field => {
    let filterField = getKeyForFilter(field);
    if (this.isFilterPresentInMissingFields(filterField)) {
      return get(this.props, ["more_filters", "missing_fields", filterField]);
    }
    return undefined;
  };

  onSearchHandler(field) {
    return e => {
      this.props.onSearchEvent(field, e.target.value);
    };
  }

  onChangehandler(field, type) {
    return e => {
      this.props.onInputFieldChangeHandler(field, e.target.value, type);
    };
  }

  onPartialRadioChange(field, type, value) {
    this.props.onInputFieldChangeHandler(field, value, type);
  }

  onInputChange(field) {
    return value => this.props.onInputChange(field, value);
  }

  onBinaryChange(field) {
    return e => this.props.onBinaryChange(field, e.target.value);
  }

  onOptionSelect(field) {
    return value => {
      console.log(`${field} ${value}`);
      let val = undefined;
      if (value !== undefined) {
        if (value.key !== undefined) {
          val = value.key;
        } else {
          val = value;
        }
      }
      this.props.onOptionSelectEvent(field, val);
    };
  }

  onRestOptionSelect(field) {
    return option => {
      console.log(field, option);
      const filter = this.props.filtersConfig.filter(rec => rec.field === field);
      if (filter.length > 0 && filter[0].returnCall) {
        filter[0].returnCall(field, option ? option : undefined);
      }
      this.props.onOptionSelectEvent(field, option ? option : undefined);
    };
  }

  onRestCascadeOptionSelect(field) {
    return option => {
      const filter = this.props.filtersConfig.filter(rec => rec.field === field);
      let tags = filter[0].selected || [];
      if (option === undefined) {
        //tags = []
      } else {
        tags.push(option);
      }
      this.setState(
        {
          tags: tags
        },
        () => {
          this.props.onOptionSelectEvent(field, tags);
        }
      );
    };
  }

  // make changes
  onRangeSelect(field, dataType) {
    return (dates, dateStrings) => {
      if (dates.length > 1) {
        let data = {
          $gt: getStartOfDayFromDate(dates[0]),
          $lt: getEndOfDayFromDate(dates[1])
        };
        if (dataType === "string") {
          data = {
            $gt: data["$gt"].toString(),
            $lt: data["$lt"].toString()
          };
        }
        return this.props.onOptionSelectEvent(field, data, "date");
      } else {
        return this.props.onOptionSelectEvent(field, undefined, "date");
      }
    };
  }

  handleRemoveTag(field, selected, index) {
    return e => {
      e.preventDefault();
      console.log("removing tag");
      let tags = selected || [];
      tags.splice(index, 1);
      this.setState(
        {
          tags: [...tags]
        },
        () => {
          this.props.onOptionSelectEvent(field, tags);
        }
      );
    };
  }

  renderLabel = label => (
    <div className="filter-label">
      <span>{label}</span>
    </div>
  );

  getFilters() {
    // eslint-disable-next-line array-callback-return
    return this.props.filtersConfig.map((filter, index) => {
      if (filter.type === "search") {
        return (
          <AntCol key={index} className="gutter-row" span={filter.span ? filter.span : 4}>
            {this.renderLabel(filter.label)}
            <AntInput
              id={`search-${filter.id}`}
              placeholder={filter.label}
              onChange={this.onSearchHandler(filter.field)}
              value={filter.selected || ""}
              name={filter.id}
            />
          </AntCol>
        );
      }
      if (filter.type === "binary") {
        return (
          <AntCol key={index} className="gutter-row flex align-center" span={filter.span ? filter.span : 4}>
            {this.renderLabel(filter.label)}
            <Radio.Group
              id={`binary-${filter.id}`}
              style={{ marginLeft: "1rem" }}
              onChange={this.onBinaryChange(filter.field)}
              value={filter.selected === undefined ? "all" : filter.selected}>
              <Radio value={true}>Yes</Radio>
              <Radio value={false}>No</Radio>
              <Radio value={"all"}>All</Radio>
            </Radio.Group>
          </AntCol>
        );
      }

      if (filter.type === "input") {
        return (
          <AntCol key={index} className="gutter-row" span={filter.span ? filter.span : 4}>
            {this.renderLabel(filter.label)}
            <InputNumber
              style={{ width: "100%" }}
              id={`input-${filter.id}`}
              placeholder={filter.label}
              onChange={this.onInputChange(filter.field)}
              value={filter.selected || ""}
              name={filter.id}
            />
          </AntCol>
        );
      }

      if (filter.type === FILTER_TYPE.PARTIAL_MATCH) {
        return (
          <PartialMatchFilter
            filterConfig={filter}
            index={index}
            onChangehandler={this.onChangehandler}
            renderLabel={this.renderLabel}
            onPartialRadioChange={this.onPartialRadioChange}></PartialMatchFilter>
        );
      }

      if (filter.type === FILTER_TYPE.FE_SELECT) {
        return (
          <AntCol key={index} className="gutter-row" span={filter.span ? filter.span : 12}>
            {this.renderLabel(filter.label)}
            <CustomSelect
              dataTestid="filter-list-element-select"
              dataFilterNameDropdownKey={`open-report-filter-list-${filter.label}`}
              valueKey={"value"}
              className={"w-100"}
              labelKey={"label"}
              createOption={true}
              labelCase={"title_case"}
              options={[]}
              mode={"multiple"}
              value={filter.selected}
              truncateOptions={true}
              sortOptions
              dropdownVisible={true}
              showSwitch={false}
              onChange={this.onOptionSelect(filter.field)}
            />
          </AntCol>
        );
      }

      if (filter.type === "tags") {
        return (
          <AntCol key={index} className="gutter-row" span={filter.span ? filter.span : 4}>
            {this.renderLabel(filter.label)}
            <EditableTag
              tagLabel={"Add Epic"}
              style={{ width: "100%" }}
              tags={filter.selected || []}
              onTagsChange={value => this.props.onTagsChange("epics", value)}
            />
          </AntCol>
        );
      }

      if (["select", "multiSelect"].includes(filter.type)) {
        const optionLength = (filter.options || []).length;
        let filteredOptions = filter.options;
        const maxLength = filter.unlimitedLength ? optionLength : Math.min(20, optionLength);
        if (optionLength > 20 && !filter.unlimitedLength) {
          filteredOptions = (filter.options || [])
            .filter(opt => {
              if (typeof opt === "string") {
                return (opt || "").toLowerCase().includes((this.state.search_value || "").toLowerCase());
              }
              return (
                (opt.value || "").includes(this.state.search_value) ||
                (opt.label || "").toLowerCase().includes((this.state.search_value || "").toLowerCase())
              );
            })
            .slice(0, maxLength);
        }
        //done because we have case where we have more than 20 options and the selected option has index > 20
        const filterKeys = filteredOptions.map(option => option.value);
        if (filter.type === "multiSelect") {
          (filter.selected || []).forEach(item => {
            const option = filter.options.find(option => option.value === item);
            if (option && !filterKeys.includes(item)) {
              filteredOptions.push(option);
            }
          });
        }
        if (filter.type === "select") {
          const option = filter.options.find(option => option.value === filter.selected);
          if (option && !filterKeys.includes(filter.selected)) {
            filteredOptions.push(option);
          }
        }
        return (
          <AntCol
            key={index}
            className="gutter-row"
            span={filter.span ? filter.span : filter.type === "select" ? 4 : 12}>
            <CustomFormItemLabel
              label={filter.label}
              withSwitch={{
                showSwitch: filter.showExcludeSwitch,
                showSwitchText: filter.showExcludeSwitch,
                switchValue: filter.excludeSwitchValue,
                disabled: this.filterCheckboxValue(filter.field) !== undefined,
                onSwitchValueChange: value => this.props?.onExcludeSwitchChange?.(filter.field, value)
              }}
              withCheckBoxes={{
                showCheckboxes: filter.showCheckboxes,
                checkboxes: [
                  { label: "PR", key: "pr", value: this.filterCheckboxValue(filter.field) === false },
                  { label: "AB", key: "ab", value: this.filterCheckboxValue(filter.field) === true }
                ],
                onCheckBoxChange: (key, value) => this.props?.onCheckBoxValueChange?.(filter.field, key, value)
              }}
            />
            <AntSelect
              style={{ width: "100%" }}
              id={`select-${filter.id}`}
              placeholder={filter.label}
              onSearch={value => {
                this.setState({ search_value: value });
              }}
              options={[...filteredOptions]}
              filterOption={optionLength < 20}
              defaultValue={filter.selected}
              onBlur={e => this.setState({ search_value: "" })}
              value={filter.selected}
              onChange={this.onOptionSelect(filter.field)}
              allowClear={true}
              disabled={this.filterCheckboxValue(filter.field) !== undefined}
              mode={filter.type === "multiSelect" ? "multiple" : "default"}
            />
          </AntCol>
        );
      }

      if (filter.type === "dateRange") {
        // considering the timestamp is in UTC, showing in local timezone
        const selected = filter.selected
          ? Object.keys(filter.selected).map(key =>
              moment.unix(filter.selected[key]).add(moment().utcOffset() * -1, "m")
            )
          : [];
        return (
          <AntCol key={index} className={"gutter-row"} span={filter.span ? filter.span : 8}>
            {this.renderLabel(filter.label)}
            <RangePicker
              onChange={this.onRangeSelect(filter.field, filter.rangeDataType)}
              allowClear={true}
              value={selected || []}
              showTime={{
                defaultValue: [moment("00:00:00", "HH:mm:ss"), moment("11:59:59", "HH:mm:ss")]
              }}
            />
          </AntCol>
        );
      }

      if (filter.type === "timeRange") {
        return (
          <AntCol key={index} className={"gutter-row"} span={filter.span ? filter.span : 8}>
            <InputRangeFilter
              label={filter.label}
              onChange={this.onInputChange(filter.field)}
              value={filter.selected || {}}
            />
          </AntCol>
        );
      }

      if (["apiSelect", "apiMultiSelect"].includes(filter.type)) {
        return (
          <AntCol
            key={index}
            className="gutter-row"
            span={filter.span ? filter.span : filter.type === "apiSelect" ? 4 : 12}>
            {this.renderLabel(filter.label === "" ? filter.uri : filter.label)}
            <SelectRestapi
              style={{ width: "100%" }}
              id={`apiselect-${filter.id}`}
              placeholder={filter.label}
              //rest_api={this.props.rest_api}
              uri={filter.uri}
              //fetchData={filter.apiCall}
              searchField={filter.searchField}
              defaultValue={filter.selected || null}
              additionalOptions={filter.options || []}
              value={filter.selected || ""}
              onChange={this.onRestOptionSelect(filter.field)}
              createOption={false}
              mode={filter.type === "apiMultiSelect" ? "multiple" : "default"}
              specialKey={filter.specialKey}
            />
          </AntCol>
        );
      }

      if (["cascade"].includes(filter.type)) {
        const selected = filter.selected || [];
        return (
          <>
            <AntCol key={index} className="gutter-row" span={filter.span ? filter.span : 24}>
              {this.renderLabel(filter.label === "" ? filter.uri : filter.label)}
              <AntRow type={"flex"} align={"middle"} gutter={[10, 10]}>
                <AntCol span={8}>
                  <CascadeRestapi
                    style={{ width: "40%" }}
                    id={`cascade-${filter.id}`}
                    placeholder={filter.label}
                    //rest_api={this.props.rest_api}
                    uri={filter.uri}
                    //fetchData={filter.apiCall}
                    searchField={filter.searchField}
                    defaultValue={filter.selected || null}
                    //value={filter.selected || ''}
                    onChange={this.onRestCascadeOptionSelect(filter.field)}
                    createOption={false}
                    mode={filter.type === "apiMultiSelect" ? "multiple" : "default"}
                    childMethod={filter.childMethod || "values"}
                    fetchChildren={filter.fetchChildren}
                  />
                </AntCol>
                <AntCol span={12}>
                  {selected.map((tag, index) => {
                    return (
                      <Tag closable={true} onClose={this.handleRemoveTag(filter.field, filter.selected, index)}>
                        {tag}
                      </Tag>
                    );
                  })}
                </AntCol>
              </AntRow>
            </AntCol>
          </>
        );
      }
    });
  }

  render() {
    return (
      <div className={`${this.props.className}`} data-testid={"server-paginated-filters"}>
        <Icon
          type="close"
          style={{ right: "1rem", position: "absolute", zIndex: 1 }}
          onClick={this.props.onCloseFilters}
        />
        <AntRow gutter={[16, 16]} justify={"start"} type={"flex"} align={"bottom"}>
          {this.getFilters()}
        </AntRow>
      </div>
    );
  }
}

FiltersContainer.propTypes = {
  filtersConfig: PropTypes.array.isRequired,
  onSearchEvent: PropTypes.func,
  onOptionSelectEvent: PropTypes.func,
  onInputChange: PropTypes.func,
  onBinaryChange: PropTypes.func,
  onTagsChange: PropTypes.func,
  onMultiOptionsChangeEvent: PropTypes.func,
  className: PropTypes.string
};

FiltersContainer.defaultProps = {
  onSearchEvent: () => null,
  onOptionSelectEvent: () => null,
  onMultiOptionsChangeEvent: () => null,
  className: "filters"
};
