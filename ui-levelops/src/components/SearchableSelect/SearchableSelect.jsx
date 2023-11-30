import React, { Component } from "react";
import PropTypes from "prop-types";
import CreatableSelect from "react-select/lib/Creatable";
import { getLoading } from "../../utils/loadingUtils";
import debounce from "lodash/debounce";

export class SearchableSelect extends Component {
  constructor(props) {
    super(props);
    let method = "search";
    let moreFilters = {};
    if (props.hasOwnProperty("method")) {
      method = props.method;
    }
    if (props.hasOwnProperty("moreFilters")) {
      moreFilters = props.moreFilters;
    }
    this.state = {
      input: "",
      options: [],
      loading: true,
      method: method,
      more_filters: moreFilters
    };
    this.fetchData = this.fetchData.bind(this);
    this.getOptions = this.getOptions.bind(this);
    this.returnValid = this.returnValid.bind(this);
    console.log("Searchable select is loading");
    this.fetchData("");
  }

  compareFilters(filter1, filter2) {
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

  fetchData(inputValue, pageSize = undefined) {
    // this is going to be the rest api dispatch call that happens
    this.props.fetchData({
      filter: {
        ...this.state.more_filters,
        partial: {
          [this.props.searchField]: inputValue
        }
      },
      page_size: pageSize
    });
  }

  componentWillReceiveProps(nextProps, nextContext) {
    let filter1 = nextProps.moreFilters;
    let filter2 = this.state.more_filters;
    if (!this.compareFilters(filter1, filter2)) {
      this.setState({ more_filters: filter1 }, () => this.fetchData(""));
    }

    if (!getLoading(nextProps.rest_api, this.props.uri, this.state.method, "0")) {
      let options = nextProps.rest_api[this.props.uri][this.state.method]["0"].data.records.map(rec => ({
        label: rec[this.props.searchField],
        value: rec.id,
        more: rec[nextProps.additionalField]
      }));
      if (this.props.hasOwnProperty("additionalOptions")) {
        options.unshift(...this.props.additionalOptions);
      }
      this.setState({
        loading: false,
        options: options
      });
    }
  }

  getOptions(inputValue) {
    // if it is still loading, return [] as options
    // if loading is done, then return the actual options mapped as label and value
    // we just need the api name, the rest we will always look for search "0" endpoint
    if (inputValue !== this.state.input && inputValue !== "") {
      this.setState({ input: inputValue, loading: true }, () => {
        this.fetchData(inputValue);
      });
    }
  }

  returnValid(inputValue, selectValue, selectOptions) {
    if (this.props.creatable === undefined) {
      return false;
    }
    return this.props.creatable && inputValue !== "";
  }

  render() {
    return (
      <CreatableSelect
        {...this.props}
        isLoading={this.state.loading && this.state.input !== ""}
        isMulti={this.props.isMulti}
        closeMenuOnSelect={this.props.closeMenuOnSelect}
        onChange={this.props.onChange}
        filterOption={() => true}
        value={this.props.value}
        options={this.state.options}
        onCreateOption={this.props.onCreateOption}
        onInputChange={debounce(this.getOptions, 500)}
        isValidNewOption={this.returnValid}
        isClearable={this.props.isClearable ? this.props.isClearable : true}
      />
    );
  }
}

SearchableSelect.propTypes = {
  searchField: PropTypes.any.isRequired,
  uri: PropTypes.any.isRequired,
  fetchData: PropTypes.func.isRequired,
  rest_api: PropTypes.any.isRequired,
  noOptionsMessage: PropTypes.any,
  defaultOptions: PropTypes.any,
  additionalField: PropTypes.string
};

//export default connect(mapRestapiStatetoProps,null)(SearchableSelect);

export default SearchableSelect;
