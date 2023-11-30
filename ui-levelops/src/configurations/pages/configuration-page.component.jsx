import React, { useMemo } from "react";
import * as PropTypes from "prop-types";
import { useSelector } from "react-redux";

import { AntCard, SvgIcon, AntRow, AntCol, AntIcon } from "shared-resources/components";
import "./configuration.style.scss";
import routes from "routes/routes";
import { useHasEntitlements } from "custom-hooks/useHasEntitlements";
import { Entitlement, EntitlementCheckType } from "custom-hooks/constants";
import { SETTINGS_TITLE_MAPPING } from "./constant";
import { groupBy } from "lodash";
import { isSelfOnboardingUser } from "reduxConfigs/selectors/session_current_user.selector";
import { svgIconsList } from "shared-resources/components/svg-icon/svg-icons.list";
import { sessionRBACSelector } from "reduxConfigs/selectors/session_base.selector";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";

export const ConfigurationPage = props => {
  const isTrialUser = useSelector(isSelfOnboardingUser);
  const rbac = useSelector(sessionRBACSelector);
  const entDevProd = useHasEntitlements(
    [Entitlement.SETTING_DEV_PRODUCTIVITY_READ, Entitlement.SETTING_DEV_PRODUCTIVITY],
    EntitlementCheckType.OR
  );
  const entSSO = useHasEntitlements(Entitlement.SETTING_SSO);
  const entEffortInvestment = useHasEntitlements(
    [Entitlement.SETTING_EFFORT_INVESTMENT, Entitlement.SETTING_EFFORT_INVESTMENT_READ],
    EntitlementCheckType.OR
  );
  const entWorkflowProfile = useHasEntitlements(
    [Entitlement.SETTING_WORKFLOW, Entitlement.SETTING_WORKFLOW_READ],
    EntitlementCheckType.OR
  );
  const entApiKeys = useHasEntitlements(
    [Entitlement.SETTING_API_KEYS, Entitlement.SETTING_API_KEYS_READ],
    EntitlementCheckType.OR
  );
  const entOrgUnits = useHasEntitlements(
    [Entitlement.SETTING_ORG_UNITS, Entitlement.SETTING_ORG_UNITS_READ],
    EntitlementCheckType.OR
  );
  const newTrellisProfile = useHasEntitlements(Entitlement.TRELLIS_BY_JOB_ROLES, EntitlementCheckType.AND);
  const centralPageAccess = window.isStandaloneApp ? getRBACPermission(PermeableMetrics.TRELLIS_CENTRAL_PROFILE_ACCESS) : true;
  const configurationItems = [
    "global-page",
    "user-page",
    ...(entSSO ? ["sso-page"] : []),
    !isTrialUser && "audit_logs",
    "integrations",
    ...(entOrgUnits ? ["organization", "organization-users"] : []),
    ...(entApiKeys && !isTrialUser ? ["apikeys"] : []),
    !isTrialUser && "sla",
    ...(entWorkflowProfile ? ["lead-time-profile"] : []),
    ...(!newTrellisProfile && entDevProd ? ["trellis_score_profile"] : []),
    ...(newTrellisProfile && entDevProd && centralPageAccess ? ["trellis_central_profile"] : []),
    ...(entEffortInvestment ? ["effort-investment"] : []),
    !isTrialUser && "reports",
    !isTrialUser && "table-configs"
  ];
  const configurationRoutes = useMemo(() => {
    const items = [];
    configurationItems.map(i => (items[i] = {}));
    const filteredRoutes = routes().reduce((carry, route) => {
      if (route.hide !== true && configurationItems.includes(route.id)) {
        if (window.isStandaloneApp) {
          if (route.rbac.includes(rbac)) {
            carry[route.id] = route;
          }
        } else {
          carry[route.id] = route;
        }
      }
      return carry;
    }, items);
    return groupBy(
      (Object.values(filteredRoutes) || []).filter(route => Object.keys(route || {}).length),
      "settingsGroupId"
    );
  }, [configurationItems, rbac]);

  const { className, history } = props;
  return (
    <div className={className}>
      {Object.values(configurationRoutes || [])?.map((groupRoute, index) => {
        return (
          <AntRow key={`${index}`} gutter={[30, 30]}>
            {groupRoute.map((route, groupIndex) => {
              return (
                <>
                  {groupIndex === 0 && (
                    <AntRow className="settings-title">
                      <AntCol>
                        <h3 htmlFor="">{SETTINGS_TITLE_MAPPING?.[route?.settingsGroupId]}</h3>
                        <hr className="hr-border" />
                      </AntCol>
                    </AntRow>
                  )}
                  <AntCol key={route.id} xs={12} sm={12} md={9} lg={7} xl={5} span={8}>
                    <AntCard
                      className="configuration__card"
                      key={route.id}
                      onClickEvent={() => {
                        history.push(`${route.layout}${route.path}`);
                      }}>
                      <AntRow className="flex" gutter={[24, 24]}>
                        <AntCol className="p-r-0">
                          {!!svgIconsList[route.icon] ? (
                            <SvgIcon icon={route.icon} style={{ width: 24, height: 24 }} />
                          ) : (
                            <AntIcon type={route.icon} className={route.icon_classname ?? ""} />
                          )}
                        </AntCol>
                        <AntCol>
                          <div className="configuration__card-label">{route.label}</div>
                        </AntCol>
                      </AntRow>
                      <AntRow className="configuration__card-desc">{route?.settingsDescription}</AntRow>
                    </AntCard>
                  </AntCol>
                </>
              );
            })}
          </AntRow>
        );
      })}
    </div>
  );
};

ConfigurationPage.propTypes = {
  className: PropTypes.string,
  rbac: PropTypes.string,
  history: PropTypes.object.isRequired
};

ConfigurationPage.defaultProps = {
  className: "configuration-page",
  rbac: "admin"
};
