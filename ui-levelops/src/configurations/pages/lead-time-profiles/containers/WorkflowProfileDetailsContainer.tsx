import React from "react";
import { useLocation } from "react-router-dom";
import queryString from "query-string";
import WorkflowProfileCreateEditNewPage from "./workflowDetails/WorkflowProfileCreateEditNewPage";
import VelocityConfigsCreateEditPage from "./velocity-configs-edit/VelocityConfigsCreateEditPage";
import { useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { velocityConfigsGetSelector } from "reduxConfigs/selectors/velocityConfigs.selector";

const WorkflowProfileDetailsContainer = () => {
  const location = useLocation();
  const urlQuery = queryString.parse(location.search);
  let configId: string = (urlQuery.configId as string) || "new";
  let profileType: string = (urlQuery.profileType as string) || "old";

  const velocityConfigsListState = useParamSelector(velocityConfigsGetSelector, {
    config_id: configId
  });
  let newScreenFlag = velocityConfigsListState.is_new;

  //configId === "new" WILL COME IN CREATE PROFILE PART IF Entitlement & CONFIGID IS NEW THEN REDIRECT TO NEW SCREEN OTHERWISE OLD SCREEN
  //configId !== "new" && newScreenFlag IS TRUE THEN REDIRECT TO NEW SCREEN OTHERWISE OLD SCREEN (THIS ONLY WORKD FOR EDIT PART)

  if ((configId === "new" && profileType === "new") || newScreenFlag) {
    return <WorkflowProfileCreateEditNewPage />;
  }
  return <VelocityConfigsCreateEditPage />;
};

export default WorkflowProfileDetailsContainer;
