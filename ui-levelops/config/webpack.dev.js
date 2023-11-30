const path = require("path");
const { merge } = require("webpack-merge");
const MiniCssExtractPlugin = require("mini-css-extract-plugin");
const HTMLWebpackPlugin = require("html-webpack-plugin");
const commonConfig = require("./webpack.common");
const CircularDependencyPlugin = require("circular-dependency-plugin");
const CONTEXT = process.cwd();
const BundleAnalyzerPlugin = require("webpack-bundle-analyzer").BundleAnalyzerPlugin;
const WebpackBar = require("webpackbar");
const ForkTsCheckerWebpackPlugin = require("fork-ts-checker-webpack-plugin");

const devConfig = {
  mode: "development",
  entry: "./src/index.ts",
  devtool: "cheap-module-source-map",
  cache: { type: "memory" },
  output: {
    path: path.resolve(CONTEXT, "dist/static/"),
    filename: "[name].js",
    chunkFilename: "[name].[id].js"
  },
  watchOptions: {
    aggregateTimeout: 500,
    ignored: ["**/strings/*.ts", "**/node_modules"]
  },
  devServer: {
    hot: true,
    host: "localhost",
    port: 3000,
    historyApiFallback: true,
    static: [path.join(process.cwd(), "src"), path.join(process.cwd(), "src/static")]
  },
  plugins: [
    new ForkTsCheckerWebpackPlugin({
      logger: {
        infrastructure: "console",
        logLevel: "silent"
      }
    }),
    new WebpackBar(),
    new BundleAnalyzerPlugin({
      analyzerMode: false
    }),
    // new CircularDependencyPlugin({
    //   exclude: /node_modules/,
    //   failOnError: true
    // }),
    new HTMLWebpackPlugin({
      template: "src/index.html",
      filename: "index.html",
      favicon: "src/static/favicon.png",
      minify: false,
      templateParameters: {
        BUILD_VERSION: process.env.VERSION,
        NODE_ENV: process.env.NODE_ENV
      },
      meta: {
        viewport: "width=device-width, initial-scale=1, shrink-to-fit=no",
        "theme-color": "#000000",
        "insight-app-sec-validation": "09fc28b1-3aa6-4f81-b6bc-34376d9d26dd"
      }
    }),
    new MiniCssExtractPlugin({
      filename: "[name].css",
      chunkFilename: "[name].[id].css",
      ignoreOrder: true
    })
  ]
};

module.exports = merge(commonConfig, devConfig);
