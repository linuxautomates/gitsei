const path = require("path");

const {
  container: { ModuleFederationPlugin },
  DefinePlugin,
  ProvidePlugin
} = require("webpack");
const { RetryChunkLoadPlugin } = require("webpack-retry-chunk-load-plugin");
const TsconfigPathsPlugin = require("tsconfig-paths-webpack-plugin");
const { GenerateStringTypesPlugin } = require("@harness/uicore/tools/GenerateStringTypesPlugin");

const prettier = require("prettier");
const moduleFederationConfig = require("./moduleFederation.config");

require("dotenv").config({ path: path.resolve(__dirname, "../", ".env") });

const CONTEXT = process.cwd();

let _keysToDisplay = {};
for (let _env of Object.keys(process.env))
  if (_env.includes("REACT_APP_") || _env === "NODE_ENV") _keysToDisplay[_env] = process.env[_env];
console.log(_keysToDisplay);

/**
 * @type {import('webpack').Configuration}
 */
module.exports = {
  target: "web",
  context: CONTEXT,
  stats: {
    modules: false,
    children: false
  },
  output: {
    publicPath: "auto",
    pathinfo: false
  },
  module: {
    rules: [
      {
        test: /\.m?js$/,
        include: /node_modules/,
        type: "javascript/auto",
        resolve: {
          fullySpecified: false
        }
      },
      {
        test: /\.(ts|tsx)$/,
        exclude: [/node_modules/],
        use: [
          {
            loader: "ts-loader",
            options: {
              compilerOptions: {
                noEmit: false,
                transpileOnly: true
              }
            }
          }
        ]
      },
      {
        test: /\.(js|jsx)$/,
        exclude: [/node_modules/],
        use: ["thread-loader", { loader: "babel-loader" }]
      },
      {
        test: /\.(sa|sc|c)ss$/,
        use: ["thread-loader", "style-loader", "css-loader", "sass-loader"]
      },
      {
        test: /\.ya?ml$/,
        type: "json",
        use: [
          {
            loader: "yaml-loader"
          }
        ]
      },
      {
        test: /\.(jpg|jpeg|png|gif)$/,
        type: "asset/resource"
      },
      {
        test: /\.(woff(2)?|eot|ttf|otf)$/,
        type: "asset/inline"
      },
      {
        test: /\.svg$/,
        use: [
          { loader: "babel-loader" },
          {
            loader: "react-svg-loader",
            options: {
              jsx: true,
              svgo: {
                plugins: [
                  {
                    removeViewBox: false
                  }
                ]
              }
            }
          }
        ]
      }
    ]
  },
  plugins: [
    new DefinePlugin({
      "process.env": JSON.stringify({}),
      "process.env.REACT_APP_NODE_ENV": JSON.stringify(process.env.NODE_ENV),
      "process.env.REACT_APP_API_URL": JSON.stringify(process.env.REACT_APP_API_URL),
      "process.env.REACT_APP_API_VERSION": JSON.stringify(process.env.REACT_APP_API_VERSION),
      "process.env.REACT_APP_GITHUB_CLIENT_ID": JSON.stringify(process.env.REACT_APP_GITHUB_CLIENT_ID),
      "process.env.REACT_APP_JIRA_CLIENT_ID": JSON.stringify(process.env.REACT_APP_JIRA_CLIENT_ID),
      "process.env.REACT_APP_SLACK_CLIENT_ID": JSON.stringify(process.env.REACT_APP_SLACK_CLIENT_ID),
      "process.env.REACT_APP_UI_URL": JSON.stringify(process.env.REACT_APP_UI_URL),
      "process.env.REACT_APP_MODE": JSON.stringify(process.env.REACT_APP_MODE),
      "process.env.REACT_APP_DEBUG": JSON.stringify(process.env.REACT_APP_DEBUG),
      "process.env.REACT_APP_TINYMCE_API_KEY": JSON.stringify(process.env.REACT_APP_TINYMCE_API_KEY),
      "process.env.REACT_APP_BUGSNAG_API_KEY": JSON.stringify(process.env.REACT_APP_BUGSNAG_API_KEY),
      "process.env.REACT_APP_ALL_ACCESS_USERS": JSON.stringify(process.env.REACT_APP_ALL_ACCESS_USERS)
    }),
    new ModuleFederationPlugin(moduleFederationConfig),
    new GenerateStringTypesPlugin({
      input: "src/strings/strings.en.yaml",
      output: "src/strings/types.ts",
      preProcess: async content => {
        const prettierConfig = await prettier.resolveConfig(process.cwd());
        return prettier.format(content, { ...prettierConfig, parser: "typescript" });
      }
    }),
    new RetryChunkLoadPlugin({
      maxRetries: 3
    }),
    new ProvidePlugin({
      process: "process/browser",
      Buffer: ["buffer", "Buffer"]
    })
  ],
  resolve: {
    extensions: [".mjs", ".js", ".jsx", ".ts", ".tsx", ".json", ".css", ".scss"],
    alias: {
      process: "process/browser",
      stream: "stream-browserify",
      zlib: "browserify-zlib",
      path: "path-browserify"
    },
    plugins: [
      new TsconfigPathsPlugin({
        configFile: "./tsconfig.json",
        extensions: [".ts", ".js", ".tsx", ".jsx"]
      })
    ],
    fallback: {
      fs: false,
      crypto: false,
      querystring: require.resolve("querystring-es3")
    }
  }
};
