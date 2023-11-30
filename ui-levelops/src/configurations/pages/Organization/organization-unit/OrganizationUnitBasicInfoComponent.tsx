import React, { useEffect, useRef, useState, useCallback, useMemo } from "react";
import queryString from "query-string";
import { Empty, Form, Typography } from "antd";
import { AntInput, AntRow, AntSelect, AntTag, AntText } from "shared-resources/components";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { managersConfigType, orgUnitBasicInfoType, PivotType } from "configurations/configuration-types/OUTypes";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { getOrgUnitUtility } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { forEach, isEqual } from "lodash";
import { getManagerOptions } from "../Helpers/OrgUnit.helper";
import { SelectRestapi } from "shared-resources/helpers";
import { tagsList } from "reduxConfigs/actions/restapi";
import { DynamicDropDownType } from "./../../constant";
import { transformOUParentNodeOptions } from "./components/helpers";
import OrgUnitNameComponent from "./components/OrgUnitNameComponent";
import { sanitizeObjectCompletely } from "utils/commonUtils";
import { orgUnitPivotsList } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { PIVOT_LIST_ID } from "../Constants";
import { useDispatch } from "react-redux";
import { useLocation } from "react-router-dom";
import Loader from "components/Loader/Loader";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";
import { getIsStandaloneApp } from "helper/helper";

interface OrgUnitBasicInfoProps {
  handleOUChanges: (key: orgUnitBasicInfoType, value: any) => void;
  pivotList: Array<PivotType>;
  pivot: PivotType;
  parentNodeRequired: boolean;
  draftOrgUnit: RestOrganizationUnit;
}

