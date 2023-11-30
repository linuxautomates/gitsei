import axios from "axios";
import {
  LOGIN,
  REFRESH,
  buildApiUrl,
  PASSWORD_RESET,
  PASSWORD_CHANGE,
  VALIDATE_EMAIL,
  VALIDATE_COMPANY
} from "../constants/restUri";
import { DASHBOARD_ID_KEY, RBAC, TOKEN_KEY, ACCOUNT_ID, PROJECT_IDENTIFIER, ORG_IDENTIFIER, COLLECTION_IDENTIFIER } from "../constants/localStorageKeys";
import AuthService from "../services/authService";
import { getRBACPermission } from "helper/userRolesPermission.helper";
import { PermeableMetrics } from "constants/userRolesPermission.constant";
import { getIsStandaloneApp } from 'helper/helper';

export const restInstance = axios.create({
  baseURL: buildApiUrl(''),
  timeout: 180000,
  headers: {
    "Content-type": "application/json"
  }
});

// TODO remove the authorization header for login call here
const authInstance = axios.create({
  baseURL: buildApiUrl(''),
  timeout: 180000,
  headers: {
    "Content-type": "application/json"
  }
});

restInstance.interceptors.request.use(
  config => {
    let as = new AuthService();
    const isStandaloneApp = getIsStandaloneApp()
    if (isStandaloneApp) {
      if (localStorage.getItem(TOKEN_KEY) === null) {
        as.logout();
        return Promise.reject("No Auth tokens found");
      }
      if (as.isRefreshNeeded()) {
        as.refresh()
          .then(response => {
            let result = as.storeResponse(response);
            if (!result) {
              as.logout();
              return Promise.reject("Invalid token");
            }
            return config;
          })
          .catch(error => {
            as.logout();
            return Promise.reject(error);
          })
          .finally(() => {
            AuthService.refreshPromise = null;
          });
      }
    }
    if (!window.noAuthHeader || isStandaloneApp) {
      // Add the auth header here
      config.headers["Authorization"] = `Bearer ${localStorage.getItem(TOKEN_KEY)}`.toString();
    }

    // adding Dashboard-Id in all dashboard request for non-admin user
    const user = localStorage.getItem(RBAC);
    const dashboardInUrl = window.location.href.split("?")[0].includes("dashboards");
    const storeDashboardId = isStandaloneApp ? getRBACPermission(PermeableMetrics.ADD_DASHBOARD_ID_TO_LOCALSTORAGE): true
    if (
      !!localStorage.getItem(DASHBOARD_ID_KEY) &&
      (storeDashboardId) &&
      dashboardInUrl
    ) {
      config.headers["Dashboard-Id"] = localStorage.getItem(DASHBOARD_ID_KEY);
      if (!isStandaloneApp) {
        config.params = {
          ...config.params,
          insightIdentifier: localStorage.getItem(DASHBOARD_ID_KEY)
        }
      }
    }

    if (!isStandaloneApp) {
      config.params = {
        ...config.params,
        routingId: localStorage.getItem(ACCOUNT_ID),
      }
      const projectIdentifier = localStorage.getItem(PROJECT_IDENTIFIER);
      const orgIdentifier = localStorage.getItem(ORG_IDENTIFIER);
      const collectionIdentifier = localStorage.getItem(COLLECTION_IDENTIFIER)
      if (projectIdentifier) {
        config.params = {
          ...config.params,
          projectIdentifier
        }
      }
      if (orgIdentifier) {
        config.params = {
          ...config.params,
          orgIdentifier
        }
      }
      if (collectionIdentifier) {
        config.params = {
          ...config.params,
          collectionIdentifier
        }
      }
    }


    return config;
  },
  err => {
    //return err;
    return Promise.reject(err);
  }
);

restInstance.interceptors.response.use(
  response => {
    return response;
  },
  error => {
    return Promise.reject(error);
  }
);

authInstance.interceptors.request.use(
  config => {
    if (config.url.toString().includes(REFRESH) && (!window.noAuthHeader || getIsStandaloneApp())) {
      config.headers["Authorization"] = `Bearer ${localStorage.getItem(TOKEN_KEY)}`.toString();
    }
    return config;
  },
  err => {
    return Promise.reject(err);
  }
);

// TODO: A quick hack for now, will setup store and saga to load and display images.
export function fetchProtectedImage(url) {
  return new Promise((resolve, reject) => {
    fetch(url, {
      method: "get",
      headers: new Headers({
        Authorization: window.noAuthHeader || !getIsStandaloneApp() ? '' : `Bearer ${localStorage.getItem(TOKEN_KEY)}`.toString()
      })
    })
      .then(async response => {
        const blob = await response.blob();
        const reader = new FileReader();
        reader.readAsDataURL(blob);
        reader.onloadend = function () {
          resolve(reader.result);
        };
      })
      .catch(error => reject(error));
  });
}

export function makeManyRequets(requests) {
  return restInstance.all(requests);
}

export function restLogin(username, password, customer, otp) {
  return authInstance.post(LOGIN, { username, password, company: customer, otp });
}

export function restRefresh(token) {
  return authInstance.post(REFRESH, { token: token });
}

export function passwordReset(username, company) {
  let postData = {
    username: username,
    company: company
  };
  return authInstance.post(PASSWORD_RESET, postData);
}

export function resetPasswordReq(params) {
  return authInstance.post(PASSWORD_CHANGE, params);
}

export function restValidateEmail(email) {
  return authInstance.get(`${VALIDATE_EMAIL}?email=${email}`);
}

export function restValidateCompany(email, company) {
  return authInstance.get(`${VALIDATE_COMPANY}?email=${email}&company=${company}`);
}
