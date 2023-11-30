import React from "react";
import * as PropTypes from "prop-types";
import { connect } from "react-redux";
import { mapRestapiStatetoProps, mapRestapiDispatchtoProps } from "reduxConfigs/maps/restapiMap";
import { mapPaginationDispatchtoProps } from "reduxConfigs/maps/paginationMap";
import { Drawer, Typography, Input, List, Icon, Tabs, Popconfirm, Collapse } from "antd";
import { get, debounce, union } from "lodash";
import { IntegrationIcon } from "shared-resources/components";
import { ICON_PATH } from "shared-resources/components/integration-icons/icon-path.config";
import { getError, getLoading, getData } from "utils/loadingUtils";
import Loader from "../../../components/Loader/Loader";

const { Text } = Typography;
const { Search } = Input;
const { TabPane } = Tabs;
const { Panel } = Collapse;

export class GenericNewNodeComponent extends React.PureComponent {
  constructor(props) {
    super(props);
    this.state = {
      nodes_loading: false,
      triggers_loading: false,
      categories_loading: false,
      schema_loading: false,
      search_by_name: "",
      active_keys: [],
      add_node_type: undefined,
      content_schema_loading: false
    };
    this.confirmedAdd = this.confirmedAdd.bind(this);
    this.loadNodesByCategory = this.loadNodesByCategory.bind(this);
    this.debouncedLoadNodesByCategory = debounce(this.loadNodesByCategory, 500);
  }

  componentDidMount() {
    this.setState(
      {
        categories_loading: true,
        nodes_loading: false,
        triggers_loading: true,
        search_by_name: "",
        content_schema_loading: false,
        add_node_type: undefined
      },
      () => {
        //this.props.propelNodeCategoriesGet();
        //this.props.propelNodeTemplatesList({});
        //this.props.paginationGet("propel_trigger_templates", "list", {}, "0");
        //this.props.propelTriggerTemplatesList({});
      }
    );
  }

  get categories() {
    const { rest_api } = this.props;
    const loading = getLoading(rest_api, "propel_node_categories", "get", "list");
    const error = getError(rest_api, "propel_node_categories", "get", "list");
    if (!loading && !error) {
      const records = getData(rest_api, "propel_node_categories", "get", "list").records || [];
      return records;
    }
    return [];
  }

  get nodes() {
    if (this.state.active_keys.length === 0 && this.state.search_by_name === "") {
      return [];
    }
    const { rest_api } = this.props;
    const loading = getLoading(rest_api, "propel_node_templates", "list", "0");
    const error = getError(rest_api, "propel_node_templates", "list", "0");
    if (!loading && !error) {
      const records = getData(rest_api, "propel_node_templates", "list", "0").records || [];
      return records;
    }
    return [];
  }

  get triggers() {
    const { rest_api } = this.props;
    const loading = getLoading(rest_api, "propel_trigger_templates", "list", "0");
    const error = getError(rest_api, "propel_trigger_templates", "list", "0");
    if (!loading && !error) {
      const triggerRecords = getData(rest_api, "propel_trigger_templates", "list", "0").records || [];
      return triggerRecords;
    }
    return [];
  }

  getContentSchema(type) {
    return getData(this.props.rest_api, "content_schema", "list", type);
  }

  buildCard(node) {
    const integrationIcons = ICON_PATH.map(icon => icon.temp_props.type);
    const { onAdd } = this.props;
    const icon = get(node, ["ui_data", "icon"], "play-circle");
    return (
      <>
        <List.Item id={node.id} onClick={e => onAdd(node.type)}>
          {integrationIcons.includes(icon) && (
            <div
              style={{
                borderRight: "1px solid #dcdfe4",
                padding: "10px"
              }}>
              <IntegrationIcon type={icon} size={"small"} />
            </div>
          )}
          {!integrationIcons.includes(icon) && <Icon type={icon} />}
          <Text level={4}>{node.name}</Text>
        </List.Item>
      </>
    );
  }

  confirmedAdd(type) {}

  loadNodesByCategory(categories) {
    const filter = {
      filter: {
        hidden: false,
        categories: categories,
        partial: {
          name: this.state.search_by_name
        }
      }
    };
    // TODO: use paginationGet instead of node templates list to pre-populate the content schema
    this.props.paginationGet("propel_node_templates", "list", filter, "0");
    //this.props.propelNodeTemplatesList(filter);
  }

