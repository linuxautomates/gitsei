const path = require("path");

const { merge } = require("webpack-merge");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const CircularDependencyPlugin = require("circular-dependency-plugin");
const HTMLWebpackPlugin = require("html-webpack-plugin");
const JSONGeneratorPlugin = require("@harness/jarvis/lib/webpack/json-generator-plugin").default;
const { CleanWebpackPlugin } = require("clean-webpack-plugin");

const commonConfig = require("./webpack.common");
const CONTEXT = process.cwd();

const prodConfig = {
  mode: "production",
  devtool: "source-map",
  output: {
    path: path.resolve(CONTEXT, "dist/"),
    filename: "[name].[contenthash:6].js",
    chunkFilename: "[name].[id].[contenthash:6].js",
    pathinfo: false,
    assetModuleFilename: "images/[hash:7][ext][query]"
  },
  plugins: [
    ,
    new CleanWebpackPlugin(), // Clean the 'dist' folder before each build
    // new CircularDependencyPlugin({
    //   exclude: /node_modules/,
    //   failOnError: true
    // }),
    new MiniCssExtractPlugin({
      filename: "[name].[contenthash:6].css",
      chunkFilename: "[name].[id].[contenthash:6].css"
    }),
    new JSONGeneratorPlugin({
      content: {
        version: require("../package.json").version,
        gitCommit: process.env.GIT_COMMIT,
        gitBranch: process.env.GIT_BRANCH
      },
      filename: "version.json"
    }),
    new HTMLWebpackPlugin({
      template: "src/index.html",
      filename: "index.html",
      minify: false,
      favicon: "src/static/favicon.png",
      templateParameters: {
        BUILD_VERSION: process.env.VERSION,
        NODE_ENV: process.env.NODE_ENV
      },
      meta: {
        viewport: "width=device-width, initial-scale=1, shrink-to-fit=no",
        "theme-color": "#000000",
        "insight-app-sec-validation": "09fc28b1-3aa6-4f81-b6bc-34376d9d26dd"
      }
    })
  ]
};

module.exports = merge(commonConfig, prodConfig);
