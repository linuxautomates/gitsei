import React, { useCallback, useMemo } from "react";
import { AutoComplete, Tree } from "antd";
import { convertArrayToTree } from "../custom-tree-select/helper";
import { AntBadge, AntInput, AntTag } from "..";
import classNames from "classnames";
import { useState } from "react";
import { forEach, get } from "lodash";
import { SELECT_NAME_TYPE_ITERATION } from "dashboard/components/dashboard-application-filters/AddFiltersComponent/filterConstants";

const { TreeNode } = Tree;

export interface PopupTreeProps {
  dataSource: any[];
  onSelectionChange: (...args: any) => any;
  selectedRowsKeys: any[];
  addbuttonHandler?: (value: string) => void;
  label: string;
}

const PopupTree: React.FC<PopupTreeProps> = props => {
  const [searchValue, setSearchValue] = useState<string>("");
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const { selectedRowsKeys, dataSource } = props;
  const isAzureIterationFilter = SELECT_NAME_TYPE_ITERATION === props.label?.toLowerCase();

  const filteredData = useMemo(() => {
    let newDataSource = (dataSource || []).filter(
      data =>
        (data?.parent_key || "").toLowerCase().includes((searchValue || "").toLowerCase()) ||
        (data?.value || "").toLowerCase().includes((searchValue || "").toLowerCase())
    );
    let newExpandedKeys: string[] = [];
    let resultantTree = convertArrayToTree(newDataSource || []);
    if (!searchValue) {
      newExpandedKeys = resultantTree.length ? [resultantTree[0].key] : [];
    } else {
      newExpandedKeys = resultantTree.length ? [...resultantTree.map(data => data?.key)] : [];
    }
    setExpandedKeys(newExpandedKeys);
    return resultantTree;
  }, [dataSource, searchValue]);

  const onExpand = useCallback((expandedKeys: string[]) => {
    setExpandedKeys(expandedKeys);
  }, []);

  const onCheck = useCallback((checkedKeys: any, e: any) => {
    const checkedNodes = get(e, ["checkedNodes"], []);
    const arrayofParentChild = checkedNodes
      .map((next: any) => {
        const nodeProps = get(next, ["props"], {});
        if (nodeProps.hasOwnProperty("parent_key")) {
          return { parent: nodeProps["parent_key"], child: nodeProps["value"] };
        }
        return undefined;
      })
      .filter((item: any) => !!item);
    props.onSelectionChange && props.onSelectionChange(arrayofParentChild);
  }, []);

  const selectedKeys = useMemo(() => {
    return selectedRowsKeys.map(key => {
      if (typeof key === "string") {
        return key;
      }
      if (props.label.toLowerCase() === "azure areas") {
        return key.child;
      }
      return `${key.parent}\\${key.child}`;
    });
  }, [selectedRowsKeys]);

  const handleAzureIterationSelection = (checkedKeys: any) => {
    props.onSelectionChange && props.onSelectionChange(checkedKeys?.checked ?? []);
  };

  const getFirstChildNode = useCallback(
    (parentKey: string) => {
      const parentNode = filteredData.find(node => node.key === parentKey);
      if (parentNode) {
        const children: { key: string }[] = get(parentNode, ["children"], []);
        if (children.length) {
          return children;
        }
        return undefined;
      }
      return undefined;
    },
    [filteredData]
  );

  const handleChildSelect = (childKeys: string[]) => {
    props.onSelectionChange?.([...selectedKeys, ...childKeys]);
  };

  const handleDeSelectChild = (childKeys: string[]) => {
    props.onSelectionChange?.(selectedKeys.filter(key => !childKeys.includes(key)));
  };

  const isFirstChildrenSelected = useCallback(
    (childKeys: string[]) => {
      let selected = true;
      forEach(childKeys, key => {
        selected &&= selectedKeys.includes(key);
      });
      return selected;
    },
    [selectedKeys]
  );

  const renderTreeNodeTitle = useCallback(
    (title: string, key: string, parentPath: string) => {
      if (isAzureIterationFilter) {
        const firstChilds = getFirstChildNode(key) ?? [];
        const firstChildKeys = firstChilds?.map(firstChild => `${parentPath}\\${firstChild?.key}`) ?? [];
        const selectedFirstChildren = firstChildKeys?.length ? isFirstChildrenSelected(firstChildKeys) : false;

        return (
          <div className="custom-tree-node">
            <span>{title}</span>
            <span
              className="custom-tree-node__child-selection"
              onClick={e =>
                selectedFirstChildren ? handleDeSelectChild(firstChildKeys) : handleChildSelect(firstChildKeys)
              }>
              {selectedFirstChildren ? "- Deselect child nodes" : "+ Select child nodes"}
            </span>
          </div>
        );
      }
      return title;
    },
    [isAzureIterationFilter, isFirstChildrenSelected, selectedKeys, filteredData]
  );

  const renderTreeNodes = useCallback(
    (data: any[], parentSprint?: string) =>
      data.map(item => {
        const key = parentSprint ? `${parentSprint}\\${item.key}` : item.key;
        if (item.children) {
          return (
            <TreeNode title={renderTreeNodeTitle(item?.title, item?.key, key)} key={key} dataRef={item}>
              {renderTreeNodes(item.children, key)}
            </TreeNode>
          );
        }
        return <TreeNode {...item} key={key} />;
      }),
    [renderTreeNodeTitle]
  );

  return (
    <div>
      <div className="popup-tree-header">
        <div className="flex align-center">
          <AntTag color={(selectedKeys || []).length === 0 ? "" : "blue"}>{`${
            (selectedKeys || []).length
          } Selected`}</AntTag>
          <AntBadge
            overflowCount={(filteredData || []).length}
            count={(filteredData || []).length}
            className={`total_count_badge ${classNames({ "mr-1": 2 < 9 }, { "mr-2": !(2 < 9) })}`}
            style={{ backgroundColor: "rgb(46, 109, 217)", zIndex: "3" }}
          />
        </div>
        <AutoComplete
          dataSource={[]}
          placeholder="Search"
          value={searchValue}
          filterOption={true}
          className="auto-complete-pop-tree"
          onSearch={(value: string) => {
            setSearchValue(value);
          }}>
          <AntInput className="search-field" type="search" />
        </AutoComplete>
      </div>
      <div className="popup-tree-container">
        <Tree
          className="popup-tree"
          multiple
          checkable
          checkStrictly={isAzureIterationFilter ? true : false}
          onExpand={onExpand}
          expandedKeys={expandedKeys}
          onCheck={isAzureIterationFilter ? handleAzureIterationSelection : onCheck}
          checkedKeys={selectedKeys || []}>
          {renderTreeNodes(filteredData)}
        </Tree>
      </div>
    </div>
  );
};

export default PopupTree;
