const pick = require("lodash/pick");
const mapValues = require("lodash/mapValues");
const deps = require("../package.json").dependencies;

/**
 * These packages must be strictly shared with exact versions
 */
const singletonDeps = [ 
  'react-dom',
  'react',
  '@harness/use-modal',
  '@blueprintjs/core',
  '@blueprintjs/select',
  '@blueprintjs/datetime',
  'react-router-dom',
];

/**
 * @type {import('webpack').ModuleFederationPluginOptions}
 */
module.exports = {
  name: "sei",
  filename: "remoteEntry.js",
  exposes: {
    "./MicroFrontendApp": "./src/App/App",
    "./CollectionResourceModalBody": './src/pages/CollectionResourceModalBody/CollectionResourceModalBody',
    "./CollectionResourcesRenderer": './src/pages/CollectionResourcesRenderer/CollectionResourcesRenderer',
    "./InsightsResourceModalBody": './src/pages/InsightsResourceModalBody/InsightsResourceModalBody',
    "./InsightsResourceRenderer": './src/pages/InsightsResourceRenderer/InsightsResourceRenderer'
  },
  shared: {
    ...deps,
    ...mapValues(pick(deps, singletonDeps), version => ({
      singleton: true,
      requiredVersion: version
    }))
  }
};
