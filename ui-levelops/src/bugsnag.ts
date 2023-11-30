import Bugsnag from "@bugsnag/js";
import { Client } from "@bugsnag/browser";
import BugsnagPluginReact from "@bugsnag/plugin-react";
import React from "react";
import { SORRY_THERE_WAS_AN_ERROR } from "constants/formWarnings";
import { hashCode } from "utils/stringUtils";
import { isString } from "lodash";
import LocalStoreService from "services/localStoreService";
import envConfig from "env-config";

export interface BugsnagErrorProps {
  message?: string;
  severity: severityTypes;
  context: issueContextTypes;
  data: any;
}

export enum severityTypes {
  INFO = "info",
  WARNING = "warning",
  ERROR = "error"
}

export enum issueContextTypes {
  BROWSERJS = "browserjs",
  UNKNOWN = "unknown",
  GENERAL = "general",
  AUTHENTICATION = "auth",
  MFA_ENROLL = "mfa_enroll",
  APIS = "api",
  WIDGETS = "widgets",
  FILES = "files",
  INTEGRATIONS = "integrations",
  KNOWLEDGEBASE = "knowledgebase",
  PROPELS = "propels",
  WORKITEM_ISSUES = "workitem_issues",
  WORKSPACE = "project",
  ORG_UNITS = "org_units",
  EFFORT_INVESTMENT = "effort_investment",
  TRELLIS = "trellis",
  VELOCITY = "velocity",
  MISC = "misc",
  JAVASCRIPT = "js",
  TRELLIS_OU = "trellis_ou"
}

const bugsnagApiKey: string = envConfig.get("BUGSNAG_API_KEY") || "BUGSNAG_API_NOT_SET";

export const IGNORE_EVENTS_MESSAGES = ["ResizeObserver loop limit exceeded", "No Auth tokens found"];

export const bugsnagClient: Client = Bugsnag.start({
  apiKey: bugsnagApiKey,
  plugins: [new BugsnagPluginReact(React)],
  appVersion: (window as any).__BUILD_VERSION__ || "unknown",
  onError: function (event) {
    if (IGNORE_EVENTS_MESSAGES.includes(event.errors[0].errorMessage)) {
      return false;
    }

    const ls = new LocalStoreService();
    const errorRef = hashCode(
      [
        event.errors[0].errorMessage,
        severityTypes.ERROR,
        issueContextTypes.JAVASCRIPT,
        ls.getUserId(),
        ls.getUserCompany()
      ].join(":")
    );
    event.errors[0].errorClass = `${event.errors[0].type} ${severityTypes.ERROR} (ref: ${errorRef})`;
    event.context = issueContextTypes.JAVASCRIPT;

    // get information in the console for CS team to help trace
    console.info(`Bugsnag ref: ${errorRef}`, {
      message: event.errors[0].errorMessage,
      context: issueContextTypes.JAVASCRIPT
    });
  },
  redactedKeys: ["Authorization", /^password$/i]
});

export const notifyRestAPIError = (e: any, action: Object, response: any) => {
  if (bugsnagClient) {
    if (isString(e) && IGNORE_EVENTS_MESSAGES.includes(e)) {
      return false;
    }

    logToBugsnag(e?.message, severityTypes.ERROR, response.status, { e, action, response }, "API ERROR");
  }
};

/**
 * Used for manully logging messages/data to bugsnag;
 */
export const logToBugsnag = (
  message: string,
  severityType: severityTypes,
  context: issueContextTypes,
  data?: any,
  errorClass?: string
) => {
  if (bugsnagClient) {
    const ls = new LocalStoreService();
    const errorRef = hashCode([message, severityType, context, ls.getUserId(), ls.getUserCompany()].join(":"));
    bugsnagClient.notify(new Error(message), function (event) {
      event.errors[0].errorClass = `${
        !!errorClass ? errorClass : event.errors[0].type
      } ${severityType} (ref: ${errorRef})`;

      if (!message) message = SORRY_THERE_WAS_AN_ERROR;

      event.errors[0].errorMessage = message;
      event.context = context;
      event.severity = severityType;
      event.unhandled = false;
      event.addMetadata("data", sanitizeSensitiveDataForBugsnag(data || {}));
    });
  }
};

export const sanitizeSensitiveDataForBugsnag = (data: any) => {
  if (data?.e?.config) {
    if (data.e.config?.data) {
      let toSanitized = JSON.parse(data.e.config.data || "");
      toSanitized.password = "**********";
      data.e.config.data = toSanitized;
    }
    if (data.e.config.headers?.Authorization) {
      data.e.config.headers.Authorization = "**********";
    }
  }
  return data;
};
