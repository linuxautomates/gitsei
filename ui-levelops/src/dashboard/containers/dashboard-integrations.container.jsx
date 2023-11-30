import React from "react";
import { connect } from "react-redux";
import { mapRestapiDispatchtoProps, mapRestapiStatetoProps } from "reduxConfigs/maps/restapiMap";
import { getData, getError, getLoading } from "utils/loadingUtils";
import Loader from "components/Loader/Loader";
import { AntCol, AntRow, AntTitle } from "shared-resources/components";
import { SelectRestapi } from "shared-resources/helpers";
import { PraetorianGraph, SnykTimeSeries, ThreatModelGraph, Tenable } from "dashboard/components";

const SUPPORTED_INTEGRATIONS = ["SNYK", "TENABLE"];
const SUPPORTED_PLUGINS = ["report_praetorian", "report_ms_tmt"];

export class DashboardIntegrationsContainer extends React.Component {
  constructor(props) {
    super(props);
    const productAggsLoading = SUPPORTED_INTEGRATIONS.reduce((map, curr) => {
      map[curr] = { loading: false, id: undefined, detail_loading: false };
      return map;
    }, {});
    const pluginResultsLoading = SUPPORTED_PLUGINS.reduce((map, curr) => {
      map[curr] = { loading: false, id: undefined, detail_loading: false };
      return map;
    }, {});
    this.state = {
      product_aggs_loading: productAggsLoading,
      plugin_results_loading: pluginResultsLoading,
      products_loading: true,
      plugins_loading: true,
      selected_product: { key: "All", label: "All Products" },
      integrations: [],
      plugins: []
    };
    this.onChangeProduct = this.onChangeProduct.bind(this);
    this.fetchResults = this.fetchResults.bind(this);
  }

  componentDidMount() {
    this.props.productsList();
    this.props.pluginsList();
  }

  componentWillUnmount() {
    this.props.restapiClear("products", "list", "0");
    this.props.restapiClear("plugin_results", "list", "-1");
    this.props.restapiClear("product_aggs", "list", "-1");
    this.props.restapiClear("plugins", "list", "0");
  }

