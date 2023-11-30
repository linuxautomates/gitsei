import React from "react";
import { SelectRestapiHelperWrapper as SelectRestapi } from "shared-resources/helpers/select-restapi/select-restapi.helper.wrapper";

export const DynamicCustomSelectComponent = props => {
  const searchField = props.search_field || "name";
  const suggestionOptions = (props.suggestions || []).map(suggestion => ({
    id: `custom|${suggestion.key}`,
    [searchField]: `${suggestion.node} - ${suggestion.value}`
  }));
  return (
    <SelectRestapi
      //labelAsValue
      labelInValue={true}
      {...props}
      additionalOptions={suggestionOptions}
      mode={props.mode ? props.mode : "single"}
      uri={props.dynamic_resource_name}
      searchField={props.search_field}
      createOption
      createPrefix={"custom|"}
    />
  );
};
