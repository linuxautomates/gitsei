import * as PropTypes from "prop-types";
import { v1 as uuid } from "uuid";
import debounce from "lodash/debounce";
import { Select, Spin, Button } from "antd";
import React from "react";
import { connect } from "react-redux";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import { capitalize, get, lowerCase, upperCase } from "lodash";
import { genericPaginationData } from "reduxConfigs/selectors/restapiSelector";
import { getAzureReportApiRecords } from "reduxConfigs/sagas/saga-helpers/BASprintReport.helper";
import { genericCachedRestApiSelector } from "reduxConfigs/selectors/cachedRestapiSelector";
import { checkEntitlements } from "custom-hooks/helpers/entitlements.helper";
import { Entitlement } from "custom-hooks/constants";
import { userEntitlementsState } from "reduxConfigs/selectors/entitlements.selector";
import { DynamicDropDownType } from "./../../../configurations/pages/constant";

import "./select-restapi.scss";
const { Option } = Select;

function compareFilters(filter1, filter2) {
  if (filter1 === undefined || filter1 === null || filter2 === undefined || filter2 === null) {
    return true;
  }

  let newFilter1 = JSON.parse(JSON.stringify(filter1));
  let newFilter2 = JSON.parse(JSON.stringify(filter2));

  Object.keys(newFilter1).forEach(key => {
    if (!newFilter2.hasOwnProperty(key)) {
      return false;
    }
    if (Array.isArray(newFilter1[key])) {
      let array1 = newFilter1[key];
      let array2 = newFilter2[key];
      if (array1.length !== array2.length) {
        return false;
      }
      array1.forEach(item => {
        if (array2.filter(i => i === item).length === 0) {
          return false;
        }
      });
    } else {
      if (newFilter1[key] !== newFilter2[key]) {
        return false;
      }
    }

    delete newFilter1[key];
    delete newFilter2[key];
  });
  return !(Object.keys(newFilter1).length > 0 || Object.keys(newFilter2).length > 0);
}

export class SelectRestapiHelper extends React.PureComponent {
  constructor(props) {
    super(props);

    this.state = {
      search_value: "",
      more_filters: props.moreFilters,
      uuid: props.uuid || "0",
      refresh: props.refresh || 0
    };

    this.fetchData = this.fetchData.bind(this);
    this.handleSearch = this.handleSearch.bind(this);
    this.handleBlur = this.handleBlur.bind(this);
    this.mapOptions = this.mapOptions.bind(this);
    this.onChange = this.onChange.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (!compareFilters(props.moreFilters, state.more_filters)) {
      const { uri, method, uuid, transformPayload } = props;
      //Pagination added for rest select api. For pagination need to pass loadAllData = true;
      if (props.useCachedState) {
        props.cachedRestApiRead(
          uri,
          method,
          uuid,
          {
            filter: {
              ...props.moreFilters,
              partial: {
                [props.searchField]: state.search_value
              }
            }
          },
          props.searchField
        );
      } else {
        const payload = {
          filter: {
            ...props.moreFilters,
            partial: {
              [props.searchField]: state.search_value
            }
          }
        };
        props.restApiSelectGenericList(
          props.loadAllData,
          uri,
          method,
          !!transformPayload ? transformPayload(payload) : payload,
          null,
          props.uuid
        );
      }
      return {
        ...state,
        more_filters: props.moreFilters
      };
    }

    if (props.refresh !== state.refresh) {
      const { uri, method, uuid, moreFilters, transformPayload } = props;

      if (props.useCachedState) {
        props.cachedRestApiRead(
          uri,
          method,
          uuid,
          {
            filter: {
              partial: {
                [props.searchField]: state.search_value
              }
            }
          },
          props.searchField
        );
      } else {
        const payload = {
          filter: {
            ...(moreFilters ?? {}),
            partial: {
              [props.searchField]: state.search_value
            }
          }
        };
        props.restApiSelectGenericList(
          props.loadAllData,
          uri,
          method,
          !!transformPayload ? transformPayload(payload) : payload,
          null,
          props.uuid
        );
      }
      return {
        ...state,
        refresh: props.refresh
      };
    }

    return null;
  }

