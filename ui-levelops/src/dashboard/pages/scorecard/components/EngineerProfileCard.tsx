import React, { useCallback, useEffect, useState } from "react";
import { RestDevProdEngineer } from "classes/RestDevProdEngineer";
import {
  devProdEngineerSnapshotSelect,
  devProductivityEngineerSnapshotLoading,
  devProductivityReportError,
  devProductivityReportLoading,
  devProRestEngineerSelect
} from "reduxConfigs/selectors/devProductivity.selector";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { AntButton, AntPopover, AntSelect, AntText, AntTooltip } from "shared-resources/components";
import { DEV_PROD_ID, DEV_PROD_USER_SNAPSHOT_ID, engineerCategoryIconsMapping } from "../constants";
import "./engineerProfileCard.styles.scss";
import { Empty, Icon, Select, Spin, Tooltip } from "antd";
import { useMemo } from "react";
import { toTitleCase } from "utils/stringUtils";
import { engineerSectionType } from "../../../dashboard-types/engineerScoreCard.types";
import { legendColorByScore } from "../../../helpers/devProductivityRating.helper";
import { TRELLIS_SECTION_MAPPING } from "configurations/pages/TrellisProfile/constant";
import { get } from "lodash";

interface EngineerProfileCardProps {
  setSelectedTrellisProfile: (selectedProfile: any) => void;
  newTrellisProfile: boolean;
}

const EngineerProfileCard: React.FC<EngineerProfileCardProps> = props => {
  const devReportSnapshotLoadingState = useParamSelector(devProductivityEngineerSnapshotLoading, {
    id: DEV_PROD_USER_SNAPSHOT_ID
  });
  const devReportErrorState = useParamSelector(devProductivityReportError, {
    id: DEV_PROD_ID
  });

  const engineer: RestDevProdEngineer = useParamSelector(devProRestEngineerSelect, {
    id: DEV_PROD_ID
  });

  const engineerSnapshot = useParamSelector(devProdEngineerSnapshotSelect, {
    id: DEV_PROD_USER_SNAPSHOT_ID
  });
  const [showName, setShowName] = useState<boolean>(false);
  const [selectedTrellisProfile, setSelectedTrellisProfile] = useState<any>();

  useEffect(() => {
    if (engineerSnapshot.dev_productivity_profiles?.length) {
      const firstProfile = engineerSnapshot.dev_productivity_profiles[0];
      setSelectedTrellisProfile(firstProfile);
      props.setSelectedTrellisProfile(firstProfile);
    }
  }, [engineerSnapshot.dev_productivity_profiles]);

  const renderEmptyProfileCard = useMemo(() => {
    return (
      <div className="engineer-profile-card" style={{ justifyContent: "center" }}>
        {devReportSnapshotLoadingState ? (
          <div className="w-100p h-100p flex justify-center align-center">
            <Spin />
          </div>
        ) : (
          <Empty className="score-empty" />
        )}
      </div>
    );
  }, [devReportSnapshotLoadingState]);

  const toggleState = () => {
    setShowName(value => !value);
  };

  const sectionScoreGrid = (section: engineerSectionType) => {
    return (
      <div className="section-score" key={section?.name}>
        <img src={(engineerCategoryIconsMapping() as any)[section?.name || ""]} className="section-score-image" />
        <AntText ellipsis className="section-score-name">
          {get(TRELLIS_SECTION_MAPPING, [section?.name], section?.name)}
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
            {toTitleCase(engineerSnapshot.full_name)}
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
    const selectedProfile = engineerSnapshot.dev_productivity_profiles.find(
      (profile: any) => profile.id == selectedProfileId
    );
    setSelectedTrellisProfile(selectedProfile);
    props.setSelectedTrellisProfile(selectedProfile);
  };

  const trellisProfiles = useMemo(() => {
    return (
      !!engineerSnapshot.dev_productivity_profiles?.length && (
        <span className="engineer-profile-card-trellis">
          <AntText className="engineer-profile-card-trellis-key">Trellis Profile:</AntText>
          <AntSelect onChange={onTrellisProfileChange} value={selectedTrellisProfile?.id} style={{ width: "100%" }}>
            {engineerSnapshot.dev_productivity_profiles.map((profile: any) => (
              <Select.Option value={profile.id}>{profile.name}</Select.Option>
            ))}
          </AntSelect>
        </span>
      )
    );
  }, [engineerSnapshot.dev_productivity_profiles, selectedTrellisProfile, onTrellisProfileChange]);

  const orgTooltipValue = useMemo(
    () =>
      !!selectedTrellisProfile?.associated_ous?.length && (
        <span className="org-tooltip-container">
          <AntText className="org-tooltip-container-key">Collection:</AntText>
          {selectedTrellisProfile.associated_ous.map((org: { [key: string]: string }, index: number) => (
            <AntText className="org-tooltip-container-name">{org?.ou_name}</AntText>
          ))}
        </span>
      ),
    [selectedTrellisProfile]
  );

  const organizations = useMemo(
    () =>
      !!selectedTrellisProfile?.associated_ous?.length && (
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
            title={selectedTrellisProfile.associated_ous.length > 5 && orgTooltipValue}>
            {selectedTrellisProfile.associated_ous.slice(0, 5).map((org: { [key: string]: string }, index: number) => (
              <AntText className="engineer-profile-card-org-name">
                {org?.ou_name}
                {index !== 4 && index !== selectedTrellisProfile.associated_ous.length - 1 && `, `}
              </AntText>
            ))}
            {selectedTrellisProfile.associated_ous.length > 5 && (
              <AntText className="engineer-profile-card-org-name">
                &nbsp; And {selectedTrellisProfile.associated_ous.length - 5} more
              </AntText>
            )}
          </Tooltip>
        </span>
      ),
    [selectedTrellisProfile, orgTooltipValue]
  );

  const scoreSummary = useMemo(
    () =>
      !devReportErrorState &&
      !!Object.keys(engineer).length && (
        <>
          <div
            className="engineer-profile-card-score-card"
            style={{ backgroundColor: `${legendColorByScore(engineer?.score)}` }}>
            <AntText className="engineer-profile-card-score-card-value">{engineer?.score ?? "NA"}</AntText>
            TRELLIS SCORE
          </div>
          <div className="engineer-profile-card-area-score">
            <AntText ellipsis className="engineer-profile-card-area-score-title">
              AREA SCORES
            </AntText>
            {(engineer.section_responses || []).map(section => {
              return sectionScoreGrid(section);
            })}
          </div>
        </>
      ),
    [devReportErrorState, engineer]
  );

  if (devReportSnapshotLoadingState || engineerSnapshot.error) {
    return renderEmptyProfileCard;
  }

  const newTrellisView = useMemo(() => {
    return (
      <>
        <span className="new-name">{userName}</span>
        <span className="role">
          <span className="role-label">Role:</span> {engineerSnapshot?.custom_fields?.contributor_role}
        </span>
        {scoreSummary}
      </>
    );
  }, [props.newTrellisProfile, userName, scoreSummary]);

  return (
    <div className="engineer-profile-card">
      {props.newTrellisProfile ? (
        newTrellisView
      ) : (
        <>
          <AntText className="engineer-profile-card-title" ellipsis>
            Summary
          </AntText>
          {userName}
          {trellisProfiles}
          {organizations}
          {scoreSummary}
        </>
      )}
    </div>
  );
};

export default EngineerProfileCard;
