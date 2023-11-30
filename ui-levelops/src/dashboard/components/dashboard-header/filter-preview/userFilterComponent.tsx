import React, { useMemo } from "react";
import { Tag } from "antd";
import { upperCase } from "lodash";
import Loader from "components/Loader/Loader";
import { AntText } from "shared-resources/components";
import { hasValue } from "../helper";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { orgUsersGenericSelector } from "reduxConfigs/selectors/orgUsersSelector";
import { useLocation } from "react-router-dom";
import queryString from "query-string";

interface UserFilterComponentProps {
  filters: any;
}

const UserFilterComponent: React.FC<UserFilterComponentProps> = ({ filters }) => {
  const location = useLocation();

  const version = queryString.parse(location.search).version as string;
  const filtersKeys = useMemo(() => Object.keys(filters || {}), [filters]);
  const userFilterListState = useParamSelector(orgUsersGenericSelector, {
    uri: "org_users_filter",
    method: "list",
    id: `version=${version}`
  });

  const finalFilters = useMemo(() => {
    const final_filters: any = [];

    if (userFilterListState?.loading) {
      return final_filters;
    }
    filtersKeys.forEach((filter_label: any) => {
      if (hasValue(filters[filter_label]) || typeof filters[filter_label] === "boolean") {
        final_filters.push({
          label: filter_label === "full_name" ? "name" : filter_label,
          value: filters[filter_label]
        });
      }
    });
    return final_filters;
  }, [userFilterListState, filters]);

  if (userFilterListState?.loading) {
    return <Loader />;
  }

  return (
    <div>
      {finalFilters.length > 0 &&
        finalFilters.map((item: any) => (
          <div className="global-filters-div-wrapper" key={item.label}>
            <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
              {upperCase(item.label.replace("custom_field_", ""))}
            </AntText>
            {Array.isArray(item?.value) && (
              <div className="global-filters-div">
                {item?.value?.map((filter: any) => {
                  return <Tag key={filter}>{`${filter}`}</Tag>;
                })}
              </div>
            )}
            {["string", "boolean"].includes(typeof item?.value) && (
              <div className="global-filters-div">
                <Tag key={item?.value}>{item?.value?.toString()}</Tag>
              </div>
            )}
            {!Array.isArray(item?.value) && !["string", "boolean"].includes(typeof item?.value) && (
              <div className="global-filters-div">
                <Tag key={item?.value?.$gt}>{`${item?.value?.$gt} - ${item?.value?.$lt}`}</Tag>
              </div>
            )}
          </div>
        ))}
    </div>
  );
};

export default UserFilterComponent;