  get loading() {
    const { paginationState, cachedPaginationState, useCachedState } = this.props;
    //const { loading, error } = loadingStatus(rest_api, uri, method, this.state.uuid);
    //const loading = paginationState.loading !== undefined ? paginationState.loading : true;
    //const error = paginationState.error !== undefined ? paginationState.error : true;
    let loading = get(paginationState, ["loading"], true);
    let error = get(paginationState, ["error"], true);
    if (useCachedState) {
      loading = get(cachedPaginationState, ["loading"], true);
      error = get(cachedPaginationState, ["error"], true);
    }
    return loading || error;
  }

  componentDidMount() {
    // get the data pre loaded
    this.setState(
      {
        uuid: this.props.uuid || "0"
      },
      () => {
        if (this.props.fetchOnMount || this.loading) {
          setTimeout(() => {
            const { uri, method } = this.props;
            const isLoadingFromState = this.props?.restapiState?.[uri]?.[method]?.[this.props.uuid]?.loading;
            // checking if data is not already being fetched from store, this by-passes multiple API calls
            if (!isLoadingFromState) {
              this.fetchData(this.state.search_value);
            }
          }, 0);
        }
      }
    );
  }

  componentWillUnmount() {
    const { uri, method } = this.props;
    if (this.props.fetchOnMount) {
      this.props.restapiClear(uri, method, this.state.uuid);
    }
  }

  fetchData(inputValue) {
    // this is going to be the rest api dispatch call that happens
    const { uri, method, uuid, useCachedState, cachedRestApiRead, searchField, transformPayload } = this.props;
    const payload = {
      ...this.props.morePayload,
      filter: {
        ...this.props.moreFilters,
        partial: {
          [this.props.searchField]: inputValue
        }
      }
    };
    if (useCachedState) {
      cachedRestApiRead(uri, method, uuid, payload, searchField, !!inputValue);
    } else {
      this.props.restApiSelectGenericList(
        this.props.loadAllData,
        uri,
        method,
        !!transformPayload ? transformPayload(payload) : payload,
        null,
        uuid
      );
    }
  }

  handleSearch(value) {
    this.setState({ search_value: value }, () => this.fetchData(value));
  }

  convertCase = value => {
    const { label_case } = this.props;
    if (!label_case) {
      return value;
    }
    if (label_case) {
      switch (label_case) {
        case "lower_case":
          return lowerCase(value);
        case "title_case":
          return capitalize(value);
        case "upper_case":
          return upperCase(value);
        default:
          return "lower_case";
      }
    }
    return value;
  };

  option = (key, title, value, label, disabled = false) => {
    return (
      <Option title={title} value={value} key={uuid()} label={label} disabled={disabled}>
        {this.props.renderOption ? this.props.renderOption(label) : label}
      </Option>
    );
  };

  mapOptions() {
    const {
      searchField,
      createOption,
      createPrefix,
      additionalOptions,
      specialKey,
      paginationState,
      transformOptions,
      cachedPaginationState,
      useCachedState,
      dynamicValueType
    } = this.props;
    if (this.loading) {
      return "";
    }
    let list = [];
    if (useCachedState) {
      list = get(cachedPaginationState, ["data", "records"], []);
      list = (list ?? []).filter(option => option[searchField].includes(this.state.search_value));
    } else {
      list = get(paginationState, ["data", "records"], []);
    }
    if (this.props.hasNewRecordsFormat) {
      list = getAzureReportApiRecords(list);
    }

    if (this.props.filterOptionMethod) {
      list = list.filter(l => this.props.filterOptionMethod(l));
    }

    if (transformOptions) {
      list = transformOptions(list || []);
    }

    let options = list.map(option => {
      let label = typeof option === "object" ? option[searchField] : option;
      label = this.convertCase(label);
      if (typeof option !== "object") {
        return this.option(label, label, label, label);
      }
      return this.option(option.id, label, specialKey ? option[specialKey] : option.id, label, option.disabled);
    });

    if (additionalOptions.length !== 0) {
      const { optionsToAppendAtStart, optionsToAppendAtEnd } = additionalOptions.reduce(
        (carry, data) => {
          const label = this.convertCase(data[searchField]);
          const option = this.option(data.id, label, data.id, label);
          if (this.state.search_value !== "") {
            //console.log("Additional option with search not empty");
            //console.log(`${this.state.search_value} ${data.id}`);
            if (!data.id.includes(this.state.search_value)) {
              return carry;
            }
          }
          if (data.placement && data.placement === "start") {
            carry.optionsToAppendAtStart.push(option);
          } else {
            carry.optionsToAppendAtEnd.push(option);
          }
          return carry;
        },
        {
          optionsToAppendAtStart: [],
          optionsToAppendAtEnd: []
        }
      );

      // Using IF/ELSE just to avoid extra computation if there is no option at append in start.
      if (optionsToAppendAtStart.length === 0) {
        options.push(...optionsToAppendAtEnd);
      } else {
        options = [...optionsToAppendAtStart, ...options, ...optionsToAppendAtEnd];
      }
    }
    if (createOption && this.state.search_value !== "" && options.length === 0) {
      options.unshift(
        this.option(
          `${this.state.search_value}`,
          `${this.state.search_value}`,
          `${createPrefix}${this.state.search_value}`,
          `${this.state.search_value}`
        )
      );
      // options.unshift(
      //   <Option
      //     value={`${createPrefix}${this.state.search_value}`}
      //     key={this.state.search_value}
      //     label={this.state.search_value}
      //     title={this.state.search_value}>
      //     {this.state.search_value}
      //   </Option>
      // );
    }
    if (
      this.state.search_value !== "" &&
      this.props.createOption === true &&
      this.isExactMatch() === 0 &&
      this.partialMatch() > 0 &&
      dynamicValueType === DynamicDropDownType.OU &&
      this.isEntAddDynamicValue()
    ) {
      options.unshift(
        this.option(
          `${this.state.search_value}`,
          `${this.state.search_value}`,
          `${createPrefix}${this.state.search_value}`,
          `${this.state.search_value}`
        )
      );
    }
    return options;
  }