  static getDerivedStateFromProps(props, state) {
    if (state.products_loading || state.plugins_loading) {
      // let selectedProduct = {};
      let productAggsLoading = SUPPORTED_INTEGRATIONS.reduce((map, curr) => {
        map[curr] = { loading: false, id: undefined, detail_loading: false };
        return map;
      }, {});
      let pluginResultsLoading = SUPPORTED_PLUGINS.reduce((map, curr) => {
        map[curr] = { loading: false, id: undefined, detail_loading: false };
        return map;
      }, {});

      const productLoading = getLoading(props.rest_api, "products", "list", "0");
      const pluginLoading = getLoading(props.rest_api, "plugins", "list", "0");
      const productError = getError(props.rest_api, "products", "list", "0");
      const pluginError = getError(props.rest_api, "plugins", "list", "0");

      if (!(productLoading || pluginLoading) && !(pluginError || productError)) {
        const products = getData(props.rest_api, "products", "list", "0").records;
        const plugins = getData(props.rest_api, "plugins", "list", "0").records;

        if (products.length) {
          // initially set the first product as selected so that we render something
          // what if we want all products
          // selectedProduct = {
          //   key: products[0].id,
          //   label: products[0].name
          // };

          // get all synk product latest aggs sorted by created_at for selected product
          SUPPORTED_INTEGRATIONS.forEach(integration => {
            productAggsLoading[integration] = { loading: true, id: undefined, detail_loading: false };
            props.productAggsList(
              {
                page_size: 1,
                filter: {
                  //product_id: products[0].id,
                  integration_types: [integration]
                },
                sort: [
                  {
                    id: "created_at",
                    desc: true
                  }
                ]
              },
              integration
            );
          });

          // get all tmt, prateroian plugin results sorted by created_at for selected product
          SUPPORTED_PLUGINS.forEach(plugin => {
            pluginResultsLoading[plugin] = { loading: true, id: undefined, detail_loading: false };
            const pluginIds = plugins.filter(plug => plug.tool === plugin);
            props.pluginResultsList(
              {
                filter: {
                  //product_ids: [products[0].id],
                  product_ids: [],
                  ids: pluginIds.map(p => p.id),
                  successful: true
                },
                sort: [
                  {
                    id: "created_at",
                    desc: true
                  }
                ]
              },
              plugin
            );
          });
        }
      }
      return {
        ...state,
        products_loading: productLoading,
        plugins_loading: pluginLoading,
        //selected_product: selectedProduct,
        product_aggs_loading: { ...productAggsLoading },
        plugin_results_loading: { ...pluginResultsLoading }
      };
    }

    let productsLoading = { ...state.product_aggs_loading };
    let pluginsLoading = { ...state.plugin_results_loading };

    const aggsListLoading = Object.keys(productsLoading).reduce((acc, curr) => {
      return acc || state.product_aggs_loading[curr].loading;
    }, false);
    const pluginsListLoading = Object.keys(pluginsLoading).reduce((acc, curr) => {
      return acc || state.plugin_results_loading[curr].loading;
    }, false);

    if (aggsListLoading) {
      SUPPORTED_INTEGRATIONS.forEach(integration => {
        const loading = getLoading(props.rest_api, "product_aggs", "list", integration);
        const error = getError(props.rest_api, "product_aggs", "list", integration);
        if (!loading && !error) {
          productsLoading[integration].loading = false;
          const data = getData(props.rest_api, "product_aggs", "list", integration);
          if (data.records.length > 0) {
            productsLoading[integration].id = data.records[0].id;
            productsLoading[integration].detail_loading = true;
            props.productAggsGet(data.records[0].id);
          } else {
            productsLoading[integration].detail_loading = false;
          }
          props.restapiClear("product_aggs", "list", "-1");
        }
      });
    }

    if (pluginsListLoading) {
      SUPPORTED_PLUGINS.forEach(plugin => {
        const loading = getLoading(props.rest_api, "plugin_results", "list", plugin);
        const error = getError(props.rest_api, "plugin_results", "list", plugin);
        if (!loading && !error) {
          pluginsLoading[plugin].loading = false;
          const data = getData(props.rest_api, "plugin_results", "list", plugin);
          if (data.records.length > 0) {
            pluginsLoading[plugin].id = data.records[0].id;
            pluginsLoading[plugin].detail_loading = true;
            props.pluginResultsGet(data.records[0].id);
          } else {
            pluginsLoading[plugin].detail_loading = false;
          }
          props.restapiClear("plugin_results", "list", "-1");
        }
      });
    }

    const productDetailsLoading = Object.keys(productsLoading).reduce((acc, curr) => {
      return acc || state.product_aggs_loading[curr].detail_loading;
    }, false);
    const pluginsDetailsLoading = Object.keys(pluginsLoading).reduce((acc, curr) => {
      return acc || state.plugin_results_loading[curr].detail_loading;
    }, false);

    if (productDetailsLoading) {
      SUPPORTED_INTEGRATIONS.forEach(integration => {
        const loading = getLoading(props.rest_api, "product_aggs", "get", productsLoading[integration].id);
        const error = getError(props.rest_api, "product_aggs", "get", productsLoading[integration].id);
        if (!loading && !error) {
          productsLoading[integration].detail_loading = false;
        }
      });
    }

    if (pluginsDetailsLoading) {
      SUPPORTED_PLUGINS.forEach(plugin => {
        const loading = getLoading(props.rest_api, "plugin_results", "get", pluginsLoading[plugin].id);
        const error = getError(props.rest_api, "plugin_results", "get", pluginsLoading[plugin].id);
        if (!loading && !error) {
          pluginsLoading[plugin].detail_loading = false;
        }
      });
    }

    return {
      ...state,
      plugin_results_loading: pluginsLoading,
      product_aggs_loading: productsLoading
    };
  }

  onChangeProduct(value) {
    if (value === undefined) {
      // do nothing
    } else {
      this.setState(
        {
          selected_product: value,
          product_aggs_loading: SUPPORTED_INTEGRATIONS.reduce((map, curr) => {
            map[curr] = { loading: true, id: undefined, detail_loading: false };
            return map;
          }, {}),
          plugin_results_loading: SUPPORTED_PLUGINS.reduce((map, curr) => {
            map[curr] = { loading: true, id: undefined, detail_loading: false };
            return map;
          }, {})
        },
        () => this.fetchResults()
      );
    }
  }

