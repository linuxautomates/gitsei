import "@testing-library/jest-dom";
import { TextEncoder } from "util";

// fix for the issue https://github.com/bugsnag/bugsnag-js/issues/452
setTimeout(function () {}).__proto__.unref = function () {};
setInterval(function () {}).__proto__.unref = function () {};

window._env_ = {
  //SKIP_PREFLIGHT_CHECK: "true",
  REACT_APP_API_URL: "http://localhost:8080",
  REACT_APP_API_VERSION: "v1",
  //REACT_APP_API_URL: "https://api.levelops.io",
  REACT_APP_GITHUB_CLIENT_ID: "8b6170c369e4c4ed514c",
  REACT_APP_JIRA_CLIENT_ID: "v7I4sDQpWLflb1t1ubtVZBAiQD49C5aC",
  REACT_APP_SLACK_CLIENT_ID: "717952128145.742811108896",
  REACT_APP_UI_URL: "http://localhost:3000"
};

global.TextEncoder = TextEncoder;