const OrgUnitBasicInfoComponent: React.FC<OrgUnitBasicInfoProps> = ({
  handleOUChanges,
  draftOrgUnit,
  pivotList,
  pivot,
  parentNodeRequired
}) => {
  const managers = useParamSelector(getOrgUnitUtility, { utility: "users" });
  const [tags, setTags] = useState<Array<any>>([]);
  const [refreshParentOptions, setRefreshParentOptions] = useState<number>(0);
  const [pivotsLoading, setPivotsLoading] = useState<boolean>(false);
  const orgUnitEnhancementSupport = useHasEntitlements(Entitlement.ORG_UNIT_ENHANCEMENTS, EntitlementCheckType.AND);
  const dispatch = useDispatch();
  const location = useLocation();
  let { ou_workspace_id, ou_category_tab } = queryString.parse(location.search);
  const prevParent = useRef<string | undefined>(draftOrgUnit.ouGroupId);

  const isConfigReadonly = useHasConfigReadOnlyPermission();
  const oldReadOnly = getRBACPermission(PermeableMetrics.ORG_UNIT_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isConfigReadonly;

  useEffect(() => {
    if (!isEqual(prevParent.current, draftOrgUnit.ouGroupId)) {
      prevParent.current = draftOrgUnit.ouGroupId;
      setRefreshParentOptions(p => p + 1);
    }
  }, [draftOrgUnit.ouGroupId]);

  useEffect(() => {
    const savedTags = draftOrgUnit?.tags?.map((tag: string) =>
      tag.includes("create:") ? { key: tag, label: tag.substring(7) } : { key: tag }
    );
    setTags(savedTags || []);
  }, [draftOrgUnit?.tags]);

  const getManagersChangeValue = useCallback(
    (value: string[]) => {
      let selectedManagers: managersConfigType[] = [];
      forEach(value, (key: string) => {
        const manager = (managers || []).find((manager: managersConfigType) => manager?.id === key);
        if (manager) {
          selectedManagers.push(manager);
        }
      });
      return selectedManagers;
    },
    [managers]
  );

  const getSelectedManagers = useMemo(() => {
    const managers = draftOrgUnit?.managers || [];
    return managers.map(manager => manager?.id || "");
  }, [draftOrgUnit]);

  const onOptionFilter = useCallback((value: string, option: any) => {
    if (!value) return true;
    return (option?.label || "").toLowerCase().includes(value.toLowerCase());
  }, []);

  const handleTagsChange = useCallback(
    (selectedTags: any[]) => {
      setTags(selectedTags);
      const tagIds = selectedTags.map((item: any) => item.key);
      handleOUChanges("tags", tagIds);
    },
    [handleOUChanges]
  );

  const handleParentOptionsTransform = useCallback(
    (list: any[]) => {
      return transformOUParentNodeOptions(list, draftOrgUnit.name ?? "", !!draftOrgUnit?.id);
    },
    [draftOrgUnit]
  );

  const handleParentNodeChange = useCallback(
    (e: any) => handleOUChanges("parentId", parseInt(e?.value)),
    [draftOrgUnit]
  );
  const handleOUNameChange = useCallback((value: string) => handleOUChanges("name", value), [draftOrgUnit]);
  const handleOUValidStatusChange = useCallback(
    (value: boolean) => handleOUChanges("validName", value),
    [draftOrgUnit]
  );

  const getPivotOptions = useMemo(() => {
    if (pivotList?.length) {
      if (pivotsLoading) setPivotsLoading(false);
      return pivotList.map((pivot: PivotType) => ({ label: pivot.name, value: pivot.id }));
    }
    return [];
  }, [pivotList, pivotsLoading, ou_workspace_id]);

  useEffect(() => {
    if (!(pivotList ?? []).length && !pivotsLoading) {
      setPivotsLoading(true);
      const filters = sanitizeObjectCompletely({ filter: { workspace_id: [ou_workspace_id] } });
      dispatch(orgUnitPivotsList(PIVOT_LIST_ID, filters));
    }
  }, [pivotList, pivotsLoading, ou_workspace_id]);

  return (
    <div className="org-unit-basic-info-container">
      <Typography.Title level={4}>BASIC INFO</Typography.Title>

      <AntRow className="basic-info-content-container">
        <OrgUnitNameComponent
          onChange={handleOUNameChange}
          onValidStatusChange={handleOUValidStatusChange}
          name={draftOrgUnit?.name || ""}
          draftOrgUnit={draftOrgUnit}
        />
        <Form.Item label="Description" colon={false}>
          <AntInput
            disabled={isReadOnly}
            value={draftOrgUnit?.description || ""}
            onChange={(e: any) => handleOUChanges("description", e.target.value)}
            placeholder="Description"
          />
        </Form.Item>
        <Form.Item label="Collections Category" colon={false}>
          {draftOrgUnit?.id && !draftOrgUnit?.isParent ? (
            <AntSelect
              options={getPivotOptions}
              disabled={isReadOnly}
              value={draftOrgUnit?.ouGroupId}
              onChange={(value: string) => handleOUChanges("ouGroupId", value)}
              showSearch
              showArrow
              onOptionFilter={onOptionFilter}
            />
          ) : (
            <AntText>{pivot?.name}</AntText>
          )}
        </Form.Item>
      </AntRow>
      <AntRow className="basic-info-content-container">
        {getIsStandaloneApp() && (
          <>
            {orgUnitEnhancementSupport ? (
              <Form.Item
                label="Collection Admins"
                colon={false}
                className={`${draftOrgUnit?.admins?.length ? "" : "admins-form-item"}`}>
                <div className="admins-tag-container">
                  {(draftOrgUnit.admins ?? []).map(admin => (
                    <AntTag key={admin.id} className="admin-tag">
                      {admin.full_name}
                    </AntTag>
                  ))}
                </div>
              </Form.Item>
            ) : (
              <Form.Item label="Managers" colon={false}>
                <AntSelect
                  options={getManagerOptions(managers)}
                  value={getSelectedManagers}
                  onChange={(value: string[]) => handleOUChanges("managers", getManagersChangeValue(value))}
                  mode="multiple"
                  disabled={isReadOnly}
                  defaultValue="Select"
                  showSearch
                  showArrow
                  onOptionFilter={onOptionFilter}
                />
              </Form.Item>
            )}
          </>
        )}

        <Form.Item label="Tags" colon={false}>
          <SelectRestapi
            value={tags}
            mode={"multiple"}
            uri={"tags"}
            disabled={isReadOnly}
            labelInValue
            fetchData={tagsList}
            createOption={true}
            searchField="name"
            onChange={handleTagsChange}
            dynamicValueType={DynamicDropDownType.OU}
          />
        </Form.Item>
        <Form.Item label="Parent Collection" colon={false} required={parentNodeRequired}>
          {draftOrgUnit?.isParent ? (
            <AntText>This Collection is the root collection. </AntText>
          ) : (
            <SelectRestapi
              value={draftOrgUnit?.parentId?.toString()}
              mode={"default"}
              uri={"organization_unit_management"}
              labelInValue
              allowClear={false}
              createOption={false}
              notFoundCallRender={(loading: boolean) => (loading ? <Loader /> : <Empty />)}
              refresh={refreshParentOptions}
              disabled={isReadOnly}
              transformOptions={handleParentOptionsTransform}
              searchField="name"
              moreFilters={{ ou_category_id: [draftOrgUnit?.ouGroupId ?? ou_category_tab] }}
              onChange={handleParentNodeChange}
              dynamicValueType={DynamicDropDownType.OU}
            />
          )}
        </Form.Item>
      </AntRow>
    </div>
  );
};

export default OrgUnitBasicInfoComponent;
