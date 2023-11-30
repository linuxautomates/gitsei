import React, { useState } from "react";
import { Select } from "antd";

const { Option, OptGroup } = Select;

export const VariableSingleSelectWrapper = props => {
  const [searchField, setSearchField] = useState("");

  const options = () => {
    if (!props.suggestions) {
      return null;
    }
    const { suggestions, content_type } = props;
    console.log(suggestions);
    console.log(content_type);
    const re = new RegExp(content_type);
    const nodes = suggestions.reduce((arr, obj) => {
      if (!arr.includes(obj.node)) {
        arr.push(obj.node);
        return arr;
      }
      return arr;
    }, []);

    const filteredSuggestions = suggestions.filter(
      suggestion =>
        content_type === undefined ||
        (suggestion.content_type && suggestion.content_type === content_type) ||
        (suggestion.content_type && suggestion.content_type.match(re) !== null)
    );

    let treeNodes = nodes.map((node, index) => (
      <OptGroup key={index} label={node}>
        {suggestions
          .filter(
            suggestion =>
              suggestion.node === node &&
              (content_type === undefined ||
                (suggestion.content_type && suggestion.content_type === content_type) ||
                (suggestion.content_type && suggestion.content_type.match(re) !== null))
          )
          .map(menu => (
            <Option key={menu.key} value={menu.key}>
              {menu.value}{" "}
            </Option>
          ))}
      </OptGroup>
    ));
    if (
      searchField !== "" &&
      filteredSuggestions.find(suggestion => suggestion.key.includes(searchField)) === undefined
    ) {
      treeNodes.push(
        <Option key={searchField} value={searchField}>
          {searchField}
        </Option>
      );
    }

    return treeNodes;
  };

  return (
    <Select
      {...props}
      showSearch={true}
      onSearch={value => setSearchField(value)}
      getPopupContainer={trigger => trigger.parentNode}>
      {options()}
    </Select>
  );
};
