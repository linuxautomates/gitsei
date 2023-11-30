import React, { useMemo } from "react";
import queryParser from "query-string";
import { useHistory, useLocation, useParams } from "react-router-dom";
import { get } from "lodash";
import { Divider, Icon, Popover } from "antd";
import { RestOrganizationUnit } from "classes/RestOrganizationUnit";
import { useParamSelector } from "reduxConfigs/selectors/selector";
import { orgUnitGetRestDataSelect } from "reduxConfigs/selectors/OrganizationUnitSelectors";
import OUOverviewComponent from "./OUOverviewComponent";
import { devProdOrgUnitSelect } from "reduxConfigs/selectors/devProductivity.selector";
import { calculateTotalEngineerScoreMapping } from "dashboard/pages/scorecard/helper";
import { scoreOverviewUtilType } from "dashboard/dashboard-types/engineerScoreCard.types";
import { getGenericRestAPISelector } from "reduxConfigs/selectors/genericRestApiSelector";
import "./ouScoreOverViewComponent.styles.scss";
import { WebRoutes } from "routes/WebRoutes";
import { useDispatch } from "react-redux";
import { genericRestAPISet } from "reduxConfigs/actions/restapi/genericSet.action";
import { ORG_UNIT_UTILITIES } from "configurations/pages/Organization/Constants";
import { ProjectPathProps } from "classes/routeInterface";
import { useHasConfigReadOnlyPermission } from "custom-hooks/HarnessPermissions/useHasConfigReadOnlyPermission";
import { getIsStandaloneApp } from "helper/helper";

const OUScoreOverViewComponent = () => {
  const location = useLocation();
  const history = useHistory();
  const dispatch = useDispatch();
  const projectParams = useParams<ProjectPathProps>();

  const { ou_id } = queryParser.parse(location.search);

  const orgUnit: RestOrganizationUnit = useParamSelector(orgUnitGetRestDataSelect, {
    id: ou_id
  });

  const tagListState = useParamSelector(getGenericRestAPISelector, {
    uri: "tags",
    method: "list",
    uuid: ou_id
  });

  const ouScoreState = useParamSelector(devProdOrgUnitSelect, {
    uri: "dev_productivity_org_unit_score_report",
    method: "list",
    id: ou_id
  });

  const ouScoreTransformedData: scoreOverviewUtilType = calculateTotalEngineerScoreMapping(ouScoreState);

  const handleOrgUnitSettingsClick = () => {
    dispatch(genericRestAPISet({}, "organization_unit_management", "get", "-1"));
    dispatch(genericRestAPISet({}, "organization_unit_management", ORG_UNIT_UTILITIES, "-1"));
    history.push(WebRoutes.organization_page.edit(projectParams, (ou_id || "") as string));
  };

  const renderPopoverContent = useMemo(
    () => <OUOverviewComponent orgUnit={orgUnit} tags={get(tagListState ?? {}, ["data", "records"], [])} />,
    [orgUnit, tagListState]
  );

  const hasConfigReadonly = useHasConfigReadOnlyPermission();

  return (
    <div className="ou-score-overview-component">
      <div className="summary-container">
        <p className="title">Summary</p>
        <div className="summary-fields">
          <div className="field-elements">
            <p className="field">Managers</p>
            <div className="value">{(orgUnit?.managers || []).map(manager => manager?.full_name).join(", ")}</div>
          </div>
        </div>
        <div className="org-unit-overview">
          <span>Collection Defintion</span>
          <Popover content={renderPopoverContent} placement="bottomLeft">
            <Icon type="question-circle" theme="filled" />
          </Popover>
        </div>
        <p className="ou-edit-link" onClick={handleOrgUnitSettingsClick}>
          Collection Settings
        </p>
        <Divider />
      </div>
      <div className="productivity-container">
        <p className="title">Trellis Score</p>
        <div className="score-container" style={{ backgroundImage: ouScoreTransformedData?.color }}>
          <div className="score">{ouScoreTransformedData.finalScore ?? 0}</div>
          <div className="score-desc">Overall Score</div>
        </div>
        {(getIsStandaloneApp() || !hasConfigReadonly) && (
          <p className="ou-edit-link" onClick={e => history.push(WebRoutes.trellis_profile.list())}>
            Trellis Score Settings
          </p>
        )}
      </div>
    </div>
  );
};

export default OUScoreOverViewComponent;
