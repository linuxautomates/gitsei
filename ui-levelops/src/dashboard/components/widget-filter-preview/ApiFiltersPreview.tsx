import React, { useEffect, useMemo, useState } from "react";
import { get, upperCase, difference, uniq } from "lodash";
import { widgetFilterValuesGet } from "reduxConfigs/actions/restapi";
import { useDispatch } from "react-redux";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntText } from "../../../shared-resources/components";
import { Tag } from "antd";
import { getWidgetConstant } from "dashboard/constants/widgetConstants";

interface ApiFiltersPreviewProps {
  filters: { key: string; label: string; value: any; exclude?: boolean; partial?: string }[];
  uri: string;
  reportType: string;
  integrationIds: string[];
  widgetId?: string;
}

const apiFiltersId = "api_filters_id";

const ApiFiltersPreview: React.FC<ApiFiltersPreviewProps> = ({
  filters,
  uri,
  reportType,
  integrationIds,
  widgetId
}) => {
  const dispatch = useDispatch();

  const [loading, setLoading] = useState<boolean>(false);
  const [filterValueData, setFilterValueData] = useState<any>([]);

  const fieldKeyMap = useMemo(() => getWidgetConstant(reportType, "FIELD_KEY_FOR_FILTERS", {}), [reportType]);

  const uniqueSelectorId = !!widgetId ? widgetId : apiFiltersId;

  const apiFilterPreviewFiltersState = useParamSelector(getGenericRestAPISelector, {
    uri: uri,
    method: "list",
    uuid: uniqueSelectorId
  });

  useEffect(() => {
    if (filters.length) {
      const fieldList = filters.map(filter => get(fieldKeyMap, [filter.key], filter.key));
      const data = get(apiFilterPreviewFiltersState, ["data", "records"], []);
      const dataKeys = data?.reduce((acc: string[], next: any) => [...acc, Object.keys(next)[0]], []);
      if ((data.length === 0 || (data.length && difference(fieldList, dataKeys).length)) && !loading) {
        dispatch(
          widgetFilterValuesGet(
            uri,
            {
              fields: uniq([...fieldList, ...difference(fieldList, dataKeys)]),
              filter: { integration_ids: integrationIds },
              integration_ids: integrationIds
            },
            null,
            uniqueSelectorId
          )
        );
        setLoading(true);
      }
    }
  }, [uri, filters, reportType, integrationIds, apiFilterPreviewFiltersState]);

  useEffect(() => {
    const loading = get(apiFilterPreviewFiltersState, ["loading"], true);
    const error = get(apiFilterPreviewFiltersState, ["error"], true);
    if (!loading && !error) {
      const data = get(apiFilterPreviewFiltersState, ["data", "records"], []);
      setFilterValueData(data);
      setLoading(false);
    }
  }, [apiFilterPreviewFiltersState]);

  const finalFiltersArray = useMemo(() => {
    return filters.map(filter => {
      const keyValueMap = filterValueData.find((data: any) => {
        const key = get(fieldKeyMap, [filter.key], filter.key);
        return key === Object.keys(data)[0];
      });
      if (keyValueMap) {
        return {
          ...filter,
          value: (filter?.value || []).map((val: string) => {
            const finalData = (
              (Array.isArray(Object.values(keyValueMap))
                ? Object.values(keyValueMap)[0]
                : Object.values(keyValueMap)) as any
            ).find((fValue: any) => {
              const key = get(fieldKeyMap, [filter?.key], undefined);
              if (key && fValue?.[key]) {
                return fValue[key] === val;
              }
              return fValue.key === val;
            });
            const keyForValue = getWidgetConstant(reportType, ["key_for_filter_value", filter?.key], "additional_key");
            return finalData ? (finalData as any)[keyForValue] || val : val;
          })
        };
      }
      return {
        ...filter
      };
    });
  }, [filterValueData, filters, reportType]);
  return (
    <>
      {finalFiltersArray.map(filter => {
        return (
          <div className="widget-filter" key={filter.key}>
            <AntText className={"widget-filter_label"}>{upperCase(filter.label)}</AntText>
            {(filter.exclude || filter.partial) && (
              <>
                {filter.exclude && <AntText className={"widget-filter_extras"}>Excludes</AntText>}
                {filter.partial && (
                  <AntText className={"widget-filter_extras"}>
                    {`Includes all the values that: ${filter.partial}`}
                  </AntText>
                )}
              </>
            )}
            <div>
              {filter?.value?.map((filter_val: any) => {
                return <Tag key={filter_val} className="widget-filter_tags">{`${filter_val}`}</Tag>;
              })}
            </div>
          </div>
        );
      })}
    </>
  );
};

export default ApiFiltersPreview;
