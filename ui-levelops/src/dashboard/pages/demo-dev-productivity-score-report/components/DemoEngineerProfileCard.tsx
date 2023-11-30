import React, { useCallback, useEffect, useState } from "react";
import { AntSelect, AntText, AntTooltip } from "shared-resources/components";
import { engineerCategoryIconsMapping } from "../../scorecard/constants";
import "../../scorecard/components/engineerProfileCard.styles.scss";
import { Empty, Icon, Select, Spin, Tooltip } from "antd";
import { useMemo } from "react";
import { toTitleCase } from "utils/stringUtils";
import { engineerSectionType } from "../../../dashboard-types/engineerScoreCard.types";
import { legendColorByScore } from "../../../helpers/devProductivityRating.helper";

interface DemoEngineerProfileCardProps {
  data: any;
}

const DemoEngineerProfileCard: React.FC<DemoEngineerProfileCardProps> = ({ data }) => {
  const [showName, setShowName] = useState<boolean>(false);
  const [selectedTrellisProfile, setSelectedTrellisProfile] = useState<any>();

  useEffect(() => {
    if (data?.dev_productivity_profiles?.length) {
      const firstProfile = data.dev_productivity_profiles[0];
      setSelectedTrellisProfile(firstProfile);
    }
  }, [data?.dev_productivity_profiles]);

  const renderEmptyProfileCard = useMemo(() => {
    return (
      <div className="engineer-profile-card" style={{ justifyContent: "center" }}>
        <Empty className="score-empty" />
      </div>
    );
  }, []);

  const toggleState = () => {
    setShowName(value => !value);
  };

  const sectionScoreGrid = (section: engineerSectionType) => {
    return (
      <div className="section-score" key={section?.name}>
        <img src={(engineerCategoryIconsMapping() as any)[section?.name || ""]} className="section-score-image" />
        <AntText ellipsis className="section-score-name">
          {section?.name}
        </AntText>
        <AntText className="section-score-chip">
          <span
            className="section-score-chip-mark"
            style={{ backgroundColor: `${legendColorByScore(section?.score)}` }}
          />
          {section?.score ?? "NA"}
        </AntText>
      </div>
    );
  };

  const userName = useMemo(
    () =>
      showName ? (
        <span className="engineer-profile-card-name">
          <AntText ellipsis className="engineer-profile-card-full_name">
            {toTitleCase(data.full_name)}
          </AntText>
          <Icon className="invisible-eye-icon" type="eye-invisible" onClick={toggleState} />
        </span>
      ) : (
        <AntText className="engineer-profile-card-hide" onClick={toggleState}>
          Show Name <Icon type="eye" />
        </AntText>
      ),
    [showName]
  );

  const onTrellisProfileChange = (selectedProfileId: any) => {
    const selectedProfile = data.dev_productivity_profiles.find((profile: any) => profile.id == selectedProfileId);
    setSelectedTrellisProfile(selectedProfile);
  };
  console.log("selectedProfile", selectedTrellisProfile);
  const trellisProfiles = useMemo(() => {
    return (
      data?.dev_productivity_profiles?.length && (
        <span className="engineer-profile-card-trellis">
          <AntText className="engineer-profile-card-trellis-key">Trellis Profile:</AntText>
          <AntSelect onChange={onTrellisProfileChange} value={selectedTrellisProfile?.id} style={{ width: "100%" }}>
            {data.dev_productivity_profiles.map((profile: any) => (
              <Select.Option value={profile.id}>{profile.name}</Select.Option>
            ))}
          </AntSelect>
        </span>
      )
    );
  }, [data?.dev_productivity_profiles, selectedTrellisProfile, onTrellisProfileChange]);

  const orgTooltipValue = useMemo(
    () =>
      data?.associated_ous?.length && (
        <span className="org-tooltip-container">
          <AntText className="org-tooltip-container-key">Collection:</AntText>
          {data.associated_ous.map((org: { [key: string]: string }, index: number) => (
            <AntText className="org-tooltip-container-name">{org?.ou_name}</AntText>
          ))}
        </span>
      ),
    [data]
  );

  const organizations = useMemo(
    () =>
      data?.associated_ous?.length && (
        <span className="engineer-profile-card-org">
          <div className="engineer-profile-card-org-unit">
            <AntText className="engineer-profile-card-org-key">Collection</AntText>
            <AntTooltip
              title={"The list of collections associated with the selected trellis profile for the user"}
              trigger={["hover", "click"]}
              placement="rightTop">
              <Icon type="info-circle" />
            </AntTooltip>
          </div>
          <Tooltip
            overlayClassName="org-name-tooltip"
            placement="right"
            title={data.associated_ous.length > 5 && orgTooltipValue}>
            {data.associated_ous.slice(0, 5).map((org: { [key: string]: string }, index: number) => (
              <AntText className="engineer-profile-card-org-name">
                {org?.ou_name}
                {index !== 4 && index !== data.associated_ous.length - 1 && `, `}
              </AntText>
            ))}
            {data.associated_ous.length > 5 && (
              <AntText className="engineer-profile-card-org-name">
                &nbsp; And {data.associated_ous.length - 5} more
              </AntText>
            )}
          </Tooltip>
        </span>
      ),
    [data, orgTooltipValue]
  );

  const scoreSummary = useMemo(
    () => (
      <>
        <div
          className="engineer-profile-card-score-card"
          style={{ backgroundColor: `${legendColorByScore(data?.score)}` }}>
          <AntText className="engineer-profile-card-score-card-value">{data?.score ?? "NA"}</AntText>
          TRELLIS SCORE
        </div>
        <div className="engineer-profile-card-area-score">
          <AntText ellipsis className="engineer-profile-card-area-score-title">
            AREA SCORES
          </AntText>
          {(data?.section_responses || []).map((section: any) => {
            return sectionScoreGrid(section);
          })}
        </div>
      </>
    ),
    [data]
  );

  if (!data) {
    return renderEmptyProfileCard;
  }

  return (
    <div className="engineer-profile-card">
      <AntText className="engineer-profile-card-title" ellipsis>
        Summary
      </AntText>
      {userName}
      {trellisProfiles}
      {organizations}
      {scoreSummary}
    </div>
  );
};

export default DemoEngineerProfileCard;
