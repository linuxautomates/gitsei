import { Card, Icon, Spin, Tree } from "antd";
import { orgUnitJSONType, OUEdgeConfig } from "configurations/configuration-types/OUTypes";
import { clone, cloneDeep, debounce, get } from "lodash";
import cx from "classnames";
import React, { ReactNode, useCallback, useEffect, useMemo, useState } from "react";
import { useDispatch } from "react-redux";
import { genericList } from "reduxConfigs/actions/restapi";
import { getGenericUUIDSelector, useParamSelector } from "reduxConfigs/selectors/genericRestApiSelector";
import { AntIcon } from "shared-resources/components";
import { getParentId, createDataTree, getAncestor, dashboardLevelSearch } from "./helper";
import "./orgUnitTreeViewComponent.styles.scss";
import { ORG_CATEGORY_DASHBOARD_REVERSE, ORG_UNIT_TREE_VIEW_ID } from "./constant";
import Search from "antd/lib/input/Search";
import { NO_OU_SEARCH_MSG } from "views/Pages/landing-page/constant";
import { useAppStore } from "contexts/AppStoreContext";
import { getIsStandaloneApp } from "helper/helper";
import { getIsDisabled } from "./OrgUnitTreeViewComponent.utils";
interface OrgUnitTreeViewProps {
  title: ReactNode;
  extra?: JSX.Element;
  ouGroupId: string;
  disableSelect?: boolean;
  loadDashboards?: (key: string, redirect?: string | number | undefined) => void;
  dashboard_id?: number;
  selectedNodes?: Array<string>;
  allowedOUs?: string[];
  setSelectedNodes?: (key: Array<string>) => void;
}
const { TreeNode } = Tree;

