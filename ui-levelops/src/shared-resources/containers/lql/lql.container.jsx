import React from "react";
import * as PropTypes from "prop-types";
import ReactFilterBox, { GridDataAutoCompleteHandler } from "react-filter-box";
import { OPERATORS, STRING_OPERATORS, TABLE_OPERATORS, PARTIAL_OPERATORS } from "constants/queryPredicates";
import CustomCheckbox from "components/CustomCheckbox/CustomCheckbox";

class CustomAutoComplete extends GridDataAutoCompleteHandler {
  // override this method to add new your operator
  // you can filter and return operators for only integer type, etc right here

  constructor(data, options, filter = "") {
    super(data, options);
    this._filter = filter;
    this._options = options;
  }

  set filter(filter) {
    this._filter = filter;
  }

  needCategories() {
    let categories = super.needCategories();
    if (this._filter) {
      return categories.filter(category => category.includes(this._filter));
    }
    return categories;
  }

  needOperators(parsedCategory) {
    let type = "text";
    for (let i = 0; i < this._options.length; i++) {
      if (this._options[i].columnField === parsedCategory) {
        type = this._options[i].type;
        break;
      }
    }
    switch (type) {
      case "text":
        return STRING_OPERATORS;
      case "number":
        return OPERATORS;
      case "table":
        return TABLE_OPERATORS;
      case "partial":
        return PARTIAL_OPERATORS;
      default:
        return OPERATORS;
    }
  }

  //override to custom to indicate you want to show your custom date time
  needValues(parsedCategory, parsedOperator) {
    // parsed operator is IN, then

    let suggestions = [];
    for (let i = 0; i < this._options.length; i++) {
      if (this._options[i].columnField === parsedCategory) {
        suggestions = this._options[i].suggestions;
        break;
      }
    }

    // no autosuggest multiselect needed if there is no suggestions in the first place
    if ((parsedOperator === "in" || parsedOperator === "nin") && suggestions !== undefined && suggestions.length > 0) {
      return [suggestions];
      //return(suggestions.map(suggest => `_contains_${suggest}`))
    }
    return suggestions;

    //return super.needValues(parsedCategory, parsedOperator);
  }
}

export class LQLContainer extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      query: this.props.query || this.props.value || "",
      query_valid: true,
      handler: new CustomAutoComplete([], this.props.predicates, this.props.filter)
    };
    this.customRenderCompletionItem = this.customRenderCompletionItem.bind(this);
    this.updateQueryValid = this.updateQueryValid.bind(this);
    this.handleSyntaxMultiSelect = this.handleSyntaxMultiSelect.bind(this);
    this.handleParseOK = this.handleParseOK.bind(this);
  }

  componentWillReceiveProps(nextProps, nextContext) {
    let handler = this.state.handler;
    if (this.props.filter !== nextProps.filter) {
      handler.filter = nextProps.filter;
      this.setState({ handler: handler });
    }
    // if(this.props.query !== this.state.query) {
    //     this.setState({query: this.props.query})
    // }
  }

  handleSyntaxMultiSelect(e) {
    e.preventDefault();
  }

  customRenderCompletionItem(self, data, registerAndGetPickFunc) {
    var className = "text-default";
    switch (data.type) {
      case "category":
        className = "text-info";
        break;
      case "operator":
        className = "text-default";
        break;
      case "literal":
        if (this.props.hasOwnProperty("table") && this.props.table) {
          if (data.value === "(" || data.value === ")" || data.value === "OR") {
            return "";
          }
        }
        className = "text-default";
        break;
      default:
        className = "text-danger";
        break;
    }

    if (data.value.toString().includes("_contains")) {
      let pick = registerAndGetPickFunc();
      let value = data.value.split("_contains_").pop();
      return (
        <div className={className}>
          <span
            onClick={e => {
              e.stopPropagation();
            }}>
            <CustomCheckbox
              id={value}
              label={value}
              number={value}
              customhandler={e => {
                e.preventDefault();
                e.persist();
                e.stopPropagation();
                //this.setState({query: this.state.query.concat(`${value},`)})
                pick(value.concat(","));
              }}
            />
          </span>
        </div>
      );
    }

    if (Array.isArray(data.value)) {
      //let value = data.value.split("_contains_").pop();
      return (
        <div className={className}>
          <span>
            <form>
              <select
                className="react-select border-0 content-no-padding"
                style={{ padding: 0, border: 0, zIndex: 999 }}
                multiple={true}
                onChange={this.handleSyntaxMultiSelect}
                onClick={e => {
                  //pick(e.target.value.concat(","))
                  //let tokens = this.state.query.split(" ");
                  let tokens = this.props.query.split(" ");
                  let lastToken = tokens.pop();
                  //let updatedQuery = this.state.query;
                  let updatedQuery = this.props.query;
                  if (lastToken === "") {
                    // this means no selection has been made
                    updatedQuery = updatedQuery.concat(`[${e.target.value}]`);
                  } else {
                    let inValues = lastToken.replace("[", "").replace("]", "").split(",");
                    inValues = inValues.filter(value => value !== "");
                    if (!inValues.includes(e.target.value)) {
                      // avoid duplicates
                      inValues.push(e.target.value);
                      let newToken = `[${inValues.join(",")}]`;
                      tokens.push(newToken);
                      updatedQuery = tokens.join(" ");
                    }
                  }
                  this.setState({ query: updatedQuery }, () =>
                    this.props.onChange(updatedQuery, { isError: false, name: "" }, this.props.id)
                  );
                }}>
                {data.value.map(option => (
                  <option value={option} key={option}>
                    {option}
                  </option>
                ))}
              </select>
            </form>
          </span>
        </div>
      );
    }

    return (
      <div className={className}>
        <span style={{ fontWeight: "bold" }}>{data.value}</span>
      </div>
    );
  }

  updateQueryValid(query, result) {
    //this.setState({query: query,query_valid:result.isError});
    if (this.props.onChange) {
      this.props.onChange(query, result, this.props.id);
    }
  }

  handleParseOK(expressions) {
    console.log(expressions);
  }

  render() {
    return (
      <div>
        <ReactFilterBox
          autoCompleteHandler={this.state.handler}
          customRenderCompletionItem={this.customRenderCompletionItem.bind(this)}
          //query={this.props.query || this.state.query}
          query={this.state.query}
          onChange={this.updateQueryValid}
          onParseOk={this.handleParseOK}
          editorConfig={{
            lineWrapping: true
          }}
        />
      </div>
    );
  }
}

LQLContainer.propTypes = {
  onChange: PropTypes.func.isRequired,
  predicates: PropTypes.array.isRequired,
  id: PropTypes.string.isRequired
};

LQLContainer.defaultProps = {};

//export default LQL;
