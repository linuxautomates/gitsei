import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Button, Dropdown, Icon, Menu, Select, Switch, Tabs } from "antd";
import { cloneDeep } from "lodash";
import { AntInput, AntTable, SvgIcon } from "shared-resources/components";
import { ConfigureFactorModal } from "./modals/ConfigureFactorModal";
import { MetricEditModal } from "./modals/MetricEditModal";
import { AdvancedConfiguration } from "./AdvancedConfiguration";
import "./TrellisJobRoles.scss";
import { trellisTableColumns } from "./helper";
import { metricKeyValueUpdate, metricObjectUpdate } from "./TrelisJobRoleHelper";
import { useTicketCategorizationFilters } from "custom-hooks";
import { useLocation } from "react-router-dom";
import { useDispatch } from "react-redux";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { TrellisDisabledPage } from "./TrellisDisabledPage";
import { TrellisGroupModal } from "./modals/TrellisGroupModal";

const { TabPane } = Tabs;

export interface TrellisJobRolesProps {
  trellisProfile: any;
  setTrellisProfile: (param: any) => void;
  trellisProfileIsEnabled: boolean;
  setTrellisProfileIsEnabled: () => void;
  orgUnitId?: any;
  showAddRole?: boolean;
}

const { Option } = Select;

const TrellisJobRoles: React.FC<TrellisJobRolesProps> = ({
  trellisProfile,
  setTrellisProfile,
  trellisProfileIsEnabled,
  setTrellisProfileIsEnabled,
  orgUnitId,
  showAddRole = false
}) => {
  const [showAllRoles, setShowAllRoles] = useState(false);
  const [showConfigFactorModal, setShowConfigFactorModal] = useState(false);
  const [showMetricEditModal, setShowMetricEditModal] = useState(false);
  const [currentMetricEditFeature, setCurrentMetricEditFeature] = useState(undefined);
  const [showAdvancedTab, setShowAdvancedTab] = useState(false);
  const [selectedSubProfileName, setSelectedSubProfileName] = useState<string | undefined>(undefined);
  const [metricSorting, setMetricSorting] = useState<string | undefined>("ONLY_ENABLED");
  const [searchValue, setSearchValue] = useState<string | undefined>(undefined);
  const [searchToggle, setSearchToggle] = useState<boolean>(false);
  const [showTrellisGroupModal, setShowTrellisGroupModal] = useState<boolean>(false);
  const [subProfileEdit, setSubProfileEdit] = useState<Record<string, any> | undefined>(undefined)
  const entOrgUnits = useHasEntitlements(Entitlement.SETTING_ORG_UNITS);
  const entOrgUnitsCountExceed = useHasEntitlements(Entitlement.SETTING_ORG_UNITS_COUNT_5, EntitlementCheckType.AND);

  const location = useLocation();
  const dispatch = useDispatch();
  const { apiData: ticketCategorizationData, apiLoading } = useTicketCategorizationFilters("dev_profile", []);

  useEffect(() => {
    setTrellisProfile(trellisProfile);
    setSelectedSubProfileName(trellisProfile?.sub_profiles?.[0]?.name);
  }, []);

  const currentSubProfileIndex = useMemo(() => {
    let foundIndex = undefined;
    if (trellisProfile) {
      trellisProfile?.sub_profiles?.find((profile: any, index: number) => {
        if (profile?.name === selectedSubProfileName) {
          foundIndex = index;
        }
      });
    }
    return foundIndex;
  }, [selectedSubProfileName]);

  const currentSubProfile = useMemo(() => {
    if (trellisProfile) {
      return trellisProfile?.sub_profiles?.find((profile: any) => {
        return profile?.name === selectedSubProfileName;
      });
    }
  }, [trellisProfile, selectedSubProfileName]);

  const dataSource = useMemo(() => {
    if (trellisProfile) {
      if (currentSubProfile) {
        return currentSubProfile?.sections
          ?.map((section: any) => {
            if (section) {
              const data = section?.features.map((feat: any) => {
                if (metricSorting === "ONLY_ENABLED" && feat?.enabled === false) {
                  return undefined;
                }
                if (searchValue) {
                  const val = searchValue?.toLocaleLowerCase();
                  if (!feat.name?.toLowerCase()?.includes(val) && !section?.name?.includes(val)) {
                    return undefined;
                  }
                }
                return {
                  metric: { enabled: feat?.enabled, name: feat.name, sectionEnabled: section?.enabled },
                  factor: { name: section?.name, weight: section?.weight },
                  range: feat,
                  edit: {
                    sectionEnabled: section?.enabled,
                    sectionName: section?.name,
                    ...feat
                  },
                  key: feat.name
                };
              });
              return data;
            }
          })
          ?.flatMap((data: any) => data)
          ?.filter((data: any) => data);
      }
    }
    return [];
  }, [currentSubProfile, selectedSubProfileName, trellisProfile, metricSorting, searchValue]);

  const enabledMetricsCount = useMemo(() => {
    if (dataSource?.length > 0) {
      return dataSource?.filter((data: any) => data?.metric?.enabled)?.length;
    }
    return "";
  }, [dataSource]);

  const featureMetricUpdate = useCallback(
    (metric: string | Record<string, any>, key: string, value: any, metricObjUpdate: boolean = false) => {
      const newTrellisProfile = cloneDeep(trellisProfile);
      if (newTrellisProfile?.sub_profiles?.[currentSubProfileIndex !== undefined ? currentSubProfileIndex : ""]) {
        const newSubProfile = metricObjUpdate
          ? metricObjectUpdate(currentSubProfile, metric)
          : metricKeyValueUpdate(currentSubProfile, metric, key, value);
        newTrellisProfile.sub_profiles[currentSubProfileIndex !== undefined ? currentSubProfileIndex : ""] =
          newSubProfile;
        setTrellisProfile(newTrellisProfile);
      }
    },
    [trellisProfile, currentSubProfileIndex, currentSubProfile, setTrellisProfile]
  );

  const subProfileFactorsUpdate = useCallback(
    (updatedProfile: any) => {
      const newTrellisProfile = cloneDeep(trellisProfile);
      if (newTrellisProfile?.sub_profiles?.[currentSubProfileIndex !== undefined ? currentSubProfileIndex : ""]) {
        newTrellisProfile.sub_profiles[currentSubProfileIndex !== undefined ? currentSubProfileIndex : ""] =
          updatedProfile;
        setTrellisProfile(newTrellisProfile);
      }
    },
    [trellisProfile, currentSubProfileIndex, currentSubProfile, setTrellisProfile]
  );

  const columns = useMemo(() => {
    return trellisTableColumns(
      setShowConfigFactorModal,
      setShowMetricEditModal,
      featureMetricUpdate,
      setCurrentMetricEditFeature
    );
  }, [trellisTableColumns, setShowConfigFactorModal, setShowMetricEditModal, trellisProfile, currentSubProfile]);

  const configFactor = useMemo(() => {
    if (showConfigFactorModal) {
      return (
        <ConfigureFactorModal
          key={currentSubProfile?.name}
          visible={showConfigFactorModal}
          onClose={() => setShowConfigFactorModal(!showConfigFactorModal)}
          currentSubProfile={currentSubProfile}
          subProfileFactorsUpdate={(profile: any) => {
            setShowConfigFactorModal(!showConfigFactorModal);
            subProfileFactorsUpdate(profile);
          }}
        />
      );
    }
  }, [currentSubProfile, showConfigFactorModal]);

  const editMetric = useMemo(() => {
    if (showMetricEditModal) {
      return (
        <MetricEditModal
          data={currentMetricEditFeature}
          visible={showMetricEditModal}
          onClose={() => setShowMetricEditModal(!showMetricEditModal)}
          featureMetricUpdate={featureMetricUpdate}
          ticketCategoryId={trellisProfile?.effort_investment_profile_id}
          ticketCategorizationData={ticketCategorizationData}
        />
      );
    }
  }, [currentMetricEditFeature, showMetricEditModal, ticketCategorizationData, featureMetricUpdate]);

  const handleAdvancedConfig = (key: string, value: any) => {
    const newTrellisProfile = cloneDeep(trellisProfile);
    newTrellisProfile[key] = value;
    setTrellisProfile(newTrellisProfile);
  };

  const handleCategories = (key: string, value: Array<string>) => {
     const newTrellisProfile = cloneDeep(trellisProfile);
     newTrellisProfile.feature_ticket_categories_map[key] = value;
     setTrellisProfile(newTrellisProfile);
  };

  const handleSubProfileToggle = useCallback(
    (value: any, index: any) => {
      const newTrellisProfile = cloneDeep(trellisProfile);
      newTrellisProfile.sub_profiles[index].enabled = value;
      setTrellisProfile(newTrellisProfile);
    },
    [currentSubProfile]
  );

  const handleTrellisGroup = useCallback(
    group => {
      const newTrellisProfile = cloneDeep(trellisProfile);
      const subProfileIndex = newTrellisProfile?.sub_profiles?.findIndex((grp: Record<string, any>) => grp?.name === subProfileEdit?.name);
      let defaultProfile = subProfileEdit
        ? cloneDeep(newTrellisProfile?.sub_profiles[subProfileIndex])
        : cloneDeep(newTrellisProfile?.sub_profiles.find((profile: any) => profile?.name === "Default"));
      
      defaultProfile = defaultProfile ? defaultProfile : cloneDeep(newTrellisProfile?.sub_profiles[0]);
      defaultProfile.name = group.name;
      defaultProfile.matching_criteria = group.matchingCriteria;
      defaultProfile.order = subProfileEdit
        ? subProfileEdit?.order
        : newTrellisProfile?.sub_profiles[newTrellisProfile?.sub_profiles?.length - 1]?.order + 1;

      if (subProfileEdit) {
        newTrellisProfile.sub_profiles[subProfileIndex] = defaultProfile;
      } else {
        newTrellisProfile.sub_profiles.push(defaultProfile);
      }
      setTrellisProfile(newTrellisProfile);
      setShowTrellisGroupModal(!showTrellisGroupModal);
      setSelectedSubProfileName(group.name);
      setSubProfileEdit(undefined)
      if (newTrellisProfile.sub_profiles?.length > 3) {
        setShowAllRoles(true);
      }
    },
    [trellisProfile, showTrellisGroupModal, subProfileEdit]
  );

  const trellisGroupModal = useMemo(() => {
    if (showTrellisGroupModal) {
			const profileNames = trellisProfile?.sub_profiles.map((profile: Record<string, string>) => profile.name);
      const criteria = (subProfileEdit?.matching_criteria || [])?.map((profile: any) => {
        return {
          ...profile,
          field: `custom_field_${profile?.field}`
        };
      });
      return (
        <TrellisGroupModal
          handleTrellisGroup={handleTrellisGroup}
          onClose={() => setShowTrellisGroupModal(!showTrellisGroupModal)}
          visible={showTrellisGroupModal}
          profile={{ name: subProfileEdit?.name, matchingCriteria: criteria }}
          profileNames={profileNames}
        />
      );
    }
  }, [showTrellisGroupModal, subProfileEdit, trellisProfile]);

  const isDefault = (name: string) => {
    return name === "Default";
  }

  const handleSubProfileEdit = useCallback(
    role => {
      setSubProfileEdit(role);
      setShowTrellisGroupModal(!showTrellisGroupModal);
    },
    [showTrellisGroupModal]
  );

  const handleSubProfileDelete = useCallback(
    role => {
      const newTrellisProfile = cloneDeep(trellisProfile);
      newTrellisProfile.sub_profiles = newTrellisProfile.sub_profiles.filter(
        (profile: any) => profile.order !== role.order
      );
      setTrellisProfile(newTrellisProfile);
    },
    [trellisProfile]
  );

  const menu = (role: any) => {
    return (
      <Menu>
        <Menu.Item key="edit" onClick={() => handleSubProfileEdit(role)}>
          Edit
        </Menu.Item>
        <Menu.Item key="delete" onClick={(e) => { e?.domEvent?.stopPropagation(); handleSubProfileDelete(role); }}>Delete</Menu.Item>
      </Menu>
    );
  };

  return (
    <>
      {trellisProfileIsEnabled && (
        <div className="trelis-job-role-wrapper">
          <div className="trelis-job-role">
            <div className="side-pane">
              <div className="info">
                <Icon type="idcard" />
                <span className="text">Configure Contributor Roles</span>
              </div>
              {showAddRole && (
                <Button
                  onClick={() => {
                    setSubProfileEdit(undefined);
                    setShowTrellisGroupModal(!showTrellisGroupModal);
                  }}
                  className="trellis-grp-btn"
                  type="dashed"
                  icon="plus">
                  Add Trellis Group
                </Button>
              )}
              <div className="roles">
                {(showAllRoles ? trellisProfile?.sub_profiles : trellisProfile?.sub_profiles?.slice(0, 4))?.map(
                  (role: any, index: number) => {
                    return (
                      <span
                        className={`role ${selectedSubProfileName === role?.name ? "selected" : ""} `}
                        onClick={e => {
                          e.preventDefault();
                          setSelectedSubProfileName(role?.name);
                          setShowAdvancedTab(false);
                        }}
                        key={role?.name}>
                        {!isDefault(role?.name) && (
                          <span className={`switch`}>
                            {" "}
                            <Switch
                              checked={role?.enabled}
                              onChange={() => handleSubProfileToggle(!role?.enabled, index)}
                            />{" "}
                          </span>
                        )}
                        <span className={`title ${isDefault(role?.name) ? "default" : ""}`}>{role?.name}</span>
                        {showAddRole && !isDefault(role?.name) && (
                          <span className="more-btn">
                            <Dropdown overlay={() => menu(role)}>
                              <Button icon="more" />
                            </Dropdown>
                          </span>
                        )}
                      </span>
                    );
                  }
                )}
                <span className="role show-more-btn" onClick={() => setShowAllRoles(!showAllRoles)}>
                  {showAllRoles ? "Show less" : `Show ${trellisProfile?.sub_profiles?.length - 4} more groups`}
                  <Icon type="double-right" className={`${showAllRoles ? "more" : "less"}`} />
                </span>
              </div>
              <div
                className={`info pointer ${showAdvancedTab ? "selected" : ""}`}
                onClick={() => {
                  setShowAdvancedTab(true);
                  setSelectedSubProfileName(undefined);
                }}>
                <span>
                  <SvgIcon icon="advancedConfig" />
                </span>
                <span className="text">Advanced Configuration</span>
              </div>
            </div>
            <div className="content">
              {!showAdvancedTab ? (
                <>
                  <div className="menu-wrapper">
                    <div className="profile-name">{selectedSubProfileName}</div>
                    <div className="filters-menu">
                      <Button
                        className="config-btn"
                        icon="edit"
                        onClick={() => setShowConfigFactorModal(!showConfigFactorModal)}>
                        Configure Factors
                      </Button>
                      <div className="btn-wrapper">
                        {searchToggle && (
                          <AntInput
                            placeholder="Search..."
                            onChange={(e: any) => setSearchValue(e?.target?.value)}
                            value={searchValue}
                          />
                        )}
                        <Button
                          className="user-search"
                          icon="search"
                          onClick={() => {
                            setSearchToggle(!searchToggle);
                            if (!searchToggle) {
                              setSearchValue(undefined);
                            }
                          }}
                        />
                        <Select className="select" value={metricSorting} onChange={value => setMetricSorting(value)}>
                          <Option value="ONLY_ENABLED">Show Metrics: Only Enabled</Option>
                          <Option value="ALL">Show Metrics: All</Option>
                        </Select>
                      </div>
                    </div>
                  </div>
                  <div className="trelis-table">
                    <div className="metrics-label">Metrics Enabled : {enabledMetricsCount}</div>
                    <AntTable columns={columns} pagination={false} dataSource={dataSource} />
                  </div>
                </>
              ) : (
                <AdvancedConfiguration
                  trellisProfile={trellisProfile}
                  ticketCategorizationData={ticketCategorizationData}
                  apiLoading={apiLoading}
                  handleChanges={handleAdvancedConfig}
                  handleCategories={handleCategories}
                />
              )}
            </div>
          </div>
          {configFactor}
          {editMetric}
        </div>
      )}
      {!trellisProfileIsEnabled && <TrellisDisabledPage setTrellisProfileIsEnabled={setTrellisProfileIsEnabled} />}
      {trellisGroupModal}
    </>
  );
};

export default TrellisJobRoles;
