import { ACCOUNT_ID, ORG_IDENTIFIER, PROJECT_IDENTIFIER, TOKEN_KEY } from "constants/localStorageKeys";
import { useAppStore } from "contexts/AppStoreContext";
import React, { useEffect } from "react";
import { useDispatch } from "react-redux";
import { sessionEntitlements, sessionProfile } from "reduxConfigs/actions/sessionActions";
import AuthService from "services/authService";
import { useWorkspace } from "custom-hooks/useWorkspace";
import { useGetEntitlementsQuery } from "@harnessio/react-sei-service-client";

const HarnessLogin = () => {
  const { accountInfo, selectedProject } = useAppStore();
  const authServices = new AuthService();
  const dispatch = useDispatch();
  const token = authServices.getToken(TOKEN_KEY);
  const { currentUserInfo, scope } = useAppStore();

  const { data, isFetching, error } = useGetEntitlementsQuery({
    queryParams: {
      defaultEntitlements: []
    }
  });

  useEffect(() => {
    if (!isFetching && !error && data) {
      const entitlements = data.content || [];
      dispatch(sessionEntitlements(entitlements));
    }
  }, [data, isFetching, error]);

  useWorkspace({
    accountId: accountInfo?.identifier || "",
    orgIdentifier: selectedProject?.orgIdentifier || "",
    projectIdentifier: selectedProject?.identifier || ""
  });

  useEffect(() => {
    dispatch(sessionProfile(token, scope.accountId, currentUserInfo.name, currentUserInfo.name, currentUserInfo.name));
  }, [token, currentUserInfo, scope]);

  useEffect(() => {
    localStorage.setItem(PROJECT_IDENTIFIER, selectedProject?.identifier || "");
    localStorage.setItem(ORG_IDENTIFIER, selectedProject?.orgIdentifier || "");
    localStorage.setItem(ACCOUNT_ID, scope.accountId || "");
  }, [selectedProject, accountInfo]);

  return <></>;
};

export default HarnessLogin;
