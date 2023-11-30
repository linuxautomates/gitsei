import React, { useCallback, useEffect, useMemo, useState } from "react";
import { cloneDeep, forEach } from "lodash";
import { Icon, Spin, Tooltip } from "antd";
import { v1 as uuid } from "uuid";
import { useSelector } from "react-redux";
import {
  filterFieldConfig,
  filterFieldType,
  OrgUnitSectionKeys,
  OrgUnitSectionPayloadType,
  OrgUnitTypes,
  sectionSelectedFilterType,
  userGroupType
} from "configurations/configuration-types/OUTypes";
import { AntButton, AntText, CustomSelect } from "shared-resources/components";
import "./OrganizationIntegrationConfig.styles.scss";
import { OrgUnitType } from "configurations/constants";
import OrgUnitIntegrationFilterField from "./OrgUnitIntegrationFilterField";
import { orgUnitFiltersMapping } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import OrganizationUnitUserSection from "./OrganizationUnitUserSection";
import cx from "classnames";
import { useIntegrationFilterConfiguration } from "../Filters/useIntegrationFilterConfiguration";

interface integrationConfigureComponentProps {
  section: OrgUnitSectionPayloadType;
  handleRemoveSection: (id: string) => void;
  handleOUSectionChanges: (key: OrgUnitSectionKeys, value: any, id: string) => void;
  integrations: any[];
}

