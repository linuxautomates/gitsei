import { Tag } from "antd";
import { upperCase } from "lodash";
import moment from "moment";
import React from "react";
import { AntText } from "shared-resources/components";
import cx from "classnames";
import "./JiraOrFiltersContainer.scss";

const jira_time_range_filter = [
  "issue_updated_in",
  "issue_created_in",
  "jira_issue_created_in",
  "jira_issue_updated_in"
];
const storypointLabel = ["parent_story_points", "story_points"];

interface JiraOrFiltersContainerProps {
  jiraOrFilters: any[];
  filterWidth?: "half" | "full";
}

const JiraOrFiltersContainer: React.FC<JiraOrFiltersContainerProps> = ({ jiraOrFilters, filterWidth }) => (
  <>
    {jiraOrFilters.map((item: any) => (
      <div className={cx("global-filters-div-wrapper", { "half-width": filterWidth === "half" })} key={item.label}>
        <AntText className={"global-filters-div-wrapper-label"}>{upperCase(item.label)}</AntText>
        {(item.exclude || item.partial) && (
          <AntText style={{ fontSize: "10px" }}>
            {item.exclude ? "Excludes" : `Includes all the values that: ${item.partial}`}
          </AntText>
        )}
        {!storypointLabel.includes(item.label) &&
          !jira_time_range_filter.includes(item.label) &&
          item.value &&
          Array.isArray(item.value) && (
            <div className="global-filters-div">
              {item?.value?.map((filter: any) => {
                return <Tag key={filter}>{`${filter}`}</Tag>;
              })}
            </div>
          )}
        {storypointLabel.includes(item.label) && (
          <div className="global-filters-div">
            {
              <div>
                <Tag>{`${item.value[0]} - ${item.value[1]}`}</Tag>
              </div>
            }
          </div>
        )}
        <div className="global-filters-div">
          {jira_time_range_filter.includes(item?.label) && (
            <Tag key={item.label} className="widget-default-filters-list_tags">
              {`${moment.unix(parseInt(item?.value[0])).utc().format("MM-DD-YYYY")} `} -{" "}
              {` ${moment.unix(parseInt(item?.value[1])).utc().format("MM-DD-YYYY")}`}
            </Tag>
          )}
        </div>
      </div>
    ))}
  </>
);

JiraOrFiltersContainer.defaultProps = {
  filterWidth: "full"
};
export default JiraOrFiltersContainer;