  fetchResults() {
    const plugins = getData(this.props.rest_api, "plugins", "list", "0").records;

    SUPPORTED_INTEGRATIONS.forEach(integration => {
      let filters = {
        integration_types: [integration]
      };

      if (this.state.selected_product.key !== "All") {
        filters = {
          ...filters,
          product_id: this.state.selected_product.key
        };
      }
      this.props.productAggsList(
        {
          filter: {
            ...filters
          },
          sort: [
            {
              id: "created_at",
              desc: true
            }
          ]
        },
        integration
      );
    });

    SUPPORTED_PLUGINS.forEach(plugin => {
      const pluginIds = plugins.filter(plug => plug.tool === plugin);
      this.props.pluginResultsList(
        {
          filter: {
            product_ids: this.state.selected_product.key !== "All" ? [this.state.selected_product.key] : [],
            ids: pluginIds.map(p => p.id),
            successful: true
          },
          sort: [
            {
              id: "created_at",
              desc: true
            }
          ]
        },
        plugin
      );
    });
  }

  get header() {
    return (
      <AntRow type={"flex"} justify={"space-between"}>
        <AntCol>
          <AntTitle level={4}>Integrations Summary</AntTitle>
        </AntCol>
        <AntCol span={6}>
          <SelectRestapi
            style={{ width: "100%" }}
            mode="single"
            showSearch
            filterOption
            showArrow
            placeholder="All Products"
            value={this.state.selected_product}
            onChange={this.onChangeProduct}
            //rest_api={this.props.rest_api}
            uri="products"
            fetchData={this.props.productsList}
            labelInValue={true}
            allowClear={false}
            additionalOptions={[{ id: "All", name: "All Products", placement: "start" }]}
          />
        </AntCol>
      </AntRow>
    );
  }

  render() {
    if (this.state.products_loading || this.state.plugins_loading) {
      return <Loader />;
    }

    const tmtReportId = this.state.plugin_results_loading.report_ms_tmt.id;
    const praetorianReportId = this.state.plugin_results_loading.report_praetorian.id;
    const snykId = this.state.product_aggs_loading.SNYK.id;
    const tenableId = this.state.product_aggs_loading.TENABLE.id;

    const tmtReport = getData(this.props.rest_api, "plugin_results", "get", tmtReportId);
    const praetorianReport = getData(this.props.rest_api, "plugin_results", "get", praetorianReportId);
    const snykReport = getData(this.props.rest_api, "product_aggs", "get", snykId);
    const tenableReport = getData(this.props.rest_api, "product_aggs", "get", tenableId);

    return (
      <div style={{ minHeight: "500px" }}>
        <AntRow gutter={[10, 10]}>
          {this.header}
          {tmtReportId !== undefined && !this.state.plugin_results_loading.report_ms_tmt.detail_loading && (
            <AntCol span={24}>
              <ThreatModelGraph report={tmtReport} product={this.state.selected_product.label} />
            </AntCol>
          )}
          {praetorianReportId !== undefined && !this.state.plugin_results_loading.report_praetorian.detail_loading && (
            <AntCol span={24}>
              <PraetorianGraph report={praetorianReport} product={this.state.selected_product.label} />
            </AntCol>
          )}
          {snykId !== undefined && !this.state.product_aggs_loading.SNYK.detail_loading && (
            <AntCol span={24}>
              <SnykTimeSeries report={snykReport} product={this.state.selected_product.label} />
            </AntCol>
          )}
          {tenableId !== undefined && !this.state.product_aggs_loading.TENABLE.detail_loading && (
            <AntCol span={24}>
              <Tenable report={tenableReport} product={this.state.selected_product.label} />
            </AntCol>
          )}
        </AntRow>
      </div>
    );
  }
}

export default connect(mapRestapiStatetoProps, mapRestapiDispatchtoProps)(DashboardIntegrationsContainer);
