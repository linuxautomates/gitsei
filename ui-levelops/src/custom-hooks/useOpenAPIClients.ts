/*
 * Copyright 2023 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */
import React, { useRef } from "react";
import { SEIServiceAPIClient } from "@harnessio/react-sei-service-client";
import { buildApiUrl } from "constants/restUri";
import AuthService from "services/authService";
import { getIsStandaloneApp } from "helper/helper";

type UseOpenApiClientsReturn = {
  seiServiceAPIClientRef: React.MutableRefObject<SEIServiceAPIClient>;
};

const authServices = new AuthService();
const getOpenAPIClientInitiator = (globalResponseHandler: (response: Response) => void, accountId: string): any => {
  const responseInterceptor = (response: Response): Response => {
    globalResponseHandler(response.clone());
    return response;
  };
  const urlInterceptor = (url: string): string => {
    return buildApiUrl(url);
  };
  const requestInterceptor = (request: Request): Request => {
    const oldRequest = request.clone();
    const headers = new Headers();
    const isStandaloneApp = getIsStandaloneApp();
    for (const key of oldRequest.headers.keys()) {
      const value = oldRequest.headers.get(key) as string;
      if (key.toLowerCase() !== "authorization") {
        headers.append(key, value);
      }
    }
    if (!window.noAuthHeader || isStandaloneApp) {
      headers.append("Authorization", `Bearer ${authServices.getToken()}`);
    }
    if (!isStandaloneApp) {
      headers.append("Harness-Account", accountId);
    }
    const newRequest = new Request(oldRequest, { headers });
    return newRequest;
  };
  return { responseInterceptor, urlInterceptor, requestInterceptor };
};

const useOpenApiClients = (
  globalResponseHandler: (response: Response) => void,
  accountId: string
): UseOpenApiClientsReturn => {
  const openAPIClientInitiator = getOpenAPIClientInitiator(globalResponseHandler, accountId);
  const seiServiceAPIClientRef = useRef(new SEIServiceAPIClient(openAPIClientInitiator));
  return {
    seiServiceAPIClientRef
  };
};

export default useOpenApiClients;