  onChange(_value) {
    const { searchField, mode, specialKey, paginationState, cachedPaginationState, useCachedState } = this.props;
    let paginatedList = [];
    if (useCachedState) {
      paginatedList = get(cachedPaginationState, ["data", "records"], []);
    } else {
      paginatedList = get(paginationState, ["data", "records"], []);
    }
    let value = _value;
    if (!!this.props.renderOption) {
      const _valueMapping = val => ({
        key: val.key,
        label: val.label.key || val.label
      });
      if (!!_value) {
        if (mode === "default") {
          value = _valueMapping(_value);
        } else if (mode === "multiple") {
          value = _value.map(_valueMapping);
        }
      }
    }

    if (mode === "default") {
      if (value === undefined) {
        return this.props.onChange(value);
      }
      const entry = paginatedList.filter(option => {
        if (typeof option !== "object") {
          return value.key === option;
        }
        if (specialKey) {
          return value ? option[specialKey] === value || option[specialKey] === value.key : false;
        }
        return value ? option.id === value || option.id === value.key : false;
      });

      if (entry.length > 0) {
        if (typeof entry[0] !== "object") {
          return this.props.onChange({
            label: entry[0],
            value: entry[0]
          });
        }
        return this.props.onChange({
          label: entry[0][searchField],
          value: specialKey ? entry[0][specialKey] : entry[0].id
        });
      } else {
        if (!value) {
          return this.props.onChange(value);
        } else {
          return this.props.onChange({ label: value.label, value: value.key });
        }
      }
    } else {
      this.props.onChange(value);
    }
    if (this.state.search_value !== "") {
      this.setState(
        {
          search_value: ""
        },
        () => this.fetchData()
      );
    }
  }

  get value() {
    const { labelInValue, value, mode } = this.props;
    return labelInValue && mode === "default" ? { key: value || "" } : value || undefined;
  }

  isExactMatch = () => {
    const { paginationState, cachedPaginationState, useCachedState } = this.props;
    let paginatedList = [];
    if (useCachedState) {
      paginatedList = get(cachedPaginationState, ["data", "records"], []);
    } else {
      paginatedList = get(paginationState, ["data", "records"], []);
    }
    let exactMatch = [];
    exactMatch = (paginatedList ?? []).filter(option => {
      return (option?.name ?? "") === this.state.search_value;
    });
    return exactMatch.length;
  };

  partialMatch = () => {
    const { paginationState, cachedPaginationState, searchField, useCachedState } = this.props;
    let paginatedList = [];
    if (useCachedState) {
      paginatedList = get(cachedPaginationState, ["data", "records"], []);
    } else {
      paginatedList = get(paginationState, ["data", "records"], []);
    }
    if (this.state.search_value) {
      return (paginatedList ?? []).filter(option => {
        const records = get(option, [searchField, "records"], []);
        return records.includes(this.state.search_value);
      })?.length;
    }
    return 0;
  };

