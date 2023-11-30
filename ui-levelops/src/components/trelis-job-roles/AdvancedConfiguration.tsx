import React, { useEffect, useMemo, useState } from "react";
import { buildExcludeObject } from "configurations/pages/TrellisProfile/ProfileDetailsPage/helper";
import { useDispatch, useSelector } from "react-redux";
import cx from "classnames";
import {
  ADVANCED_CONFIG_INFO,
  ADVANCED_CONFIG_LABEL,
  EXCLUDE_COMMITS,
  EXCLUDE_SETTINGS,
  TEXT_EXCLUDE_COMMIT_HEADER,
  TEXT_EXCLUDE_COMMIT_NOTE,
  TEXT_EXCLUDE_PR_HEADER,
  TEXT_EXCLUDE_PR_NOTE,
  TEXT_LIST_DEV_STAGES,
  TEXT_LIST_DEV_STAGES_NOTE
} from "./constant";
import { azureCategoriesFiltersValues, categoriesFiltersTrellisProfileValues } from "reduxConfigs/actions/restapi";
import { hasJiraIntegrations } from "reduxConfigs/selectors/integrationSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { categoriesFiltersValueSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { AntCol, AntRow, AntSelect, AntText, AntTooltip } from "shared-resources/components";
import "./AdvancedConfiguration.scss";
import ExcludeSetting from "./ExcludeSetting";
import { useTicketCategorizationFilters } from "custom-hooks";
import { Select } from "antd";
import { cloneDeep } from "lodash";
import { AntCheckboxComponent } from "shared-resources/components/ant-checkbox/ant-checkbox.component";
import { FEATURES_WITH_EFFORT_PROFILE } from "classes/RestTrellisProfile";

interface AdvancedConfigurationProps {
  trellisProfile: any;
  handleChanges: (param1: any, param2: any) => void;
  ticketCategorizationData: any;
  apiLoading: boolean;
  handleCategories: (key: string, value: Array<string>) => void;
}
const { Option } = Select;

