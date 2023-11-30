import Tag from "antd/lib/tag";
import Loader from "components/Loader/Loader";
import { get, upperCase } from "lodash";
import React, { useMemo } from "react";
import { useEffect, useState } from "react";
import { useDispatch } from "react-redux";
import { widgetFilterValuesGet } from "reduxConfigs/actions/restapi";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntText } from "shared-resources/components";
import { hasValue } from "./helper";

interface MicrosoftGlobalFiltersProps {
  microSoftFilter: any;
  integrationIds: any;
}

const MicrosoftGlobalFilters: React.FC<MicrosoftGlobalFiltersProps> = ({ microSoftFilter, integrationIds }) => {
  const microsoftFiltersKeys = useMemo(() => Object.keys(microSoftFilter || {}), [microSoftFilter]);
  const dispatch = useDispatch();
  const [microValuesFilters, setMicroValuesFilters] = useState<any>([]);
  const [loading, setLoading] = useState<boolean>(false);
  const microsoftFiltersState = useParamSelector(getGenericRestAPISelector, {
    uri: "microsoft_issues_filter_values",
    method: "list",
    uuid: "mirco_values"
  });

  useEffect(() => {
    const loading = get(microsoftFiltersState, ["loading"], true);
    const error = get(microsoftFiltersState, ["error"], true);
    if (!loading && !error) {
      const data = get(microsoftFiltersState, ["data", "records"], []);
      setMicroValuesFilters(data);
      setLoading(false);
    }
  }, [microsoftFiltersState]);

  useEffect(() => {
    const data = get(microsoftFiltersState, ["data", "records"], []);
    if (data.length === 0 && !loading) {
      if (microsoftFiltersKeys.includes("tag") || microsoftFiltersKeys.includes("projects")) {
        dispatch(
          widgetFilterValuesGet(
            "microsoft_issues_filter_values" || "",
            { fields: ["project", "tag"], integration_ids: integrationIds },
            null,
            "mirco_values"
          )
        );
        setLoading(true);
      }
    }
  }, [microsoftFiltersKeys, microsoftFiltersState]);

  const finalFilters = useMemo(() => {
    const final_Filters: any = [];

    microsoftFiltersKeys.forEach((filter_label: any) => {
      switch (filter_label) {
        case "tag":
          if (microValuesFilters.length) {
            let values: any = [];
            microSoftFilter[filter_label].forEach((item: any) => {
              const index = microValuesFilters.findIndex((obj: any) => Object.keys(obj).includes("tag"));
              if (index !== -1) {
                const object = microValuesFilters[index]["tag"].find((val: any) => val.key === item);
                values.push(object.value);
              }
            });
            if (values.length) {
              final_Filters.push({
                label: filter_label,
                value: values
              });
            }
          }
          break;
        case "projects":
          if (microValuesFilters.length) {
            let values: any = [];
            microSoftFilter[filter_label].forEach((item: any) => {
              const index = microValuesFilters.findIndex((obj: any) => Object.keys(obj).includes("project"));
              if (index !== -1) {
                const object = microValuesFilters[index]["project"].find((val: any) => val.key === item);
                values.push(object.value);
              }
            });
            if (values.length) {
              final_Filters.push({
                label: filter_label,
                value: values
              });
            }
          }

          break;
        default:
          if (hasValue(microSoftFilter[filter_label])) {
            final_Filters.push({
              label: filter_label,
              value: microSoftFilter[filter_label]
            });
          }
          break;
      }
    });
    return final_Filters;
  }, [microValuesFilters, microSoftFilter]);

  if (finalFilters.length === 0 && loading) {
    return <Loader />;
  }

  return (
    <div>
      {finalFilters.length > 0 &&
        finalFilters.map((item: any) => (
          <div className="global-filters-div-wrapper" key={item.label}>
            <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
              {upperCase(item.label)}
            </AntText>
            {(item.exclude || item.partial) && (
              <AntText style={{ fontSize: "10px" }}>
                {item.exclude ? "Excludes" : `Includes all the values that: ${item.partial}`}
              </AntText>
            )}
            {
              <div className="global-filters-div">
                {item?.value?.map((filter: any) => {
                  return <Tag key={filter}>{`${filter}`}</Tag>;
                })}
              </div>
            }
          </div>
        ))}
    </div>
  );
};

export default MicrosoftGlobalFilters;
