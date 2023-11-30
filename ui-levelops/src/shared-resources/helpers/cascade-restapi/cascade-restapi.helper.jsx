import * as PropTypes from "prop-types";
import debounce from "lodash/debounce";
import { Cascader } from "antd";
import React from "react";
import { getError, getLoading } from "utils/loadingUtils";

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

export class CascadeRestapiHelper extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      search_value: "",
      more_filters: {},
      selected_nodes: [],
      children_loading: false
    };

    this.getLoading = this.getLoading.bind(this);
    this.getChildLoading = this.getChildLoading.bind(this);
    this.fetchData = this.fetchData.bind(this);
    this.loadData = this.loadData.bind(this);
    this.handleSearch = this.handleSearch.bind(this);
    this.mapOptions = this.mapOptions.bind(this);
  }

  static getDerivedStateFromProps(props, state) {
    if (!compareFilters(props.moreFilters, state.more_filters)) {
      props.fetchData({
        filter: {
          ...props.moreFilters,
          partial: {
            [props.searchField]: state.search_value
          }
        }
      });
      return {
        ...state,
        more_filters: props.moreFilters
      };
    }

    return null;
  }

  getLoading() {
    return !(
      !getLoading(this.props.rest_api, this.props.uri, this.props.method, "0") &&
      !getError(this.props.rest_api, this.props.uri, this.props.method, "0")
    );
  }

  getChildLoading(id) {
    return !(
      !getLoading(this.props.rest_api, this.props.uri, this.props.childMethod, id) &&
      !getError(this.props.rest_api, this.props.uri, this.props.childMethod, id)
    );
  }

  componentDidMount() {
    // get the data pre loaded
    this.fetchData(this.state.search_value);
  }

  fetchData(inputValue, pageSize = undefined) {
    // this is going to be the rest api dispatch call that happens
    this.props.fetchData({
      filter: {
        ...this.props.moreFilters,
        partial: {
          [this.props.searchField]: inputValue
        }
      },
      page_size: pageSize
    });
  }

  handleSearch(value) {
    this.setState({ search_value: value }, () => this.fetchData(value));
  }

  mapOptions() {
    const { rest_api, uri, method, childMethod } = this.props;
    if (this.getLoading()) {
      return [];
    } else {
      let options = rest_api[uri][method]["0"].data.records.map(option => ({
        value: option,
        label: option,
        isLeaf: false
      }));
      if (this.state.selected_nodes.length > 0) {
        const node = this.state.selected_nodes[0].value;
        if (!this.getChildLoading(node)) {
          let children = rest_api[uri][childMethod][node].data.records.map(child => ({
            value: child,
            label: child,
            isLeaf: true
          }));
          options.every(option => {
            if (option.value === node) {
              option.loading = false;
              option.children = children;
              return false;
            }
            return true;
          });
        } else {
          options.every(option => {
            if (option.value === node) {
              option.loading = true;
              return false;
            }
            return true;
          });
        }
      }
      return options;
    }
  }

  loadData(selectedOptions) {
    this.setState(
      {
        selected_nodes: selectedOptions
      },
      () => {
        this.props.fetchChildren(selectedOptions[0].value);
      }
    );
  }

  render() {
    const options = this.mapOptions();
    return (
      <Cascader
        {...this.props}
        labelInValue={true}
        //value={value}
        showArrow={true}
        showSearch={false}
        filterOption={false}
        allowClear={true}
        loading={this.getLoading()}
        onSearch={debounce(this.handleSearch, 300)}
        //onChange={this.props.onChange}
        onChange={(value, selectedOptions) => {
          console.log(value);
          console.log(selectedOptions);
          if (value.length === 2) {
            const string = `${value[0]} ${value[1]}`;
            this.props.onChange(string);
          }
        }}
        loadData={this.loadData}
        options={options}
        mode={"tags"}
        expandTrigger={"click"}
        changeOnSelect
        // onSelect={(value, node, extra) => {
        //     console.log(node.props);
        //     if(node.props.type === 'label') {
        //         this.setState({
        //             expanded_nodes: [node.props.value]
        //         }, () => this.props.fetchChildren(node.props.value))
        //     }
        // }}
        style={{ width: "100%" }}
        //loadData={this.fetchChildren}
        //onTreeExpand={this.onExpand}
      />
    );
  }
}

CascadeRestapiHelper.propTypes = {
  className: PropTypes.string,
  rest_api: PropTypes.object.isRequired,
  uri: PropTypes.string.isRequired,
  fetchData: PropTypes.func.isRequired,
  fetchChildren: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
  searchField: PropTypes.string.isRequired,
  method: PropTypes.oneOf(["list", "bulk"]),
  childMethod: PropTypes.oneOf(["values", "list"]),
  mode: PropTypes.oneOf(["default", "multiple", "tags"]),
  additionalOptions: PropTypes.array,
  labelInValue: PropTypes.bool,
  specialKey: PropTypes.string,
  moreFilters: PropTypes.object
};

CascadeRestapiHelper.defaultProps = {
  className: "select-restapi-helper",
  searchField: "name",
  moreFilters: {},
  method: "list",
  childMethod: "values",
  onChange: option => {},
  mode: "default",
  createOption: false,
  additionalOptions: [],
  labelInValue: true,
  specialKey: null
};
