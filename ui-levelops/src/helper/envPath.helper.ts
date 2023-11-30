import envConfig from "env-config";

const getEnvPath = () => {
  try {
    return window.location.origin;
  } catch (err) {
    // Not able to get from window.location, using env variable as fallback
    return envConfig.get("UI_URL");
  }
};

export const BASE_UI_URL = getEnvPath();
