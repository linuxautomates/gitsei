import { issueContextTypes, logToBugsnag, severityTypes } from "bugsnag";
import { handleError } from "helper/errorReporting.helper";
import { lazy } from "react";
import { CHUNK_RETRY_ERROR } from "./../../constants/error.constants";

const lazyWithRetry = (componentImport, moduleName) =>
  lazy(async () => {
    const storageKey = `page-has-been-force-refreshed_${moduleName}`;
    const pageHasAlreadyBeenForceRefreshed = JSON.parse(window.localStorage.getItem(storageKey) || "false");

    try {
      const component = await componentImport();

      window.localStorage.setItem(storageKey, "false");

      return component;
    } catch (error) {
      handleError({
        bugsnag: {
          message: error?.message,
          severity: severityTypes.ERROR,
          context: issueContextTypes.GENERAL,
          data: { error }
        }
      });

      if (!pageHasAlreadyBeenForceRefreshed) {
        // Assuming that the user is not on the latest version of the application.
        // Let's refresh the page immediately.
        window.localStorage.setItem(storageKey, "true");
        window.location.reload();
        // setting flag for fallback component
        const _error = new Error(CHUNK_RETRY_ERROR);
        _error.retryLoading = true;
        throw _error;
      }

      throw error;
    }
  });

export default lazyWithRetry;
