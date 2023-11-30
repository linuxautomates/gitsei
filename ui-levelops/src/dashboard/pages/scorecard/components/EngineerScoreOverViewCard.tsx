import React, { useCallback, useMemo, useState } from "react";
import "./engineerScoreOverViewCard.scss";
import { Empty, Spin, Popover } from "antd";
import { AntIcon, AntText } from "../../../../shared-resources/components";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import {
  devProductivityReportError,
  devProductivityReportLoading,
  devProRestEngineerSelect
} from "reduxConfigs/selectors/devProductivity.selector";
import { DEV_PROD_ID, engineerCategoryIconsMapping, REQUIRE_TRELLIS_TEXT } from "../constants";
import { RestDevProdEngineer } from "../../../../classes/RestDevProdEngineer";
import { toTitleCase } from "utils/stringUtils";
import { engineerFeatureResponsesType } from "../../../dashboard-types/engineerScoreCard.types";
import cx from "classnames";
import { getNumberAbbreviation } from "../../../../shared-resources/charts/helper";
import { convertToFixedDays } from "../../../../utils/timeUtils";
import { capitalize, get } from "lodash";
import {
  findRatingByScoreType,
  legendColorByRating,
  legendColorByScore
} from "../../../helpers/devProductivityRating.helper";
import { ratingToLegendColorMapping } from "../../../constants/devProductivity.constant";
import ScoreCardInfoIconPopoverDetails from "./ScoreCardInfoIconPopoverDetails";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";

interface EngineerScoreOverViewCardProps {
  selectedFeature: engineerFeatureResponsesType | null;
  setSelectedFeature: (section: engineerFeatureResponsesType) => void;
  trellis_profile?: Record<string, any>;
}

const EngineerScoreOverViewCard: React.FC<EngineerScoreOverViewCardProps> = ({
  setSelectedFeature,
  selectedFeature,
  trellis_profile
}) => {
  const devReportLoadingState = useParamSelector(devProductivityReportLoading, {
    id: DEV_PROD_ID
  });

  const devReportErrorState = useParamSelector(devProductivityReportError, {
    id: DEV_PROD_ID
  });

  const engineer: RestDevProdEngineer = useParamSelector(devProRestEngineerSelect, {
    id: DEV_PROD_ID
  });

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
    return !trellis_profile ? (
      <div className="score-overview-container" style={{ justifyContent: "center" }}>
        <AntText>{REQUIRE_TRELLIS_TEXT}</AntText>
      </div>
    ) : (
      <div className="engineer-profile-card" style={{ justifyContent: "center" }}>
        {devReportLoadingState ? (
          <div className="w-100p h-100p flex justify-center align-center">
            <Spin />
          </div>
        ) : (
          <Empty className="score-empty" />
        )}
      </div>
    );
  }, [devReportLoadingState]);

  const onFeatureClick = useCallback((feature: engineerFeatureResponsesType) => {
    setSelectedFeature(feature);
  }, []);

  if (devReportLoadingState || !Object.keys(engineer).length || devReportErrorState) {
    return renderEmptyProfileCard;
  }

  return (
    <div className="score-overview-container">
      <AntText className="score-overview-container-title">SCORE FACTORS</AntText>
      <div className="section-over-view-container">
        {(engineer.section_responses || []).map(section => (
          <div className="section-over-view" key={section?.name}>
            <div className="section-over-view-details">
              <img
                src={(engineerCategoryIconsMapping() as any)[section?.name || ""]}
                className="section-over-view-details-image"
              />
              <AntText ellipsis className="section-over-view-details-name">
                {get(TRELLIS_SECTION_MAPPING, [section?.name], section?.name)}
                <Popover
                  title=""
                  content={<ScoreCardInfoIconPopoverDetails section={section} profile={trellis_profile} />}>
                  <AntIcon type="info-circle" className="ml-5" />
                </Popover>
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
              {(section?.feature_responses || []).map(feature => (
                <div
                  className={cx("feature-score", { highlighted: selectedFeature?.name === feature?.name })}
                  onClick={() => onFeatureClick(feature)}
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

export default EngineerScoreOverViewCard;