  isEntAddDynamicValue() {
    return checkEntitlements(this.props.entitlement, Entitlement.DROPDOWN_ADD_DYNAMIC_VALUE);
  }
  get value() {
    const { labelInValue, value, mode } = this.props;
    return labelInValue && mode === "default" ? { key: value || "" } : value || undefined;
  }

  handleBlur() {
    this.handleSearch("");
  }

  render() {
    const { labelInValue, defaultValue, allowClear, showSpinnerWhenLoading, selectSpinnerClassName, dynamicValueType } =
      this.props;

    if (showSpinnerWhenLoading && this.loading) {
      return (
        <div className={selectSpinnerClassName}>
          <Spin size="small" />
        </div>
      );
    }

    return (
      <div className="restapi-select">
        <Select
          {...this.props}
          ref={this.props.innerRef}
          labelInValue={labelInValue}
          defaultValue={labelInValue ? { key: defaultValue || "" } : defaultValue || undefined}
          value={this.value}
          showArrow
          showSearch={this.props.showSearch}
          filterOption={false}
          allowClear={allowClear}
          loading={this.loading}
          notFoundContent={!!this.props.notFoundCallRender ? this.props.notFoundCallRender(this.loading) : null}
          onSearch={debounce(this.handleSearch, 300)}
          onChange={this.onChange}
          dropdownClassName={this.props.dropdownClassName}
          getPopupContainer={trigger => trigger.parentNode}
          onBlur={this.handleBlur}
          onSelect={(value, obj) => {
            // console.log(obj)
          }}>
          {this.mapOptions()}
        </Select>
      </div>
    );
  }
}

SelectRestapiHelper.propTypes = {
  className: PropTypes.string,
  rest_api: PropTypes.object,
  uri: PropTypes.string.isRequired,
  fetchData: PropTypes.func,
  actionName: PropTypes.string,
  onChange: PropTypes.func.isRequired,
  searchField: PropTypes.string.isRequired,
  method: PropTypes.oneOf(["list", "bulk"]),
  mode: PropTypes.oneOf(["default", "multiple", "tags", "single"]),
  createOption: PropTypes.bool,
  createPrefix: PropTypes.string,
  additionalOptions: PropTypes.array,
  labelInValue: PropTypes.bool,
  labelAsValue: PropTypes.bool,
  notFoundCallRender: PropTypes.func,
  specialKey: PropTypes.string,
  moreFilters: PropTypes.object,
  allowClear: PropTypes.bool,
  filterOptionMethod: PropTypes.func,
  label_case: PropTypes.oneOf(["title_case", "lower_case", "upper_case"]),
  uuid: PropTypes.any,
  renderOption: data => React.ReactNode,
  fetchOnMount: PropTypes.bool,
  morePayload: PropTypes.object,
  refresh: PropTypes.number,
  showSearch: PropTypes.bool,
  showSpinnerWhenLoading: PropTypes.bool,
  selectSpinnerClassName: PropTypes.string,
  transformOptions: data => {},
  hasNewRecordsFormat: PropTypes.bool,
  loadAllData: PropTypes.bool,
  useCachedState: PropTypes.bool,
  isOU: PropTypes.bool
};

SelectRestapiHelper.defaultProps = {
  className: "select-restapi-helper",
  searchField: "name",
  moreFilters: {},
  morePayload: {},
  method: "list",
  onChange: option => {},
  mode: "default",
  createOption: false,
  createPrefix: "create:",
  additionalOptions: [],
  labelInValue: true,
  specialKey: null,
  allowClear: true,
  labelAsValue: false,
  fetchOnMount: true,
  refresh: 0,
  showSearch: true,
  showSpinnerWhenLoading: false,
  selectSpinnerClassName: "dropdown-spinner",
  hasNewRecordsFormat: false,
  loadAllData: false,
  useCachedState: false,
  isOU: false
};

const dispatchToProps = dispatch => {
  return {
    ...mapRestapiDispatchtoProps(dispatch),
    ...mapGenericToProps(dispatch)
  };
};

const mapStateToProps = (state, ownProps) => ({
  restapiState: state?.restapiReducer,
  paginationState: genericPaginationData(state, ownProps),
  cachedPaginationState: genericCachedRestApiSelector(state, ownProps),
  entitlement: userEntitlementsState(state)
});

const SelectRestAPI = React.forwardRef((props, ref) => {
  return <SelectRestapiHelper innerRef={props.innerRef} {...props} />;
});

export default connect(mapStateToProps, dispatchToProps)(SelectRestAPI);
