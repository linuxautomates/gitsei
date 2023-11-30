import React, { useCallback, useMemo } from "react";
import { useDispatch } from "react-redux";
import { useHistory } from "react-router-dom";
import { Empty } from "antd";
import {
  ticketCategorizationSchemeClone,
  ticketCategorizationSchemeResetColorPalette,
  ticketCategorizationSchemesDelete,
  ticketCategorizationSchemeSetToDefault
} from "reduxConfigs/actions/restapi/ticketCategorizationSchemes.action";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { ticketCategorizationSchemesRestListSelector } from "reduxConfigs/selectors/ticketCategorizationSchemes.selector";
import { WebRoutes } from "routes/WebRoutes";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import { TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID } from "../../constants/ticket-categorization.constants";
import { EIConfigurationTabs } from "../../types/ticketCategorization.types";
import { ProfilesPaginatedTable } from "shared-resources/containers";
import {
  actionColumn,
  categoriesColumn,
  defaultColumn,
  descriptionColumn,
  nameColumn,
  PROFILE_ACTIONS
} from "../../helper/profiles.helper";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { useConfigScreenPermissions } from "custom-hooks/HarnessPermissions/useConfigScreenPermissions";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";

const ProfilesLandingPageContent: React.FC = () => {
  const history = useHistory();
  const dispatch = useDispatch();

  const profileRestListState: RestTicketCategorizationScheme[] = useParamSelector(
    ticketCategorizationSchemesRestListSelector,
    {
      id: TICKET_CATEGORIZATION_SCHEMES_SEARCH_ID
    }
  );

  const editUrl = (profileId: string) =>
    WebRoutes.ticket_categorization.scheme.edit(profileId, EIConfigurationTabs.BASIC_INFO);

  const handleMenuClick = useCallback((key: string, profileId: string) => {
    switch (key) {
      case PROFILE_ACTIONS.EDIT:
        history.push(editUrl(profileId));
        break;
      case PROFILE_ACTIONS.DELETE:
        dispatch(ticketCategorizationSchemesDelete(profileId || ""));
        break;
      case PROFILE_ACTIONS.CLONE:
        dispatch(ticketCategorizationSchemeClone(profileId, "list"));
        break;
      case PROFILE_ACTIONS.RESET_COLOR_PALETTE:
        dispatch(ticketCategorizationSchemeResetColorPalette(profileId));
        break;
      case "set-default":
        dispatch(ticketCategorizationSchemeSetToDefault(profileId));
        break;
      default:
        return;
    }
  }, []);

  const access = useConfigScreenPermissions();

  const actionColumns = useMemo(() => {
    if (window.isStandaloneApp) {
      return [PROFILE_ACTIONS.EDIT, PROFILE_ACTIONS.CLONE, PROFILE_ACTIONS.RESET_COLOR_PALETTE, PROFILE_ACTIONS.DELETE];
    }
    let columns = [PROFILE_ACTIONS.EDIT];
    if (access) {
      access[0] && columns.push(PROFILE_ACTIONS.CLONE);
      access[1] && columns.push(PROFILE_ACTIONS.RESET_COLOR_PALETTE);
      access[2] && columns.push(PROFILE_ACTIONS.DELETE);
    }
    return columns;
  }, [access]);

  const isHarnessReadonly = useHasConfigReadOnlyPermission();
  const oldReadOnly = getRBACPermission(PermeableMetrics.EFFORT_INVESTMENT_READ_ONLY);
  const isReadOnly = window.isStandaloneApp ? oldReadOnly : isHarnessReadonly;

  const columns = useMemo(() => {
    const baseColumns = [
      nameColumn(profileRestListState, editUrl),
      descriptionColumn(),
      categoriesColumn("categories"),
      defaultColumn("defaultScheme", handleMenuClick, isReadOnly)
    ];

    if (!isReadOnly) {
      baseColumns.push(actionColumn(actionColumns, handleMenuClick));
    }

    return baseColumns;
  }, [handleMenuClick, profileRestListState, actionColumns]);

  if (!(profileRestListState || []).length) return <Empty />;

  return <ProfilesPaginatedTable dataSource={profileRestListState} columns={columns} />;
};

export default ProfilesLandingPageContent;
