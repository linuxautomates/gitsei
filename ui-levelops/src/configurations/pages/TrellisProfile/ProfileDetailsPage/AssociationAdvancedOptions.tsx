import { RestTrellisScoreProfile } from "classes/RestTrellisProfile";
import React, { useEffect, useState } from "react";
import { useDispatch, useSelector } from "react-redux";
import { azureCategoriesFiltersValues, categoriesFiltersValues } from "reduxConfigs/actions/restapi";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { hasJiraIntegrations } from "reduxConfigs/selectors/integrationSelectors";
import { categoriesFiltersValueSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { AntButton, AntSelect, AntText } from "shared-resources/components";
import {
  EXCLUDE_COMMITS,
  EXCLUDE_SETTINGS,
  TEXT_EXCLUDE_COMMIT_HEADER,
  TEXT_EXCLUDE_COMMIT_NOTE,
  TEXT_EXCLUDE_PR_HEADER,
  TEXT_EXCLUDE_PR_NOTE,
  TEXT_LIST_DEV_STAGES,
  TEXT_LIST_DEV_STAGES_NOTE
} from "../constant";
import ExcludeSettings from "./ExcludeSettings";
import { buildExcludeObject } from "./helper";

interface AssociationAdvancedOptionsProps {
  profile: RestTrellisScoreProfile;
  handleChanges: (section_name: string, value: any, type: string) => void;
}
const AssociationAdvancedOptions: React.FC<AssociationAdvancedOptionsProps> = ({ profile, handleChanges }) => {
  const [collapse, setCollapse] = useState<boolean>(true);
  const [statusList, setStatusList] = useState<Array<string>>([]);

  const hasJiraIntegrtion = useParamSelector(hasJiraIntegrations);

  const handleExcludeChange = (event: any, key: string, type: string, isNumeric: boolean = false) => {
    const { settings } = profile;
    const updatedObj = buildExcludeObject(profile, event, key, type, isNumeric);
    const newExcludeObject = isNumeric
      ? {
          ...(settings?.exclude || {}),
          [key]: updatedObj
        }
      : {
          partial_match: {
            ...(settings?.exclude?.partial_match || {}),
            [key]: updatedObj
          }
        };

    handleChanges(
      "",
      {
        ...settings,
        exclude: newExcludeObject
      },
      "settings"
    );
  };

  const handleDevelopmentStages = (value: string) => {
    handleChanges(
      "",
      {
        ...profile.settings,
        development_stages: value
      },
      "settings"
    );
  };

  const dispatch = useDispatch();
  useEffect(() => {
    if (!statusList.length) {
      if (hasJiraIntegrtion) {
        dispatch(categoriesFiltersValues());
      } else {
        dispatch(azureCategoriesFiltersValues());
      }
    }
  }, [hasJiraIntegrtion, statusList]);

  const categoriesFilterValuesState = useSelector(categoriesFiltersValueSelector);
  useEffect(() => {
    const { loading, error } = categoriesFilterValuesState;
    if (!loading && !error) {
      const statusKey = hasJiraIntegrtion ? "status" : "workitem_status";
      const { payload } = categoriesFilterValuesState;
      const statusObject = payload.data?.find((item: any) => item.hasOwnProperty(statusKey));
      const statusList =
        statusObject && statusObject[statusKey]
          ? statusObject[statusKey].reduce((acc: Array<string>, item: any) => {
              acc.push(item.key);
              return acc;
            }, [])
          : [];
      setStatusList(statusList);
    }
  }, [categoriesFilterValuesState]);

  return (
    <div className="dev-score-profile-container-section-container">
      <div className="dev-score-profile-container-section-container-header">
        <AntButton
          type="link"
          icon={collapse ? "right" : "down"}
          className="advanced-option-collapse"
          onClick={() => setCollapse(!collapse)}>
          <AntText className="section-header">ADVANCED OPTIONS</AntText>
        </AntButton>
      </div>
      <div className={collapse ? "d-none" : "d-block"}>
        <div className="dev-score-profile-container-section-container-body">
          <AntText>{TEXT_EXCLUDE_PR_HEADER}</AntText>
          <AntText className="note">{TEXT_EXCLUDE_PR_NOTE}</AntText>
          {EXCLUDE_SETTINGS.map((row: any, index: number) => {
            return (
              <ExcludeSettings
                key={`${row.BEKey}-${index}`}
                handleExcludeChange={handleExcludeChange}
                profile={profile}
                settings={row}
              />
            );
          })}
        </div>
        <div className="dev-score-profile-container-section-container-body">
          <AntText>{TEXT_EXCLUDE_COMMIT_HEADER}</AntText>
          <AntText className="note">{TEXT_EXCLUDE_COMMIT_NOTE}</AntText>
          {EXCLUDE_COMMITS.map((row: any, index: number) => {
            return (
              <ExcludeSettings
                key={`${row.BEKey}-${index}`}
                handleExcludeChange={handleExcludeChange}
                profile={profile}
                settings={row}
                isNumericValue={row.isNumeric}
              />
            );
          })}
        </div>
        <div className="dev-score-profile-container-section-container-body">
          <AntText>{TEXT_LIST_DEV_STAGES}</AntText>
          <AntText className="note">{TEXT_LIST_DEV_STAGES_NOTE}</AntText>
          <AntSelect
            className="selector"
            mode="multiple"
            value={profile?.settings?.development_stages}
            onChange={handleDevelopmentStages}
            options={statusList}
          />
        </div>
      </div>
    </div>
  );
};

export default AssociationAdvancedOptions;
