import React from "react";
import { DynamicSelectComponent } from "./dynamic-select.component";

export const DynamicCustomMultiSelectWrapper = props => {
  const searchField = props.search_field || "name";
  const suggestionOptions = (props.suggestions || []).map(suggestion => ({
    id: `custom|${suggestion.key}`,
    [searchField]: `${suggestion.node} - ${suggestion.value}`
  }));
  return (
    <DynamicSelectComponent
      additionalOptions={suggestionOptions}
      {...props}
      mode={"multiple"}
      createOption
      createPrefix={"custom|"}
    />
  );
};
