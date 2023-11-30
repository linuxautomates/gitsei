import React, { useState, useEffect, useContext, useMemo, useRef } from "react";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { useLocation } from "react-router-dom";
import { forEach, get, isEqual } from "lodash";
import queryParser from "query-string";
import { engineerCategoryMapping } from "dashboard/pages/scorecard/constants";
import { AntTable } from "shared-resources/components";
import { ouScoreTableConfig } from "./ouTableConfig";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { DevWidgetTimeRangeContext } from "dashboard/pages/context";
import { getGenericRestAPISelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { transformScoreTableData } from "dashboard/pages/scorecard/helper";
import { devProdOrgUnitSelect } from "reduxConfigs/selectors/devProductivity.selector";
import { engineerSectionType } from "dashboard/dashboard-types/engineerScoreCard.types";
import "./ouScoreTableComponent.styles.scss";
import { legendColorByRating } from "../../../helpers/devProductivityRating.helper";
import { engineerRatingType } from "../../../constants/devProductivity.constant";
import { timeInterval } from "../../../constants/devProductivity.constant";
import { getInterval } from "dashboard/graph-filters/components/DevProductivityFilters/constants";
const DEV_ORG_USERS_LIST_ID = "DEV_ORG_USERS_LIST_ID";

interface OUScoreTableComponentProps {
  searchValue: string;
  interval?: string;
}

const OUScoreTableComponent: React.FC<OUScoreTableComponentProps> = ({ searchValue, interval }) => {
  const [apiData, setApiData] = useState<Array<basicMappingType<string | number>>>([]);
  const [apiLoading, setApiLoading] = useState<boolean>();
  const [totalCount, setTotalCount] = useState<number>();
  const [showBlurred, setShowBlurred] = useState<boolean>(true);
  const location = useLocation();
  const { ou_uuid, ou_id } = queryParser.parse(location.search);
  const { dashboardTimeRange } = useContext(DevWidgetTimeRangeContext);
  const apiFiltersRef = useRef<basicMappingType<any>>();
  const dispatch = useDispatch();

  const orgUsersState = useParamSelector(getGenericRestAPISelector, {
    uri: "dev_productivity_user_score_report",
    method: "list",
    uuid: DEV_ORG_USERS_LIST_ID
  });

  const ouScoreState = useParamSelector(devProdOrgUnitSelect, {
    uri: "dev_productivity_org_unit_score_report",
    method: "list",
    id: ou_id
  });

  const apiFilters = useMemo(() => {
    const timeRange = dashboardTimeRange.toUpperCase();
    return {
      filter: {
        ou_ids: [ou_uuid],
        interval: getInterval(timeRange)
      }
    };
  }, [dashboardTimeRange, ou_uuid]);

  const fetchData = () => {
    dispatch(
      genericList("dev_productivity_user_score_report", "list", apiFilters, null, DEV_ORG_USERS_LIST_ID, true, {
        ou_id: ou_uuid
      })
    );
    setApiLoading(true);
  };

  useEffect(() => {
    apiFiltersRef.current = apiFilters;
    fetchData();
  }, []);

  useEffect(() => {
    if (!isEqual(apiFilters, apiFiltersRef.current)) {
      apiFiltersRef.current = apiFilters;
      fetchData();
    }
  }, [apiFilters]);

  useEffect(() => {
    if (apiLoading) {
      const loading = get(orgUsersState, ["loading"], true);
      const error = get(orgUsersState, ["error"], false);
      if (!loading && !error) {
        const result = get(orgUsersState, ["data", "records"], []);
        const metaData = get(orgUsersState, ["data", "_metadata"], {});
        const transformedResult = transformScoreTableData(result);
        const totalCount = get(metaData ?? {}, ["total_count"], 0);
        setTotalCount(totalCount);
        setApiData(transformedResult);
        setApiLoading(false);
      }
    }
  }, [orgUsersState]);

  const getModifiedDataSource = useMemo(() => {
    const orgScoreOverviewRecord = ouScoreState.length ? ouScoreState[0] : {};
    let newApiData = apiData;
    if (Object.keys(orgScoreOverviewRecord).length) {
      const sections: engineerSectionType[] = get(orgScoreOverviewRecord, ["section_responses"], []);
      let categoryMapping: basicMappingType<number | "N/A"> = {};
      forEach(sections, section => {
        const score = get(section, ["score"], "N/A");
        let finalScore = typeof score === "number" ? Math.round(score) : score;
        categoryMapping[engineerCategoryMapping[section?.name] ?? section?.name] = finalScore as any;
      });
      newApiData = newApiData.filter(data =>
        (data?.name + "" || "").toLowerCase().includes((searchValue ?? "").toLowerCase())
      );
      const newCount = newApiData?.length;
      const topScoreRow = { total_score: orgScoreOverviewRecord?.score ?? 0, ...categoryMapping };
      const totalModifiedCount = (newCount ?? 0) + Math.ceil((totalCount ?? 0) / 10);
      let modfiedData = [],
        dataIdx = 0;
      for (let i = 0; i < totalModifiedCount; i += 1) {
        if (i % 10 === 0) {
          modfiedData[i] = topScoreRow;
        } else if (dataIdx < newCount) {
          modfiedData[i] = newApiData[dataIdx];
          dataIdx += 1;
        }
      }
      return modfiedData;
    }
    return newApiData;
  }, [ouScoreState, apiData, searchValue]);

  const supportedSections = useMemo(() => {
    const orgScoreOverviewRecord = ouScoreState.length ? ouScoreState[0] : {};
    if (Object.keys(orgScoreOverviewRecord).length) {
      const sections: engineerSectionType[] = get(orgScoreOverviewRecord, ["section_responses"], []);
      return sections.map(section => section?.name?.toLowerCase());
    }
    return [];
  }, [ouScoreState]);


  return (
    <div className="ou-score-table-container">
      <AntTable
        className="ou-score-table"
        columns={ouScoreTableConfig({
          count: totalCount,
          handleBlurChange: setShowBlurred,
          isBlur: showBlurred,
          sections: supportedSections,
          interval: interval?.toLowerCase(),
          ou_id
        })}
        dataSource={getModifiedDataSource ?? []}
        totalRecords={getModifiedDataSource.length}
      />
      <div className="ou-score-legend">
        {Object.values(engineerRatingType).map(key => {
          return (
            <div className="legend">
              <div className="shape" style={{ backgroundColor: legendColorByRating(key) }} />
              <span className="text">{key.toLowerCase()}</span>
            </div>
          );
        })}
      </div>
    </div>
  );
};

export default OUScoreTableComponent;
