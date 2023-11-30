import React, { useCallback, useEffect, useMemo, useState } from "react";
import { Card, Icon } from "antd";
import { v1 as uuid } from "uuid";
import {
  managersConfigType,
  orgUnitJSONType,
  OrgUnitListType,
  PivotType
} from "configurations/configuration-types/OUTypes";
import queryString from "query-string";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { useDispatch, useSelector } from "react-redux";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import {
  ORGANIZATION_UNIT_NODE,
  NEW_ORG_UNIT_ID,
  ORG_UNIT_LIST_ID,
  ORG_UNIT_CLONE_ID,
  ORG_UNIT_DELETE_ID
} from "configurations/pages/Organization/Constants";
import { Entitlement, EntitlementCheckType, TOOLTIP_ACTION_NOT_ALLOWED } from "custom-hooks/constants";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { WebRoutes } from "routes/WebRoutes";
import { AntTooltip, AntButton, TableRowActions, AntText } from "shared-resources/components";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { ServerPaginatedTable } from "shared-resources/containers";
import {
  orgUnitTableConfig,
  renderAssociatedProfileColumns
} from "configurations/pages/Organization/User/container/UserListPage/tableConfig";
import { OrganizationUnitDelete } from "reduxConfigs/actions/restapi/OrganizationUnit.action";
import { orgUnitBulkDeleteDataState, orgUnitCloneDataState } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { cloneDeep, get } from "lodash";
import OrgUnitTreeViewComponent from "../../components/org-unit-tree-view/OrgUnitTreeViewComponent";
import OrgUnitCloneModalComponent from "../../components/OrgUnitCloneModalComponent";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { actionsColumn } from "utils/tableUtils";
import RootOUComponent from "../../components/root-ou-component/RootOUComponent";
import "./orgUnitListContainer.styles.scss";
import { restapiClear } from "reduxConfigs/actions/restapi";
import OrgTrellisAssociationModal from "../OrgTrellisAssociationModal";
import OrgWorkflowAssociationModal from "../OrgWorkflowAssociationModal";
import { sessionUserWorkspacesSelections } from "reduxConfigs/selectors/session_current_user.selector";
import LocalStoreService from "services/localStoreService";
import { USERROLES } from "routes/helper/constants";
import { Link } from "react-router-dom";
import { useCollectionPermissions } from "custom-hooks/HarnessPermissions/useCollectionPermissions";
import { getIsStandaloneApp, hasAccessFromHarness } from "helper/helper";
import { ProjectPathProps } from "classes/routeInterface";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";

interface OrgUnitListContainerProps {
  activeTab: PivotType;
  resetTabs: () => void;
  currentWorkspaceIsDemo: boolean;
  ouEnhancementSupport?: boolean;
}