const OrgUnitTreeViewComponent: React.FC<OrgUnitTreeViewProps> = (props: OrgUnitTreeViewProps) => {
  const {
    title,
    extra,
    ouGroupId,
    loadDashboards,
    disableSelect,
    selectedNodes,
    setSelectedNodes = () => {},
    allowedOUs
  } = props;
  const dispatch = useDispatch();
  const [orgUnitListLoading, setOrgUnitListLoading] = useState<boolean>(false);
  const [rootOrgUnit, setRootOrgUnit] = useState<string>("");
  const [curParentId, setCurParentId] = useState<string>(ORG_UNIT_TREE_VIEW_ID);
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const [autoExpandParent, setAutoExpandParent] = useState(true);
  const [showMorePageMap, setShowMorePageMap] = useState<Record<string, number>>({});
  const [treeData, setTreeData] = useState<Array<any>>([]);
  const [searchValue, setSearch] = useState<string>("");
  const [hashTable, setHashTable] = useState<Record<any, any>>({});
  const isStandaloneApp = getIsStandaloneApp();
  const { selectedProject } = useAppStore();

  const onExpand = (newExpandedKeys: string[]) => {
    setExpandedKeys(newExpandedKeys);
  };

  const loadOuDashboards = (node: any, key: string, redirect: string | number | undefined = undefined) => {
    if (loadDashboards) {
      key = key || node?.props?.eventKey;
      loadDashboards(key, redirect);
    }
  };

  const orgUnitListState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: curParentId
  });

  const orgCategoryDashboardReverseState = useParamSelector(getGenericUUIDSelector, {
    uri: "organization_unit_management",
    method: "list",
    uuid: ORG_CATEGORY_DASHBOARD_REVERSE
  });

  const fetchData = (parentId?: string) => {
    const nShowMorePageMap = { ...showMorePageMap };
    if (parentId) {
      if (nShowMorePageMap.hasOwnProperty(parentId)) {
        nShowMorePageMap[parentId] = nShowMorePageMap[parentId] + 1;
      } else {
        nShowMorePageMap[parentId] = 0;
      }
      setCurParentId(parentId);
      setShowMorePageMap(nShowMorePageMap);
    }
    setOrgUnitListLoading(true);
    dispatch(
      genericList(
        "organization_unit_management",
        "list",
        {
          filter: {
            ou_category_id: [ouGroupId]
          }
        },
        null,
        parentId ?? curParentId
      )
    );
  };

  useEffect(() => {
    fetchData();
  }, []);

  useEffect(() => {
    if (orgUnitListLoading) {
      const loading = get(orgUnitListState, ["loading"], true);
      const error = get(orgUnitListState, ["error"], true);
      if (!loading) {
        if (!error) {
          const records = get(orgUnitListState, ["data", "records"], []);
          const hashMap = Object.create(null);
          let root: orgUnitJSONType = {};
          records.forEach((aData: orgUnitJSONType) => {
            hashMap[aData?.id || ""] = { ...aData, key: aData.id, title: aData.name, children: [] };
            if (!aData.parent_ref_id) {
              root = aData;
            }
          });
          setHashTable(hashMap);
          setTreeData(createDataTree(records, false));
          setAutoExpandParent(true);
          if (Object.keys(root).length > 0 && (isStandaloneApp || root.access_response?.view)) {
            setRootOrgUnit(root?.id || "");
            setExpandedKeys([root?.id || ""]);
            loadOuDashboards(undefined, root?.id || "");
          }
        }
        setOrgUnitListLoading(false);
      }
    }
  }, [orgUnitListState, orgUnitListLoading]);

  useEffect(() => {
    const reverseState = get(orgCategoryDashboardReverseState, ["data", "records"], undefined);
    if (reverseState?.length > 0 && orgUnitListState?.data?.records?.length > 0) {
      getOuDashboards();
    }
  }, [orgCategoryDashboardReverseState, orgUnitListState, orgUnitListLoading]);

  const getOuDashboards = async () => {
    let data: Array<orgUnitJSONType> = [];
    let ancestor: Record<string, orgUnitJSONType> = {};
    const reverseState = get(orgCategoryDashboardReverseState, ["data", "records"], undefined);
    if (rootOrgUnit && !reverseState.find((obj: orgUnitJSONType) => obj.id === rootOrgUnit)) {
      ancestor = dashboardLevelSearch(reverseState, orgUnitListState?.data?.records, hashTable, rootOrgUnit);
    }
    if (Object.keys(ancestor).length !== 0) {
      data = createSearchTree(ancestor);
    } else {
      data = createDataTree(orgUnitListState?.data?.records);
    }
    setTreeData(data);
  };
  const switchIcon = useMemo(() => <AntIcon type="caret-down" />, []);

  const onSelect = (selectedKeys: string[] = [], info: any) => {
    if (selectedKeys.length) {
      setSelectedNodes(selectedKeys);
    }
    const selected = getParentId(info?.node);
    if (!selected?.isShowMore) {
      loadOuDashboards(info?.node, selected?.parent, props?.dashboard_id);
    }
  };
  const createSearchTree = (ancestor: Record<string, orgUnitJSONType>) => {
    let searchedTreeData: Array<orgUnitJSONType> = [];
    if (Object.keys(ancestor).length !== 0) {
      const data = orgUnitListState?.data?.records.filter(
        (item: orgUnitJSONType) => ancestor[item?.id || ""] !== undefined || item.id === rootOrgUnit
      );
      searchedTreeData = createDataTree(data);
    }
    return searchedTreeData;
  };
  const onSearch = (value: any) => {
    let searchedTreeData: Array<orgUnitJSONType> = [];
    let ancestor: Record<string, orgUnitJSONType> = {};
    let hashCopy = cloneDeep(hashTable);
    if (value) {
      ancestor = getAncestor(orgUnitListState?.data?.records, value.toLocaleLowerCase(), hashCopy, rootOrgUnit);
      if (Object.keys(ancestor).length !== 0) {
        searchedTreeData = createSearchTree(ancestor);
      }
      ancestor[rootOrgUnit] = "" as any;
    } else {
      searchedTreeData = createDataTree(orgUnitListState?.data?.records);
      ancestor[rootOrgUnit] = "" as any;
    }
    setExpandedKeys(Object.keys(ancestor));
    setTreeData(searchedTreeData);
    setSearch(value);
  };

  const debouncedSearch = debounce(onSearch, 300);

  const renderTreeNodes = useCallback(
    (treeData, disabled = undefined) => {
      const reverseState = get(orgCategoryDashboardReverseState, ["data", "records"], undefined);
      return treeData.map((item: any) => {
        const index = item.title.toLowerCase().indexOf(searchValue.toLowerCase());
        const beforeStr = item.title.substr(0, index);
        const found = item.title.substr(index, searchValue.length);
        const afterStr = item.title.substr(index + searchValue.length);
        const title =
          index > -1 ? (
            <span>
              {beforeStr}
              <span style={{ color: "#f50" }}>{found}</span>
              {afterStr}
            </span>
          ) : (
            <span>{item.title}</span>
          );

        const isDisabled = getIsDisabled(item, disabled, reverseState, allowedOUs);
        if (item.children) {
          return (
            <TreeNode disabled={isDisabled} title={title} key={item.key}>
              {renderTreeNodes(item.children, isDisabled)}
            </TreeNode>
          );
        }
        return <TreeNode disabled={isDisabled} key={item.key} title={title} {...item} />;
      });
    },
    [treeData, searchValue]
  );

  return (
    <Card title={title} extra={extra} bordered={false}>
      {orgUnitListState?.loading && (
        <div className="flex align-center justify-center" style={{ width: "100%", height: "100%" }}>
          <Spin />
        </div>
      )}
      {!orgUnitListState?.loading && orgUnitListState?.data?.records?.length !== 0 && (
        <>
          <Search
            className="tree-search"
            allowClear
            placeholder="Search"
            onChange={(e: any) => {
              debouncedSearch(e.target.value);
            }}
          />
          {treeData.length === 0 &&
            (searchValue || !selectedProject ? (
              <div className="tree-search">{NO_OU_SEARCH_MSG}</div>
            ) : (
              <div className="tree-search">{`You do not have permission to access any collections within ${
                selectedProject?.name || ""
              } project. Please request access from the admin.`}</div>
            ))}
        </>
      )}
      {!orgUnitListState?.loading && (
        <Tree
          onExpand={onExpand}
          expandedKeys={expandedKeys}
          autoExpandParent={false}
          selectedKeys={selectedNodes}
          switcherIcon={switchIcon}
          showLine={true}
          className={cx("ou-tree-container", {
            "ou-tree-selected-node-container": !disableSelect,
            "no-events": !loadDashboards
          })}
          onSelect={onSelect}>
          {renderTreeNodes(treeData)}
        </Tree>
      )}
    </Card>
  );
};

export default OrgUnitTreeViewComponent;
