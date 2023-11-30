import React from "react";
import { SelectRestapiHelperWrapper as SelectRestapi } from "shared-resources/helpers/select-restapi/select-restapi.helper.wrapper";
import { get, forEach } from "lodash";

export const DynamicSelectComponent = props => {
  const moreFilters = () => {
    const filters = get(props, "filters", []);
    if (filters.length) {
      let mappedFilters = {};
      forEach(filters, filter => {
        if (props.dynamic_resource_name === "integrations") {
          mappedFilters[filter.key] = filter.default_value === "true";
        } else {
          mappedFilters[filter.key] = filter.default_value;
        }
      });
      return mappedFilters;
    }
    return {};
  };

  return (
    <SelectRestapi
      labelInValue={true}
      {...props}
      moreFilters={moreFilters()}
      mode={props.mode ? props.mode : "single"}
      uri={props.dynamic_resource_name === "playbooks" ? "propels" : props.dynamic_resource_name} // Ideally ticket https://levelops.atlassian.net/browse/LEV-5132 should change dynamic_resource_name from playbooks to propels as values coming from BE only. Once LEV-5132 is fixed then we can remove this check. Due to BE bandwidth issue, adding FE side check.
      searchField={props.search_field}
    />
  );
};
