import React, { useCallback, useMemo, useState } from "react";
import "../../scorecard/components/engineerScoreOverViewCard.scss";
import { Empty, Spin } from "antd";
import { AntText } from "../../../../shared-resources/components";
import { engineerCategoryIconsMapping } from "../../scorecard/constants";
import { toTitleCase } from "utils/stringUtils";
import { engineerFeatureResponsesType } from "../../../dashboard-types/engineerScoreCard.types";
import cx from "classnames";
import { getNumberAbbreviation } from "../../../../shared-resources/charts/helper";
import { convertToFixedDays } from "../../../../utils/timeUtils";
import { capitalize } from "lodash";
import {
  findRatingByScoreType,
  legendColorByRating,
  legendColorByScore
} from "../../../helpers/devProductivityRating.helper";
import { ratingToLegendColorMapping } from "../../../constants/devProductivity.constant";

interface DemoEngineerScoreOverViewCardProps {
  data: any;
}

const DemoEngineerScoreOverViewCard: React.FC<DemoEngineerScoreOverViewCardProps> = ({ data }) => {
  const getFeatureResult = useCallback((feature: engineerFeatureResponsesType) => {
    const unit = (feature?.feature_unit || "").toLowerCase();
    let result = feature?.mean ?? feature?.result ?? "N/A";
    if (result !== "N/A") {
      if (unit?.toLowerCase() === "days") {
        result = feature?.result ?? "N/A";
        return `${convertToFixedDays(result)} ${capitalize(unit)}`;
      }
      return `${getNumberAbbreviation(result, false, 2)} ${capitalize(unit)}`;
    }
    return `${result}`;
  }, []);

  const renderEmptyProfileCard = useMemo(() => {
    return <Empty className="score-empty" />;
  }, [data]);

  if (!data) {
    return renderEmptyProfileCard;
  }

  return (
    <div className="score-overview-container">
      <AntText className="score-overview-container-title">SCORE FACTORS</AntText>
      <div className="section-over-view-container">
        {(data?.section_responses || []).map((section: any) => (
          <div className="section-over-view" key={section?.name}>
            <div className="section-over-view-details">
              <img
                src={(engineerCategoryIconsMapping() as any)[section?.name || ""]}
                className="section-over-view-details-image"
              />
              <AntText ellipsis className="section-over-view-details-name">
                {section?.name}
              </AntText>
              <AntText className="section-over-view-details-chip">
                <span
                  className="section-over-view-details-chip-mark"
                  style={{ backgroundColor: `${legendColorByScore(section?.score)}` }}
                />
                {section?.score ?? "NA"}
              </AntText>
            </div>
            <div className="section-over-view-body">
              {(section?.feature_responses || []).map((feature: any) => (
                <div
                  className={cx("feature-score")}
                  // onClick={() => onFeatureClick(feature)}
                  key={feature?.name}>
                  <AntText
                    ellipsis
                    className="feature-score-value"
                    style={{
                      backgroundColor: `${legendColorByRating(findRatingByScoreType(feature?.rating))}`
                    }}>
                    {getFeatureResult(feature)}
                  </AntText>
                  <AntText ellipsis className="feature-score-name">
                    {feature?.name}
                  </AntText>
                </div>
              ))}
            </div>
          </div>
        ))}
      </div>
      <div className="score-overview-container-legends">
        <div className="score-overview-container-legends-wrapper">
          {Object.entries(ratingToLegendColorMapping).map(([legend, color]) => (
            <div className="score-legend-container" key={legend}>
              <span className="score-legend-mark" style={{ backgroundColor: `${color}` }}></span>
              {toTitleCase(legend)}
            </div>
          ))}
        </div>
      </div>
    </div>
  );
};

export default DemoEngineerScoreOverViewCard;