  render() {
    const { onClose, visible, onAdd, onAddTrigger } = this.props;
    const buttonStyle = {
      border: 0,
      background: "transparent",
      boxShadow: "none",
      animationDuration: "0s"
    };

    const nodesLoading = getLoading(this.props.rest_api, "propel_node_templates", "list", "0");

    const activeCategories = this.nodes.reduce((acc, node) => {
      if (!acc.includes(node.category)) {
        acc.push(node.category);
        return acc;
      }
      return acc;
    }, []);

    const integrationIcons = ICON_PATH.map(icon => icon.temp_props.type);
    return (
      <Drawer
        width={500}
        title={"Nodes and Triggers"}
        placement={"right"}
        closable={true}
        onClose={onClose}
        visible={visible}
        getContainer={false}>
        <Tabs size={"small"}>
          <TabPane key={"nodes"} tab={"Nodes"}>
            <Search
              onSearch={value => {
                this.setState(
                  {
                    search_by_name: value
                  }
                  //() => this.loadNodesByCategory([])
                );
              }}
            />

            <Collapse
              activeKey={
                this.state.search_by_name !== ""
                  ? union(this.state.active_keys, activeCategories)
                  : this.state.active_keys
              }
              onChange={keys => {
                this.setState({
                  active_keys: keys
                });
                //this.loadNodesByCategory(keys);
              }}
              bordered={false}
              style={{ backgroundColor: "transparent", padding: "0px", margin: "0px" }}
              //className={`custom-collapse`}
            >
              {this.categories.map(category => (
                <Panel key={category} header={<Text strong>{category}</Text>} style={{ border: "0px" }}>
                  {nodesLoading && <Loader />}
                  {!nodesLoading && this.nodes.filter(node => node.category === category).length > 0 && (
                    <div
                    //className="new-node"
                    >
                      <List
                        className="new-node-list new-node-list__actions"
                        bordered={false}
                        style={{ margin: "0px", padding: "0px" }}>
                        {this.nodes
                          .filter(node => node.category === category)
                          .filter(node => node.name.includes(this.state.search_by_name))
                          .map(node => {
                            const icon = get(node, ["ui_data", "icon"], "play-circle");
                            return (
                              <>
                                {/* TODO: make sure content schema is loaded here */}
                                <List.Item
                                  id={node.id}
                                  onClick={e => onAdd(node.type)}
                                  //onClick={e => this.handleAddNodeWithSchema(node.type)}
                                >
                                  {integrationIcons.includes(icon) && (
                                    <div
                                      style={{
                                        borderRight: "1px solid #dcdfe4",
                                        padding: "10px",
                                        paddingRight: "15px"
                                      }}>
                                      <IntegrationIcon type={icon} size={"small"} />
                                    </div>
                                  )}
                                  {!integrationIcons.includes(icon) && <Icon type={icon} />}
                                  <Text level={4}>{node.name}</Text>
                                </List.Item>
                              </>
                            );
                          })}
                      </List>
                    </div>
                  )}
                </Panel>
              ))}
            </Collapse>
          </TabPane>
          <TabPane key={"triggers"} tab={"Triggers"}>
            <div className="new-node">
              <List className="new-node-list new-node-list__actions" bordered={false}>
                {this.triggers.map((trigger, index) => {
                  const icon = get(trigger, ["ui_data", "icon"], "play-circle");
                  return (
                    <Popconfirm
                      title="You will lose current trigger configurations. Continue?"
                      onConfirm={e => onAddTrigger(trigger.type)}
                      okText="Yes"
                      cancelText="No">
                      <List.Item
                        id={index}
                        //onClick={e => onAddTrigger(trigger.type)}
                      >
                        {integrationIcons.includes(icon) && (
                          <div
                            style={{
                              borderRight: "1px solid #dcdfe4",
                              padding: "10px"
                            }}>
                            <IntegrationIcon type={icon} size={"small"} />
                          </div>
                        )}
                        {!integrationIcons.includes(icon) && <Icon type={icon} />}
                        <Text level={4}>{trigger.display_name}</Text>
                      </List.Item>
                    </Popconfirm>
                  );
                })}
              </List>
            </div>
          </TabPane>
        </Tabs>
        {/*<PaginatedGrid*/}
        {/*    itemsPerRow={1}*/}
        {/*    uri={"propel_node_templates"}*/}
        {/*    method={"list"}*/}
        {/*    mapData={this.buildCard}*/}
        {/*    pageSize={100}*/}
        {/*    showPagination={false}*/}
        {/*/>*/}
      </Drawer>
    );
  }
}

GenericNewNodeComponent.propTypes = {
  className: PropTypes.string.isRequired,
  visible: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
  onAdd: PropTypes.func.isRequired,
  nodes: PropTypes.array,
  onAddTrigger: PropTypes.func
};

GenericNewNodeComponent.defaultProps = {
  className: "new-node-drawer",
  visible: false,
  onAdd: e => {
    console.log(e.currentTarget.id);
  }
};

const dispatchToProps = dispatch => ({
  ...mapRestapiDispatchtoProps(dispatch),
  ...mapPaginationDispatchtoProps(dispatch)
});

export default connect(mapRestapiStatetoProps, dispatchToProps)(GenericNewNodeComponent);