const OrgUnitListContainer: React.FC<OrgUnitListContainerProps> = ({
  activeTab,
  resetTabs,
  currentWorkspaceIsDemo,
  ouEnhancementSupport = false
}) => {
  const [listView, setListView] = useState<OrgUnitListType>(OrgUnitListType.FLAT_LIST);
  const [showOrgTrellisMapping, setShowOrgTrellisMapping] = useState<boolean>(false);
  const [orgUnitsDeleteLoading, setOrgUnitsDeleteLoading] = useState<boolean>(false);
  const [orgUnitsCloneLoading, setOrgUnitsCloneLoading] = useState<boolean>(false);
  const [orgUnitToClone, setOrgUnitToClone] = useState<orgUnitJSONType>();
  const [orgUntiDeleteId, setOrgUnitDeleteId] = useState<string>(ORG_UNIT_DELETE_ID);
  const [orgUnitCloneId, setOrgUnitCloneId] = useState<string>(ORG_UNIT_CLONE_ID);
  const [orgToAssociate, setOrgToAssociate] = useState<orgUnitJSONType>();
  const [showOrgWorkflowMapping, setShowOrgWorkflowMapping] = useState<boolean>(false);

  const orgUnitDeleteState = useParamSelector(orgUnitBulkDeleteDataState, { id: orgUntiDeleteId });
  const orgUnitCloneState = useParamSelector(orgUnitCloneDataState, { id: orgUnitCloneId });
  const entOrgUnits = useHasEntitlements(Entitlement.SETTING_ORG_UNITS);
  const entOrgUnitsCountExceed = useHasEntitlements(Entitlement.SETTING_ORG_UNITS_COUNT_5, EntitlementCheckType.AND);
  const newTrellisProfile = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);

  const isStandaloneApp = getIsStandaloneApp();
  const projectParams = useParams<ProjectPathProps>();
  const history = useHistory();
  const location = useLocation();
  const ls = new LocalStoreService();
  const userRole = ls.getUserRbac();
  const isConfigReadonly = useHasConfigReadOnlyPermission();
  const isAdmin = userRole?.toLowerCase() === USERROLES.ADMIN;
  const dispatch = useDispatch();
  const allowedWorkspaces: Record<string, string[]> = useSelector(sessionUserWorkspacesSelections);
  const { ou_workspace_id } = queryString.parse(location.search);
  const handleListViewChange = (view: OrgUnitListType) => {
    setListView(view);
  };
  const accesses = useCollectionPermissions();

  useEffect(() => {
    if (orgUnitsDeleteLoading) {
      const { loading, error } = orgUnitDeleteState;
      if (!loading && !error) {
        const status = get(orgUnitDeleteState, ["data"], "false");
        if (status === "ok") {
          resetTabs();
        }
        setOrgUnitsDeleteLoading(false);
      }
    }
  }, [orgUnitDeleteState, orgUnitsDeleteLoading, activeTab]);

  useEffect(() => {
    if (orgUnitsCloneLoading) {
      const { loading, error } = orgUnitCloneState;
      if (!loading && !error) {
        const status = get(orgUnitCloneState, ["data", "success"], []);
        if (status.length) {
          dispatch(restapiClear(ORGANIZATION_UNIT_NODE, "create", orgUnitCloneId));
          resetTabs();
        }
        setOrgUnitsCloneLoading(false);
      }
    }
  }, [orgUnitCloneState, orgUnitsCloneLoading, activeTab, orgUnitCloneId]);

  const handleCreateOrgUnit = useCallback(() => {
    dispatch(
      genericRestAPISet(
        new RestOrganizationUnit({ version: "1", ou_category_id: activeTab.id }).json,
        ORGANIZATION_UNIT_NODE,
        "create",
        NEW_ORG_UNIT_ID
      )
    );
    history.push(WebRoutes.organization_page.create_org_unit(projectParams, ou_workspace_id as string, activeTab.id));
  }, [activeTab, ou_workspace_id]);

  const renderTitle = useMemo(() => {
    const treeViewAccess = isStandaloneApp
      ? getRBACPermission(PermeableMetrics.ORG_UNIT_LIST_TREE_VIEW)
      : !isConfigReadonly;
    return (
      <>
        {treeViewAccess ? (
          <div className="mb-10">
            View :
            <span
              className="ml-10 mr-10"
              style={{
                color: listView === OrgUnitListType.FLAT_LIST ? "var(--blueText)" : "var(--title)",
                cursor: "pointer"
              }}
              onClick={e => handleListViewChange(OrgUnitListType.FLAT_LIST)}>
              Flat List
            </span>
            {isAdmin && (
              <>
                |
                <span
                  className="ml-10"
                  style={{
                    color: listView === OrgUnitListType.TREE_LIST ? "var(--blueText)" : "var(--title)",
                    cursor: "pointer"
                  }}
                  onClick={e => handleListViewChange(OrgUnitListType.TREE_LIST)}>
                  Tree View
                </span>
              </>
            )}
          </div>
        ) : (
          " "
        )}
        {listView === OrgUnitListType.FLAT_LIST && (
          <RootOUComponent
            category_id={activeTab.id}
            workspace_id={ou_workspace_id as string}
            currentWorkspaceIsDemo={currentWorkspaceIsDemo}
          />
        )}
      </>
    );
  }, [listView, activeTab, ou_workspace_id, currentWorkspaceIsDemo]);

  const renderExtraSuffixContent = useMemo(() => {
    const hasCreateAccess = isStandaloneApp ? getRBACPermission(PermeableMetrics.ORG_UNIT_CREATE) : accesses[0];
    return hasCreateAccess ? (
      <span>
        <AntTooltip title={!entOrgUnits || entOrgUnitsCountExceed ? TOOLTIP_ACTION_NOT_ALLOWED : ""}>
          <AntButton
            type="primary"
            onClick={handleCreateOrgUnit}
            disabled={!entOrgUnits || entOrgUnitsCountExceed || currentWorkspaceIsDemo}>
            <Icon type="plus" />
            {activeTab.name ?? "Create Collection"}
          </AntButton>
        </AntTooltip>
      </span>
    ) : (
      <></>
    );
  }, [handleCreateOrgUnit, activeTab, entOrgUnits, currentWorkspaceIsDemo, accesses, isStandaloneApp]);

  const buildActionOptions = useCallback(
    (orgUnit: orgUnitJSONType) => {
      const hasDeletePermission = orgUnit.access_response ? orgUnit.access_response.delete : true;
      const hasCreatePermission = orgUnit.access_response ? orgUnit.access_response.create : true;
      const actions = [
        {
          type: "copy",
          id: orgUnit.id || "",
          description: "Clone",
          onClickEvent: () => {
            const cloneId = `${orgUnit.id}_${uuid()}_clone`;
            setOrgUnitsCloneLoading(true);
            setOrgUnitToClone(orgUnit);
            setOrgUnitCloneId(cloneId);
          },
          disabled:
            !entOrgUnits ||
            entOrgUnitsCountExceed ||
            !orgUnit.parent_ref_id ||
            currentWorkspaceIsDemo ||
            !hasDeletePermission,
          tooltip: !entOrgUnits || entOrgUnitsCountExceed || !orgUnit.parent_ref_id ? TOOLTIP_ACTION_NOT_ALLOWED : ""
        },
        {
          type: "delete",
          id: orgUnit.id || "",
          description: "Delete",
          onClickEvent: () => {
            const deleteId = ORG_UNIT_DELETE_ID.concat(orgUnit.id || "");
            setOrgUnitsDeleteLoading(true);
            setOrgUnitDeleteId(deleteId);
            dispatch(
              OrganizationUnitDelete(
                {
                  page: 0,
                  page_size: 20,
                  filter: { parent_ref_id: orgUnit?.id, ou_category_id: [activeTab.id] }
                },
                deleteId
              )
            );
          },
          disabled:
            !entOrgUnits ||
            entOrgUnitsCountExceed ||
            !orgUnit.parent_ref_id ||
            currentWorkspaceIsDemo ||
            !hasCreatePermission,
          tooltip:
            !entOrgUnits || entOrgUnitsCountExceed || !orgUnit.parent_ref_id || !hasCreatePermission
              ? TOOLTIP_ACTION_NOT_ALLOWED
              : ""
        }
      ];
      return <TableRowActions actions={actions} />;
    },
    [
      orgUnitCloneState,
      orgUnitDeleteState,
      orgUnitsCloneLoading,
      orgUnitsDeleteLoading,
      entOrgUnits,
      currentWorkspaceIsDemo,
      entOrgUnitsCountExceed
    ]
  );

  const onAssociateClick = (profile: "trellis" | "workflow", org: orgUnitJSONType) => {
    setOrgToAssociate(org);
    if (profile === "trellis") {
      setShowOrgTrellisMapping(true);
    }
    if (profile === "workflow") {
      setShowOrgWorkflowMapping(true);
    }
  };

  const mappedColumns = useMemo(() => {
    let columns = cloneDeep(orgUnitTableConfig);
    const hasAccess = isStandaloneApp ? isAdmin : !isConfigReadonly;
    if (!hasAccess || newTrellisProfile) {
      columns = columns.filter(col => col?.key !== "trellis_profile_name");
    }

    if (ouEnhancementSupport) {
      columns = columns.filter(col => col.dataIndex !== "managers");
      if (isStandaloneApp) {
        columns.push({
          title: "Collection Admins",
          dataIndex: "admins",
          key: "admins",
          render: (item: managersConfigType[]) => {
            return (
              <div>
                {(item || []).map((admin, index) => (
                  <span>{`${index > 0 ? "," : ""} ${admin.email}`}</span>
                ))}
              </div>
            );
          },
          width: "10%"
        });
      }
    }
    const hasDeleteAccess = isStandaloneApp ? getRBACPermission(PermeableMetrics.ORG_UNIT_LIST_PAGE_ACTIONS) : true;
    if (hasDeleteAccess) {
      columns.push({ ...actionsColumn(), width: "10%" });
    }

    return columns.map(column => {
      switch (column.key) {
        case "id":
          return {
            ...column,
            render: (item: any, record: orgUnitJSONType) => buildActionOptions(record)
          };
        case "name":
          const nColumn = cloneDeep(column);
          return {
            ...nColumn,
            morePayload: {
              fields: ["name"],
              ou_category_id: [activeTab?.id]
            },
            render: (item: string, record: orgUnitJSONType) => {
              const _url = WebRoutes.organization_page.edit(
                projectParams,
                record?.id,
                ou_workspace_id as string,
                activeTab.id
              );
              const hasUpdateAccess = isStandaloneApp ? getRBACPermission(PermeableMetrics.ORG_UNIT_UPDATE) : true;
              return (
                <AntText className={"pl-10"} style={{ color: "var(--link-and-actions)" }}>
                  {hasUpdateAccess && !currentWorkspaceIsDemo ? (
                    <Link className={"ellipsis"} to={_url}>
                      {item}
                    </Link>
                  ) : (
                    item
                  )}
                </AntText>
              );
            }
          };
        case "trellis_profile_name":
          return {
            ...column,
            render: renderAssociatedProfileColumns(onAssociateClick)
          };
        default:
          return column;
      }
    });
  }, [
    entOrgUnits,
    entOrgUnitsCountExceed,
    activeTab,
    ou_workspace_id,
    currentWorkspaceIsDemo,
    ouEnhancementSupport,
    newTrellisProfile,
    isConfigReadonly,
    isAdmin
  ]);

  if (!activeTab?.count_of_ous) {
    return (
      <Card bordered={false} className="pt-40">
        <div className="flex direction-column align-center justify-center">
          <div className="mb-10">
            {" "}
            Add a Collection to get started. Collections can be used as filters in your insights.
          </div>
          {renderExtraSuffixContent}
        </div>
      </Card>
    );
  }

  const handleCloseOUCloneModal = (clonning?: boolean) => {
    setOrgUnitToClone(undefined);
    if (!clonning) {
      setOrgUnitsCloneLoading(false);
    }
  };

  const onTrellisAssociationDismiss = () => setShowOrgTrellisMapping(false);

  const onWorkflowAssociationDismiss = () => setShowOrgWorkflowMapping(false);

  const orgUnitTransformRecordsBasedOnRBAC = (data: { records: orgUnitJSONType[] }) => {
    if (!("records" in data)) return [];
    const hasAccess = isStandaloneApp ? isAdmin : true;
    if (hasAccess) {
      // Admin will always have all access
      return get(data, ["records"], []).filter(rec =>
        !isStandaloneApp && rec.access_response ? rec.access_response.view : true
      );
    }
    if (allowedWorkspaces && Object.keys(allowedWorkspaces).length) {
      // org admin workspace OU check
      const allowedOUs = allowedWorkspaces[(ou_workspace_id ?? "") as string];
      return get(data, ["records"], []).filter(rec => allowedOUs.includes(rec?.id ?? ""));
    }
    return [];
  };

  return (
    <>
      {orgUnitToClone && (
        <OrgUnitCloneModalComponent
          orgUnit={orgUnitToClone}
          setVisiblitiyOff={handleCloseOUCloneModal}
          cloneId={orgUnitCloneId}
        />
      )}
      {listView === OrgUnitListType.FLAT_LIST ? (
        <ServerPaginatedTable
          title={renderTitle}
          uri={"organization_unit_management"}
          columns={mappedColumns}
          hasFilters={true}
          hasDelete={false}
          hasSearch={true}
          transformRecordsData={orgUnitTransformRecordsBasedOnRBAC}
          extraSuffixActionButtons={renderExtraSuffixContent}
          moreFilters={{ ou_category_id: [activeTab.id] }}
          showCustomFilters={false}
          uuid={`${ORG_UNIT_LIST_ID}_${activeTab.id}`}
          displayCount={false}
          rowKey={"ou_id"}
          pageSize={ouEnhancementSupport ? 100 : 50}
        />
      ) : (
        <OrgUnitTreeViewComponent
          title={renderTitle}
          extra={renderExtraSuffixContent}
          ouGroupId={activeTab.id}
          disableSelect={true}
        />
      )}
      <OrgTrellisAssociationModal
        showModal={showOrgTrellisMapping}
        onCancel={onTrellisAssociationDismiss}
        onAssociationSuccess={resetTabs}
        org={orgToAssociate}
      />
      <OrgWorkflowAssociationModal
        showModal={showOrgWorkflowMapping}
        onCancel={onWorkflowAssociationDismiss}
        onAssociationSuccess={resetTabs}
        org={orgToAssociate}
        workspaceId={ou_workspace_id}
      />
    </>
  );
};

export default OrgUnitListContainer;
