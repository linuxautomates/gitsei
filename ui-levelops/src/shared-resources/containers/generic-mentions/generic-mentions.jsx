import React from "react";
import { Mentions } from "antd";
import * as PropTypes from "prop-types";
import { debounce } from "lodash";
import { getMentionsSearchResults } from "reduxConfigs/selectors/restapiSelector";
import { mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { mapGenericToProps } from "reduxConfigs/maps/restapi";
import ErrorWrapper from "hoc/errorWrapper";
import { connect } from "react-redux";

class GenericMentions extends React.PureComponent {
  static getDerivedStateFromProps(props, state) {
    if (state.options_loading) {
      const { loading, error, data: optionsData } = props.options;
      if (!loading && !error && optionsData !== undefined) {
        const users = optionsData.records;
        return {
          options: users,
          options_loading: false
        };
      }
    }

    return null;
  }

  componentDidMount() {
    this.setState({ options_loading: true }, () => {
      if (this.props.loadOnMount) {
        this.loadOptions("");
      }
    });
  }

  componentWillUnmount() {
    if (this.props.loadOnMount) {
      this.props.restapiClear(this.props.uri, this.props.method, this.props.id);
    }
  }

  constructor(props) {
    super(props);
    this.state = {
      options: [],
      options_loading: false,
      last_query: undefined
    };
    this.onSearch = this.onSearch.bind(this);
    this.handleChange = this.handleChange.bind(this);
    this.loadOptions = debounce(this.loadOptions.bind(this), 800);
  }

  loadOptions(query) {
    const { uri, method, id, searchField } = this.props;
    this.props.genericList(
      uri,
      method,
      {
        filter: {
          partial: {
            [searchField]: query
          }
        }
      },
      null,
      id
    );
    this.setState({ last_query: query });
  }

  onSearch(query) {
    this.setState(
      state => ({
        //options: [],
        options_loading: true
      }),
      () => {
        this.loadOptions(query);
      }
    );
  }

  handleChange(comment) {
    this.setState({ options_loading: true }, () => this.loadOptions(""));
    if (!this.props.onChange) {
      console.error("[GenericMentions] onChange not implemented");
      return;
    }

    this.props.onChange(comment);
  }

  getOptionValue = option => {
    const { searchField, optionValueField, optionValueTransformer } = this.props;

    if (!!optionValueField) {
      return option[optionValueField];
    }

    if (!!optionValueTransformer) {
      return optionValueTransformer(option);
    }

    return option[searchField];
  };

  render() {
    const { Option } = Mentions;

    const { placeholder, value, searchField, prefix, optionRenderer, disabled, optionKeyField } = this.props;

    return (
      <Mentions
        prefix={prefix}
        //split={split}
        rows={"3"}
        value={value}
        placeholder={placeholder}
        style={{ width: "100%" }}
        loading={this.state.options_loading}
        onChange={this.handleChange}
        disabled={disabled}
        onSearch={query => {
          this.onSearch(query);
        }}>
        {this.state.options.map(option => (
          <Option
            key={optionKeyField ? option[optionKeyField] : option[searchField]}
            value={this.getOptionValue(option)}>
            {optionRenderer && optionRenderer(option, option[searchField])}
            {!optionRenderer && option[searchField]}
          </Option>
        ))}
      </Mentions>
    );
  }
}

GenericMentions.propTypes = {
  placeholder: PropTypes.string,
  value: PropTypes.string.isRequired,
  id: PropTypes.string.isRequired,
  uri: PropTypes.string.isRequired,
  method: PropTypes.string.isRequired,
  searchField: PropTypes.string.isRequired,
  onChange: PropTypes.func.isRequired,
  disabled: PropTypes.bool.isRequired,
  prefix: PropTypes.string,
  split: PropTypes.string,
  optionRenderer: PropTypes.func, // (entity, searchField) => JSX
  optionKeyField: PropTypes.string,
  optionValueField: PropTypes.string,
  optionValueTransformer: PropTypes.func,
  loadOnMount: PropTypes.bool
};

GenericMentions.defaultProps = {
  id: "mentions",
  value: "",
  prefix: "@",
  split: "",
  disabled: false,
  loadOnMount: true
};

const mapStateToProps = (state, ownProps) => {
  return {
    options: getMentionsSearchResults(state, ownProps.uri, ownProps.method, ownProps.id)
  };
};

const dispatchToProps = dispatch => {
  return {
    ...mapRestapiDispatchtoProps(dispatch),
    ...mapGenericToProps(dispatch)
  };
};

export default ErrorWrapper(connect(mapStateToProps, dispatchToProps)(GenericMentions));
