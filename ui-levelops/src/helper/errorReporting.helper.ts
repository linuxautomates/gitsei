import { notification } from "antd";
import { logToBugsnag, BugsnagErrorProps } from "bugsnag";
import { SORRY_THERE_WAS_AN_ERROR } from "constants/formWarnings";

interface HandleErrorObjectProps {
  showNotfication?: boolean;
  message?: string;
  bugsnag?: BugsnagErrorProps;
}

export const handleError = (options: HandleErrorObjectProps) => {
  if (!!options.showNotfication && !!options.message) {
    notification.error({ message: options.message });
  }

  // report to bugsnag
  if (!!options.bugsnag) {
    let _message;
    if (!!options.bugsnag.message) _message = options.bugsnag.message;
    else if (!!options.message) _message = options.message;
    else _message = SORRY_THERE_WAS_AN_ERROR;

    logToBugsnag(_message, options.bugsnag.severity, options.bugsnag.context, options.bugsnag.data);
  }
};
