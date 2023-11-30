import React, { ReactElement, ReactNode } from "react";
import { ColumnProps } from "antd/lib/table";
import { basicMappingType } from "dashboard/dashboard-types/common-types";
import { engineerCategoryIconsMapping, engineerCategoryType } from "dashboard/pages/scorecard/constants";
import { TitleWithCount } from "shared-resources/components";
import { Icon } from "antd";
import { Link, useParams } from "react-router-dom";
import { WebRoutes } from "routes/WebRoutes";
import { ouScoreTableTransformer } from "dashboard/graph-filters/components/sort.helper";
import { legendColorByScore } from "../../../helpers/devProductivityRating.helper";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";
import { get } from "lodash";
import { ProjectPathProps } from "classes/routeInterface";

const getScoreColumnRender = (score: number, record: basicMappingType<any>, index: number) => {
  if (index === 0) {
    return (
      <div className="flex justify-center">
        <div style={{ backgroundColor: legendColorByScore(score) }} className="designed-score">
          {score}
        </div>
      </div>
    );
  }
  return (
    <div className="score">
      <div className="color-dot" style={{ backgroundColor: legendColorByScore(score) }} />
      {score}
    </div>
  );
};
const ScoreCardLink = ({
  user_id,
  name,
  interval,
  ou_id
}: {
  user_id: string;
  interval: string | null;
  name?: ReactNode;
  ou_id?: string;
}) => {
  const projectParams = useParams<ProjectPathProps>();
  const url = WebRoutes.dashboard.scorecard(projectParams, user_id, interval, undefined, ou_id);
  return (
    <Link to={url} target="_blank">
      {name}
    </Link>
  );
};
export const ouScoreTableConfig = (utitlity: basicMappingType<any>): ColumnProps<any>[] => {
  return [
    {
      title: <TitleWithCount title="Name" count={utitlity.count ?? 0} />,
      dataIndex: "name",
      key: "name",
      width: "10%",
      render: (item: string, record, index) => {
        if (!record?.ou_user_uuid) {
          return (
            <div className="blur-names-container" onClick={e => utitlity.handleBlurChange((prev: boolean) => !prev)}>
              <Icon type={utitlity?.isBlur ? "eye-invisible" : "eye"} className="blur-icon" />
              Show Names
            </div>
          );
        }
        if (utitlity.isDemo) {
          return (
            <a>
              <span className={utitlity.isBlur ? "blurred-view" : ""}>{item}</span>
            </a>
          );
        } else {
          return (
            <ScoreCardLink
              user_id={record?.ou_user_uuid}
              interval={utitlity?.interval}
              name={<span className={utitlity.isBlur ? "blurred-view" : ""}>{item}</span>}
              ou_id={utitlity?.ou_id}
            />
          );
        }
      }
    },
    {
      title: "Overall",
      dataIndex: "total_score",
      key: "total_score",
      width: "10%",
      sorter: ouScoreTableTransformer("total_score"),
      sortDirections: ["ascend", "descend"],
      render: getScoreColumnRender
    },
    ...Object.values(engineerCategoryType)
      .filter(section => (utitlity?.sections || [])?.includes(section.toLowerCase()))
      .map(
        key =>
          ({
            title: (
              <div className="category-header">
                <img src={engineerCategoryIconsMapping()[key]} alt="icon" />
                <span className="category-title">{get(TRELLIS_SECTION_MAPPING, [key], key).split(" ")[0]}</span>
              </div>
            ),
            sorter: ouScoreTableTransformer(key),
            sortDirections: ["ascend", "descend"],
            dataIndex: key,
            key,
            width: key === engineerCategoryType.LEARDERSHIP ? "15%" : "13%",
            render: getScoreColumnRender
          } as ColumnProps<any>)
      )
  ];
};
