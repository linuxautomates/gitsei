/*
 * For a detailed explanation regarding each configuration property and type check, visit:
 * https://jestjs.io/docs/configuration
 */

export default {
  // Automatically clear mock calls, instances, contexts and results before every test
  clearMocks: true,
  // Indicates whether the coverage information should be collected while executing the test
  collectCoverage: true,
  // The directory where Jest should output its coverage files
  coverageDirectory: "coverage",
  // An array of directory names to be searched recursively up from the requiring module's location
  moduleDirectories: ["node_modules", "src"],
  // An array of file extensions your modules use
  moduleFileExtensions: ["js", "mjs", "cjs", "jsx", "ts", "tsx", "json", "node"],
  // A list of paths to directories that Jest should use to search for files in
  roots: ["<rootDir>"],
  // The test environment that will be used for testing
  testEnvironment: "jsdom",
  // The regexp pattern or array of patterns that Jest uses to detect test files
  testRegex: "(/__tests__/.*|\\.(test|spec))\\.(ts|tsx|js)$",
  // A map from regular expressions to paths to transformers
  // transform: {
  //   "^.+\\.tsx?$": "ts-jest"
  // },
  transform: {
    "^.+\\.(ts|tsx)$": [
      "ts-jest",
      {
        tsconfig: "<rootDir>/tsconfig.json",
        isolatedModules: true,
        diagnostics: false
      }
    ],
    "^.+\\.jsx?$": [
      "ts-jest",
      {
        tsconfig: "<rootDir>/tsconfig.json",
        isolatedModules: true,
        diagnostics: false
      }
    ]
  },
  // An array of regexp pattern strings that are matched against all test paths, matched tests are skipped
  testPathIgnorePatterns: [
    "/node_modules/",
    "<rootDir>/src/configurations/pages/smart-tickets/containers/smart-ticket-edit/tests/helpers.test.ts",
    "<rootDir>/src/core/containers/header/header-helper.test.ts",
    "<rootDir>/src/workitems/pages/helper.test.ts",
    "<rootDir>/src/shared-resources/charts/score-chart/score-chart-helper.test.ts",
    "<rootDir>/src/shared-resources/charts/charts-helper.test.ts",
    "<rootDir>/src/shared-resources/containers/server-paginated-table/tests/helper.test.ts",
    "<rootDir>/src/shared-resources/containers/config-table-widget-wrapper/tests/configWidgets.helper.test.ts",
    "<rootDir>/src/shared-resources/containers/widget-api-wrapper/widget-api-wrapper-helper.test.ts",
    "<rootDir>/src/utils/utils.test.ts",
    "<rootDir>/src/utils/__tests__/stringUtilsTests/joinUrl.test.ts",
    "<rootDir>/src/utils/__tests__/dateUtilsTests/isDisabledDate.test.ts",
    "<rootDir>/src/dashboard/graph-filters/components/helper.test.ts",
    "<rootDir>/src/dashboard/graph-filters/components/utils/tests/getMappedRangeValue.test.ts",
    "<rootDir>/src/dashboard/graph-filters/components/utils/tests/getCreatedAtUpdateAtOptions.test.ts",
    "<rootDir>/src/dashboard/graph-filters/components/utils/tests/createOnRangePickerChange.test.ts",
    "<rootDir>/src/dashboard/pages/dashboard-drill-down-preview/dashboard-drilldown-preview-helper.test.ts",
    "<rootDir>/src/dashboard/helpers/drilldown-transformers/tests/jenkinsDrilldownTransformer.test.ts",
    "<rootDir>/src/dashboard/helpers/drilldown-transformers/tests/mergeCustomHygieneFilters.test.ts",
    "<rootDir>/src/dashboard/helpers/drilldown-transformers/tests/genericDrillDowntransformer.helper.test.ts",
    "<rootDir>/src/dashboard/helpers/dashboards.test.ts",
    "<rootDir>/src/custom-hooks/tests/sankeyHelper.test.ts",
    "<rootDir>/src/custom-hooks/tests/multiplexDataHelper.test.ts",
    "<rootDir>/src/custom-hooks/tests/pagerDutyServicesHelper.test.ts",
    "<rootDir>/src/custom-hooks/helpers/tests/statReport.helper.test.ts",
    "<rootDir>/src/custom-hooks/helpers/tests/trendReport.helper.test.ts",
    "<rootDir>/src/custom-hooks/helpers/custom-hooks.test.ts",
    "<rootDir>/src/configuration-tables/helper.test.ts",
    "<rootDir>/src/workflow/pages/propel/propel-helper.test.ts",
    "<rootDir>/src/configurable-dashboard/dynamic-graph-filter/container/tests/helper.test.ts",
    "<rootDir>/src/configurable-dashboard/helpers/helper.test.ts",
    "<rootDir>/src/configurations/containers/apikeys/apikey-create.test.tsx",
    "<rootDir>/src/configurations/containers/global-settings/state-create.container.test.tsx",
    "<rootDir>/src/configurations/pages/knowledgebase/knowledgebase-create-edit/knowledgebase-create-edit.page.test.tsx",
    "<rootDir>/src/configurations/pages/templates/templates-add/templates-add.page.test.tsx",
    "<rootDir>/src/configurations/pages/assessment-templates/assessment-templates-edit/assessment-template-edit.page.test.tsx",
    "<rootDir>/src/configurations/pages/assessment-templates/assessment-templates-list/assessment-templates-list.test.tsx",
    "<rootDir>/src/products/products-list.page.test.tsx",
    "<rootDir>/src/products/containers/product-mappings/product-mappings.container.test.tsx",
    "<rootDir>/src/smart-tickets/pages/smart-ticket-template/smart-ticket-template-edit.page.test.tsx",
    "<rootDir>/src/smart-tickets/pages/smart-ticket-template/smart-ticket-template-list.page.test.tsx",
    "<rootDir>/src/shared-resources/helpers/select-restapi/tests/helpers.test.tsx",
    "<rootDir>/src/triage/pages/triageDetails.test.tsx",
    "<rootDir>/src/dashboard/tests/dashboard-drilldown/dashboard-drilldown.test.tsx",
    "<rootDir>/src/dashboard/graph-filters/tests/dashboard-graph-filters.test.tsx",
    "<rootDir>/src/dashboard/components/dashboard-application-filters/tests/helper.test.tsx",
    "<rootDir>/src/components/generic-form-elements/input-tags/input-tags.test.tsx",
    "<rootDir>/src/workflow/components/dry-run-modal/tests/node-dry-modal.test.tsx",
    "<rootDir>/src/configurable-dashboard/components/configure-widget-modal/tests/configure-modal.test.tsx",
    "<rootDir>/src/views/Pages/LoginPage.test.tsx",
    "<rootDir>/src/dashboard/reports/jira/issues-report/tests/filter.config.test.ts"
  ],

  // All imported modules in your tests should be mocked automatically
  // automock: false,

  // Stop running tests after `n` failures
  // bail: 0,

  // The directory where Jest should store its cached dependency information
  // cacheDirectory: "/private/var/folders/4r/k6z3l2hs1wlc940z5tjx9z9h0000gn/T/jest_dx",

  // An array of glob patterns indicating a set of files for which coverage information should be collected
  // collectCoverageFrom: undefined,

  // An array of regexp pattern strings used to skip coverage collection
  // coveragePathIgnorePatterns: [
  //   "/node_modules/"
  // ],

  // Indicates which provider should be used to instrument code for coverage
  // coverageProvider: "babel",

  // A list of reporter names that Jest uses when writing coverage reports
  // coverageReporters: [
  //   "json",
  //   "text",
  //   "lcov",
  //   "clover"
  // ],

  // An object that configures minimum threshold enforcement for coverage results
  // coverageThreshold: undefined,

  // A path to a custom dependency extractor
  // dependencyExtractor: undefined,

  // Make calling deprecated APIs throw helpful error messages
  // errorOnDeprecated: false,

  // The default configuration for fake timers
  // fakeTimers: {
  //   "enableGlobally": false
  // },

  // Force coverage collection from ignored files using an array of glob patterns
  // forceCoverageMatch: [],

  // A path to a module which exports an async function that is triggered once before all test suites
  // globalSetup: undefined,

  // A path to a module which exports an async function that is triggered once after all test suites
  // globalTeardown: undefined,

  // A set of global variables that need to be available in all test environments
  // globals: {},

  // The maximum amount of workers used to run your tests. Can be specified as % or a number. E.g. maxWorkers: 10% will use 10% of your CPU amount + 1 as the maximum worker number. maxWorkers: 2 will use a maximum of 2 workers.
  // maxWorkers: "50%",

  // A map from regular expressions to module names or to arrays of module names that allow to stub out resources with a single module
  moduleNameMapper: {
    "\\.s?css$": "identity-obj-proxy",
    "\\.(jpg|jpeg|png|gif|svg|eot|otf|webp|ttf|woff|woff2|mp4|webm|wav|mp3|m4a|aac|oga)$":
      "<rootDir>/jestMediaFileTransformer.js"
  },

  // An array of regexp pattern strings, matched against all module paths before considered 'visible' to the module loader
  // modulePathIgnorePatterns: [],

  // Activates notifications for test results
  // notify: false,

  // An enum that specifies notification mode. Requires { notify: true }
  // notifyMode: "failure-change",

  // A preset that is used as a base for Jest's configuration
  // preset: undefined,

  // Run tests from one or more projects
  // projects: undefined,

  // Use this configuration option to add custom reporters to Jest
  // reporters: undefined,

  // Automatically reset mock state before every test
  // resetMocks: false,

  // Reset the module registry before running each individual test
  // resetModules: false,

  // A path to a custom resolver
  // resolver: undefined,

  // Automatically restore mock state and implementation before every test
  // restoreMocks: false,

  // The root directory that Jest should scan for tests and modules within
  // rootDir: undefined,

  modulePaths: ["<rootDir>/src/"],

  // Allows you to use a custom runner instead of Jest's default test runner
  // runner: "jest-runner",

  // The paths to modules that run some code to configure or set up the testing environment before each test
  // setupFiles: [],

  // A list of paths to modules that run some code to configure or set up the testing framework before each test
  // setupFilesAfterEnv: [],

  // The number of seconds after which a test is considered as slow and reported as such in the results.
  // slowTestThreshold: 5,

  // A list of paths to snapshot serializer modules Jest should use for snapshot testing
  // snapshotSerializers: [],

  // Options that will be passed to the testEnvironment
  // testEnvironmentOptions: {},

  // Adds a location field to test results
  // testLocationInResults: false,

  // The glob patterns Jest uses to detect test files
  // testMatch: [
  //   "**/__tests__/**/*.[jt]s?(x)",
  //   "**/?(*.)+(spec|test).[tj]s?(x)"
  // ],

  // This option allows the use of a custom results processor
  // testResultsProcessor: undefined,

  // This option allows use of a custom test runner
  // testRunner: "jest-circus/runner",

  // An array of regexp pattern strings that are matched against all source file paths, matched files will skip transformation
  // transformIgnorePatterns: [
  //   "/node_modules/",
  //   "\\.pnp\\.[^\\/]+$"
  // ],
  transformIgnorePatterns: ["<rootDir>/node_modules/?!(lodash-es|@harness)"]

  // An array of regexp pattern strings that are matched against all modules before the module loader will automatically return a mock for them
  // unmockedModulePathPatterns: undefined,

  // Indicates whether each individual test should be reported during the run
  // verbose: undefined,

  // An array of regexp patterns that are matched against all source file paths before re-running tests in watch mode
  // watchPathIgnorePatterns: [],

  // Whether to use watchman for file crawling
  // watchman: true,
};