const OrganizationIntegrationConfigureComponent: React.FC<integrationConfigureComponentProps> = ({
  section,
  handleOUSectionChanges,
  handleRemoveSection,
  integrations
}) => {
  const [loading, setLoading] = useState<boolean>(false);
  const applicationAndId: string[] = (section?.type || "")?.split("@");
  const integrationIds = [applicationAndId?.[1]];
  const application: string = applicationAndId?.[0];
  const mapping = useSelector(orgUnitFiltersMapping);

  const [allFilterConfig, selectedIntegrationFilters] = useIntegrationFilterConfiguration(application, integrationIds, { removeCicdStageStepFilter: true });

  useEffect(() => {
    if (mapping && mapping[section?.type]) {
      setLoading(false);
    }
  }, [mapping, handleOUSectionChanges]);

  const handleAddFilter = () => {
    const dummyFilter: sectionSelectedFilterType = { key: "", value: "", param: "" };
    const nfilters = [...(section?.integration?.filters || [])];
    nfilters.push(dummyFilter);
    const nIntegration: filterFieldConfig = {
      ...(section?.integration || {}),
      filters: nfilters
    };
    handleOUSectionChanges("integration", nIntegration, section?.id);
  };

  const handleRemoveFilter = (index: number) => {
    let nFilters: sectionSelectedFilterType[] = [];
    forEach(section?.integration?.filters || [], (filter, idx) => {
      if (idx !== index) {
        nFilters.push(filter);
      }
    });
    const nIntegration: filterFieldConfig = {
      ...(section?.integration || {}),
      filters: nFilters
    };
    handleOUSectionChanges("integration", nIntegration, section?.id);
  };

  const handleActionsClick = (key: OrgUnitTypes, id?: string) => {
    switch (key) {
      case OrgUnitType.FILTER:
        handleAddFilter();
        break;
      case OrgUnitType.USER:
        const nUserGroup: userGroupType = {
          id: uuid(),
          dynamic_user_definition: [],
          users: [],
          csv_users: {}
        };
        const nUserGroups = [...(section?.user_groups || []), nUserGroup];
        handleOUSectionChanges("user_groups", nUserGroups, section?.id);
        break;
      case OrgUnitType.DELETE:
        handleRemoveSection(section?.id);
        break;
    }
  };

  const handleUpdateUserGroups = useCallback(
    (key: OrgUnitSectionKeys, value: any, id: string) => {
      const corrUserGroup = (section?.user_groups || []).find(group => group?.id === id);
      const index = (section?.user_groups || []).findIndex(group => group?.id === id);
      if (corrUserGroup) {
        const nUserGroup = {
          ...corrUserGroup,
          [key]: value
        };
        const nUserGroups = cloneDeep(section?.user_groups || []);
        nUserGroups[index] = nUserGroup;
        handleOUSectionChanges("user_groups", nUserGroups, section?.id);
      }
    },
    [section]
  );

  const handleRemoveUserGroups = useCallback(
    (id: string) => {
      const nUserGroups = (section?.user_groups || []).filter(groups => groups.id !== id) || [];
      handleOUSectionChanges("user_groups", nUserGroups, section?.id);
    },
    [section, handleOUSectionChanges]
  );

  const handleIntegrationFieldChanges = (type: filterFieldType, index: number, value: any) => {
    const nfilters = cloneDeep(section?.integration?.filters || []);
    if (nfilters.length) {
      let nField: sectionSelectedFilterType = cloneDeep(nfilters[index]);
      (nField as any)[type] = value;
      if (type === "key") {
        nField.param = "";
        nField.value = "";
      }
      if (type === "param") {
        nField.value = "";
      }
      nfilters[index] = nField;
      const nIntegration: filterFieldConfig = {
        ...(section?.integration || {}),
        filters: nfilters
      };
      handleOUSectionChanges("integration", nIntegration, section?.id);
    }
  };

  const integrationApplication = useMemo(() => section?.type?.split("@")?.[0], [section?.type]);
  return (
    <div className="integration-configure-container" key={section?.id}>
      <div className="static-content">
        <div className="integration-select">
          <p className="title">INTEGRATION:</p>
          <CustomSelect
            valueKey="value"
            labelKey="label"
            labelCase="none"
            style={{ width: "50%" }}
            showArrow={true}
            createOption={false}
            sortOptions
            mode="default"
            options={integrations || []}
            value={section?.type || ""}
            onChange={(value: string) => handleOUSectionChanges("type", value, section?.id)}
          />
        </div>
        <div className="action-container">
          <Tooltip
            title="Add Contributors"
            trigger="hover"
            overlayStyle={{ display: !!(section?.user_groups || []).length ? "none" : "" }}>
            <AntButton
              onClick={(e: any) => handleActionsClick(OrgUnitType.USER)}
              disabled={!!(section?.user_groups || []).length}>
              <Icon type="usergroup-add" />
            </AntButton>
          </Tooltip>
          <Tooltip title={!allFilterConfig?.length ? "No Filters Available" : "Add Filters"}>
            <span
              className={cx("action-filter-button", { "action-filter-button-not-allowed": !allFilterConfig?.length })}>
              <AntButton
                onClick={(e: any) => handleActionsClick(OrgUnitType.FILTER)}
                style={{ pointerEvents: !allFilterConfig.length && "none" }}
                disabled={!allFilterConfig.length}>
                <Icon type="filter" />
              </AntButton>
            </span>
          </Tooltip>

          <Tooltip title="Delete">
            <AntButton onClick={(e: any) => handleActionsClick(OrgUnitType.DELETE)}>
              <Icon type="delete" />
            </AntButton>
          </Tooltip>
        </div>
      </div>
      {loading ? (
        <div className="filter-loading-spinner">
          <Spin size="small" />
        </div>
      ) : (
        <div className="dynamic-content">
          {(section?.integration?.filters || []).map((filter: sectionSelectedFilterType, index: number) => {
            return (
              <OrgUnitIntegrationFilterField
                integrationApplication={integrationApplication}
                key={`${filter.key}_${index}`}
                apiLoading={loading}
                apiRecords={selectedIntegrationFilters || []}
                index={index}
                field={filter}
                handleRemoveFilter={handleRemoveFilter}
                handleFieldChange={handleIntegrationFieldChanges}
                allFiltersConfig={allFilterConfig}
                integrationIds={integrationIds}
              />
            );
          })}
          {(section?.user_groups || []).map(userGroup => {
            return (
              <OrganizationUnitUserSection
                userSectionId={userGroup?.id}
                fromIntegration={true}
                csv_users={userGroup?.csv_users || {}}
                users={userGroup?.users || []}
                dynamic_user_definitions={userGroup?.dynamic_user_definition || []}
                handleUpdateParent={(key: OrgUnitSectionKeys, value: any) =>
                  handleUpdateUserGroups(key, value, userGroup?.id)
                }
                handleRemoveUserGroups={() => {
                  handleRemoveUserGroups(userGroup?.id);
                }}
              />
            );
          })}
        </div>
      )}
      {!allFilterConfig?.length && <AntText className="filter-not-available">No Filters Available</AntText>}
    </div>
  );
};

export default OrganizationIntegrationConfigureComponent;
