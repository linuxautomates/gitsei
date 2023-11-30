import React, { useMemo } from "react";
import { Empty, Tag } from "antd";
import { AntBadge, AntText } from "shared-resources/components";
import { upperCase, forEach, get, cloneDeep } from "lodash";
import "./DemoWidgetFilterPreviewWrapper.style.scss";
import { toTitleCase } from "utils/stringUtils";
import { getDateRangeEpochToString } from "utils/dateUtils";

interface DemoWidgetFilterPreviewWrapperProps {
  widgetData: any;
}

const DemoWidgetFilterPreviewWrapper: React.FC<DemoWidgetFilterPreviewWrapperProps> = (
  props: DemoWidgetFilterPreviewWrapperProps
) => {
  const { widgetData } = props;
  const { query } = widgetData;

  /** This function seriralizes the object and return a non nested object */
  const transformQuery = (query: any) => {
    let nQuery: { [x: string]: Array<string> | string | boolean } = {};
    const ignoreKeys = ["exclude", "partial_match", "across"]; // currently excluding them from widget filter preview.
    forEach(Object.keys(query), (key: string) => {
      if (!ignoreKeys.includes(key)) {
        const value = get(query, [key]);
        if (key === "time_range") {
          nQuery[key] = getDateRangeEpochToString(value);
        } else if (value && typeof value === "object" && !Array.isArray(value)) {
          const childQuery = transformQuery(value);
          nQuery = {
            ...(nQuery ?? {}),
            ...(childQuery ?? {})
          };
        } else {
          nQuery[key] = value;
        }
      }
    });
    return nQuery;
  };

  const finalQuery = useMemo(() => (query ? transformQuery(cloneDeep(query)) : query), [query]);

  const renderWidgetHeader = useMemo(() => {
    return (
      <div className="widget-filter-container">
        <div className="widget-filter-header">
          <div className="filters-count-container">
            <AntText className="filters-label">
              Widget Filters ({(query && Object.keys(finalQuery).length) || 0})
            </AntText>
          </div>
        </div>
      </div>
    );
  }, [widgetData, finalQuery]);

  const renderDashboardHeader = useMemo(() => {
    return (
      <div className="widget-filter-header">
        <div className="filters-count-container">
          <div className="filters-label-container">
            <AntText className="filters-label">Dashboard Filters</AntText>
          </div>
          <AntBadge className="filters-count" count={0} overflowCount={1000} />
        </div>
      </div>
    );
  }, []);

  const renderWidgetFilters = useMemo(() => {
    if (!finalQuery) {
      return <Empty className="filter-placeholder" description="No Filters" />;
    }
    return (
      finalQuery &&
      Object.keys(finalQuery).map((filter: any, index: number) => {
        const value = get(finalQuery, [filter], "");
        return (
          <div className="widget-filter" key={`${filter}-${index}`}>
            <AntText className={"widget-filter_label"}>{upperCase(filter)}</AntText>
            <div>
              {Array.isArray(value) &&
                value.map((filterValue: any, index: number) => {
                  return (
                    <Tag key={`filterValue-${index}`} className="widget-filter_tags">
                      {toTitleCase(filterValue)}
                    </Tag>
                  );
                })}
              {!Array.isArray(value) && (
                <Tag key={`filterValue-${index}`} className="widget-filter_tags">
                  {toTitleCase(value)}
                </Tag>
              )}
            </div>
          </div>
        );
      })
    );
  }, []);

  return (
    <div className="demo-widget-filters-wrapper">
      <div className={""}>
        {renderDashboardHeader}
        <div className="db-filter-container"></div>
      </div>
      {renderWidgetHeader}
      <div className="widget-filters-list">{renderWidgetFilters}</div>
    </div>
  );
};

export default DemoWidgetFilterPreviewWrapper;
