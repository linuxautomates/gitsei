import React, { useCallback } from "react";
import { get, upperCase } from "lodash";
import widgetConstants from "../../constants/widgetConstants";
import moment from "moment";
import { Tag } from "antd";
import { AntText } from "../../../shared-resources/components";
import LevelopsApiFilterValues from "./LevelopsApiFilterValues";
import { isvalidTimeStamp } from "utils/dateUtils";

interface LevelopsFiltersPreviewProps {
  filters: any;
  integrationIds: string[];
  reportType: string;
  supportedFilters?: any[];
  dateInTimeStamp?: boolean;
}

const LevelopsFiltersPreview: React.FC<LevelopsFiltersPreviewProps> = props => {
  const { filters, reportType, dateInTimeStamp = true } = props;
  const supportedFilters = get(widgetConstants, [reportType, "supported_filters"], props.supportedFilters || []);
  const getFilterDetails = (key: string) => {
    return supportedFilters.find((filter: any) => filter.filterField === key);
  };

  const isValidTimestamp = useCallback((filter: any) => {
    return (
      isvalidTimeStamp(filter?.["$gt"] || filter?.["$gte"]) || isvalidTimeStamp(filter?.["$lt"] || filter?.["$lte"])
    );
  }, []);

  return (
    <>
      {Object.keys(filters).map((filter_key: string) => {
        const filterDetails = getFilterDetails(filter_key);
        const label = (
          <AntText style={{ color: "#595959", fontSize: "10px", fontWeight: "bold" }}>
            {upperCase(filterDetails?.label)}
          </AntText>
        );
        switch (filter_key) {
          case "completed":
            return (
              <div className={"global-filters-div-wrapper"} key={filter_key}>
                {label}
                <div>
                  <Tag key={filterDetails?.label} className="widget-filter_tags">
                    {filters[filter_key] ? "COMPLETED" : "NOT COMPLETED"}
                  </Tag>
                </div>
              </div>
            );
          case "updated_at":
          case "created_at":
            return (
              <div className={"global-filters-div-wrapper"} key={filter_key}>
                {label}
                {isValidTimestamp(filters?.[filter_key]) && (
                  <div>
                    <Tag key={filterDetails?.label} className="widget-filter_tags">
                      {`${
                        filters?.[filter_key]?.["$gt"] || filters?.[filter_key]?.["$gte"]
                          ? moment
                              .unix(parseInt(filters?.[filter_key]?.["$gt"] || filters?.[filter_key]?.["$gte"]))
                              .utc()
                              .format("MM-DD-YYYY")
                          : "NA"
                      } `}{" "}
                      -{" "}
                      {` ${
                        filters?.[filter_key]?.["$lt"] || filters?.[filter_key]?.["$lte"]
                          ? moment
                              .unix(parseInt(filters?.[filter_key]?.["$lt"] || filters?.[filter_key]?.["$lte"] || "NA"))
                              .utc()
                              .format("MM-DD-YYYY")
                          : "NA"
                      }`}
                    </Tag>
                  </div>
                )}
                {!isValidTimestamp(filters?.[filter_key]) && (
                  <div>
                    <Tag key={filterDetails?.label} className="widget-filter_tags">
                      {`${filters?.[filter_key]?.["$gt"] || filters?.[filter_key]?.["$gte"] || "NA"} `} -{" "}
                      {` ${filters?.[filter_key]?.["$lt"] || filters?.[filter_key]?.["$lte"] || "NA"}`}
                    </Tag>
                  </div>
                )}
              </div>
            );
          case "questionnaire_template_id":
          case "tags":
          case "products":
          case "reporters":
          case "assignees":
          case "states":
            return (
              <div className={"global-filters-div-wrapper"} key={filter_key}>
                {label}
                <div>
                  <LevelopsApiFilterValues
                    filterValues={filters[filter_key] || []}
                    reportType={reportType}
                    uri={filterDetails?.uri}
                    labelField={filterDetails?.searchField || "name"}
                  />
                </div>
              </div>
            );
          case "product_ids":
          case "tag_ids":
          case "assignee_user_ids":
            const values = (filters[filter_key] || []).map((item: any) => item.key);
            return (
              <div className={"global-filters-div-wrapper"} key={filter_key}>
                {label}
                <div>
                  <LevelopsApiFilterValues
                    filterValues={values}
                    reportType={reportType}
                    uri={filterDetails?.uri}
                    labelField={filterDetails?.searchField || "name"}
                  />
                </div>
              </div>
            );
          case "reporter":
          case "status":
            return (
              <div className={"global-filters-div-wrapper"} key={filter_key}>
                {label}
                <div>
                  <LevelopsApiFilterValues
                    filterValues={[filters[filter_key]] || []}
                    reportType={reportType}
                    uri={filterDetails?.uri}
                    labelField={filterDetails?.searchField || "name"}
                  />
                </div>
              </div>
            );
          default:
            return <></>;
        }
      })}
    </>
  );
};

export default LevelopsFiltersPreview;