export const AdvancedConfiguration: React.FC<AdvancedConfigurationProps> = ({
  trellisProfile,
  handleChanges,
  ticketCategorizationData,
  apiLoading,
  handleCategories
}) => {
  const [statusList, setStatusList] = useState<Array<string>>([]);
  const hasJiraIntegration = useParamSelector(hasJiraIntegrations);

  const profile = useMemo(() => {
    return cloneDeep(trellisProfile);
  }, [trellisProfile]);

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

    handleChanges("settings", {
      ...settings,
      exclude: newExcludeObject
    });
  };

  const handleDevelopmentStages = (value: string) => {
    handleChanges("settings", {
      ...profile.settings,
      development_stages: value
    });
  };

  const dispatch = useDispatch();
  useEffect(() => {
    if (!statusList.length) {
      if (hasJiraIntegration) {
        dispatch(categoriesFiltersTrellisProfileValues());
      } else {
        dispatch(azureCategoriesFiltersValues());
      }
    }
  }, [hasJiraIntegration, statusList.length]);

  const categoriesFilterValuesState = useSelector(categoriesFiltersValueSelector);
  useEffect(() => {
    const { loading, error } = categoriesFilterValuesState;
    if (!loading && !error) {
      const statusKey = hasJiraIntegration ? "status" : "workitem_status";
      const { payload } = categoriesFilterValuesState;
      const statusObject = payload.data?.find((item: any) => item?.hasOwnProperty(statusKey));
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

  const profileOptions = useMemo(() => {
    return ticketCategorizationData?.map((data: any) => ({
      value: data?.id,
      label: data?.name,
      categories: Object.keys(data?.config?.categories).length
    }));
  }, [ticketCategorizationData]);

  const categoryOptions = useMemo(() => {
    if (!profile.effort_investment_profile_id) {
      return [];
    }
    const config = ticketCategorizationData?.find(
      (ticket: any) => ticket?.id === profile.effort_investment_profile_id
    )?.config;
    return !config
      ? []
      : Object.values(config?.categories || {}).map((category: any) => ({
          value: category?.id,
          label: category?.name
        }));
  }, [ticketCategorizationData, profile.effort_investment_profile_id]);
  return (
    <div className={"advanced-config"}>
      <div className="title">
        <label>{ADVANCED_CONFIG_LABEL}</label>
        <div className="info">{ADVANCED_CONFIG_INFO}</div>
      </div>

      <div className="config-row">
        <div className="dev-score-profile-container-section-container-header">
          <AntText className="section-header">INVESTMENT PROFILE</AntText>
        </div>
        <AntSelect
          className="selector"
          value={profile.effort_investment_profile_id}
          key="InvestmentProfile"
          allowClear
          optionLabelProp="label"
          placeholder={"Select a Profile"}
          onChange={(value: any) => handleChanges("effort_investment_profile_id", value)}
          loading={apiLoading}>
          {(profileOptions || []).map((option: { value: string; label: string; categories: number }) => (
            <Select.Option
              value={option.value}
              label={option.label}
              className="effort-investment-profile-selector-option">
              <span
                className={cx("circular-span", {
                  "border-blue": option.value === profile?.effort_investment_profile_id
                })}>
                {option.value === profile?.effort_investment_profile_id && <span className="blue-circle"></span>}
              </span>
              {option.label} - <span className="dev-profile-categories-count">{option.categories} Categories</span>
            </Select.Option>
          ))}
        </AntSelect>

        <div className="flex categories-container">
          <div className="categories">
            <label>High Impact stories worked on per month</label>
            <div className="select">
              <AntTooltip
                placement="topLeft"
                title={!profile?.effort_investment_profile_id ? "Please select effort investment profile" : ""}>
                <AntSelect
                  className="effort-investment-category-selector"
                  mode="multiple"
                  value={profile?.feature_ticket_categories_map?.[FEATURES_WITH_EFFORT_PROFILE[1]]}
                  optionLabelProp="label"
                  disabled={!profile?.effort_investment_profile_id}
                  placeholder={"Select Categories"}
                  onChange={(value: any) => {
                    handleCategories(FEATURES_WITH_EFFORT_PROFILE[1], value);
                  }}>
                  {(categoryOptions || []).map((option: { value: string; label: string }) => (
                    <Option value={option.value} label={option.label}>
                      <AntCheckboxComponent
                        className="effort-investment-category-selector-check"
                        onClick={(e: any) => e.preventDefault()}
                        checked={profile.feature_ticket_categories_map?.[FEATURES_WITH_EFFORT_PROFILE[1]]?.includes(
                          option.value
                        )}
                      />
                      {option.label}
                    </Option>
                  ))}
                </AntSelect>
              </AntTooltip>
            </div>
          </div>
          <div className="categories">
            <label>High Impact bugs worked on per month</label>
            <div className="select">
              <AntTooltip
                placement="topLeft"
                title={!profile?.effort_investment_profile_id ? "Please select effort investment profile" : ""}>
                <AntSelect
                  className="effort-investment-category-selector"
                  mode="multiple"
                  value={profile?.feature_ticket_categories_map?.[FEATURES_WITH_EFFORT_PROFILE[0]]}
                  optionLabelProp="label"
                  disabled={!profile?.feature_ticket_categories_map}
                  placeholder={"Select Categories"}
                  onChange={(value: any) => {
                    handleCategories(FEATURES_WITH_EFFORT_PROFILE[0], value);
                  }}>
                  {(categoryOptions || []).map((option: { value: string; label: string }) => (
                    <Option value={option.value} label={option.label}>
                      <AntCheckboxComponent
                        className="effort-investment-category-selector-check"
                        onClick={(e: any) => e.preventDefault()}
                        checked={profile.feature_ticket_categories_map?.[FEATURES_WITH_EFFORT_PROFILE[0]]?.includes(option.value)}
                      />
                      {option.label}
                    </Option>
                  ))}
                </AntSelect>
              </AntTooltip>
            </div>
          </div>
        </div>
      </div>
      <div className="config-row">
        <AntText>{TEXT_EXCLUDE_PR_HEADER}</AntText>
        <div className="note">
          <AntText>{TEXT_EXCLUDE_PR_NOTE}</AntText>
        </div>
        <AntRow gutter={[15, 0]} className="flex m-t-05">
          <AntCol className="text-align-left header-col first" span={8}>
            Attribute
          </AntCol>
          <AntCol className="text-align-left header-col" span={5}>
            Operator
          </AntCol>
          <AntCol className="text-align-left header-col" span={5}>
            Matches Value
          </AntCol>
        </AntRow>
        {EXCLUDE_SETTINGS.map((row: any, index: number) => {
          return (
            <>
              <ExcludeSetting
                key={`${row.BEKey}-${index}`}
                handleExcludeChange={handleExcludeChange}
                profile={profile}
                settings={row}
              />
            </>
          );
        })}
      </div>
      <div className="config-row">
        <AntText>{TEXT_EXCLUDE_COMMIT_HEADER}</AntText>
        <div className="note">
          <AntText>{TEXT_EXCLUDE_COMMIT_NOTE}</AntText>
        </div>
        <AntRow gutter={[15, 0]} className="flex m-t-05">
          <AntCol className="text-align-left header-col first" span={8}>
            Attribute
          </AntCol>
          <AntCol className="text-align-left header-col" span={5}>
            Operator
          </AntCol>
          <AntCol className="text-align-left header-col" span={5}>
            Matches Value
          </AntCol>
        </AntRow>
        {EXCLUDE_COMMITS.map((row: any, index: number) => {
          return (
            <ExcludeSetting
              key={`${row.BEKey}-${index}`}
              handleExcludeChange={handleExcludeChange}
              profile={profile}
              settings={row}
              isNumericValue={row.isNumeric}
            />
          );
        })}
      </div>
      <div className={"config-row"}>
        <AntText>{TEXT_LIST_DEV_STAGES}</AntText>
        <div className="note">
          <AntText className="note">{TEXT_LIST_DEV_STAGES_NOTE}</AntText>
        </div>
        <AntSelect
          className="selector"
          mode="multiple"
          value={profile?.settings?.development_stages}
          onChange={handleDevelopmentStages}
          options={statusList}
        />
      </div>
    </div>
  );
};
