import React, { useEffect, useMemo } from "react";
import { useHistory, useLocation } from "react-router-dom";
import { useDispatch } from "react-redux";
import { get } from "lodash";
import queryString from "query-string";
import { getIntegrationPage } from "constants/routePaths";
import { integrationSubType, integrationType } from "reduxConfigs/actions/integrationActions";
import { setPageSettings } from "reduxConfigs/actions/pagesettings.actions";
import { AntButton, AntCard, AntTitle } from "shared-resources/components";
import GitlabOuthCard from "./components/gitlab-outh-card";
import { IntegrationAuthTypes, INTEGRATION_DETAILS, getBreadcrumbsForCreatePage } from "./helpers";
import "./integration-types.style.scss";

const AddIntegrationLandingPage: React.FC = () => {
  const history = useHistory();
  const location = useLocation();
  const dispatch = useDispatch();

  const application: string = (queryString.parse(location.search)?.application as string) || "";

  useEffect(() => {
    const settings = {
      title: "Integrations",
      bread_crumbs: getBreadcrumbsForCreatePage(application)
    };
    dispatch(setPageSettings(location.pathname, settings));
  }, []);

  const handleTypeClick = (type: IntegrationAuthTypes) => {
    return () => {
      dispatch(integrationType(application));
      dispatch(integrationSubType(type));
      history.push(`${getIntegrationPage()}/new-add-integration-page`);
    };
  };

  const renderTypes = useMemo(() => {
    const types = get(INTEGRATION_DETAILS, [application], []);
    return (
      <>
        {types.map((item: any, index: number) => {
          return (
            <AntCard
              title={item.title}
              className="integration-type-card"
              actions={[
                <AntButton className="select-type-btn" type="secondary" onClick={handleTypeClick(item.type)}>
                  Select
                </AntButton>
              ]}>
              <div className="card-content">
                <p>{item.description}</p>
              </div>
            </AntCard>
          );
        })}
      </>
    );
  }, [application]);

  return (
    <div className="flex flex-row align-center justify-center landing-page">
      <div>
        <AntTitle className="hint-label" level={4}>
          Select how to integrate
        </AntTitle>
        <div>
          <div className="flex">{renderTypes}</div>
        </div>
      </div>
    </div>
  );
};

export default AddIntegrationLandingPage;
