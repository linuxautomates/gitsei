import React, { useCallback, useMemo } from "react";
import { AutoComplete, Icon, Tree, TreeSelect } from "antd";
import { convertArrayToTree } from "../custom-tree-select/helper";
import { AntBadge, AntInput, AntTag } from "..";
import classNames from "classnames";
import { useState } from "react";
import { forEach, get } from "lodash";
import "./OrgTree.scss";

const { TreeNode } = Tree;
const { SHOW_ALL } = TreeSelect;
export interface PopupTreeProps {
  dataSource: any[];
  onCheck: (...args: any) => any;
  selectedRowsKeys: any[];
  addbuttonHandler?: (value: string) => void;
  label: string;
  notFoundContent?: any;
  flatList?: any[];
}

const OrgTree: React.FC<PopupTreeProps> = props => {
  const [searchValue, setSearchValue] = useState<string>("");
  const [expandedKeys, setExpandedKeys] = useState<string[]>([]);
  const { selectedRowsKeys = [], dataSource, notFoundContent, flatList } = props;
  let textInput: any = null;
  const filteredData = useMemo(() => {
    let newExpandedKeys: string[] = [];
    let resultantTree = dataSource;
    if (!searchValue) {
      newExpandedKeys = resultantTree.length ? [resultantTree[0].key] : [];
    } else {
      newExpandedKeys = resultantTree.length ? [...resultantTree.map(data => data?.key)] : [];
    }
    // setExpandedKeys(newExpandedKeys);
    return resultantTree;
  }, [dataSource, searchValue]);

  const selectedKeys = useMemo(() => {
    return flatList?.reduce((acc, ou) => {
      if (selectedRowsKeys.indexOf(ou?.id) !== -1) {
        acc.push({ label: ou?.name, key: ou?.id, value: ou?.id });
      }
      return acc;
    }, []);
  }, [selectedRowsKeys]);

  return (
    <div data-testid="org-tree-select">
      <div className="org-tree-container">
        <TreeSelect
          className="org-tree"
          treeCheckable={true}
          showSearch
          treeNodeFilterProp="title"
          onChange={(checkedKeys: any) => props.onCheck(checkedKeys?.map((val: any) => val?.value))}
          value={(selectedKeys as any) || []}
          treeData={filteredData}
          showCheckedStrategy={SHOW_ALL}
          notFoundContent={notFoundContent}
          maxTagCount={2}
          placeholder={"Please select"}
          ref={button => {
            textInput = button;
          }}
          treeCheckStrictly={true}
        />
        <a onClick={() => textInput?.rcTreeSelect?.setOpenState(true)} className="edit-icon">
          Edit
        </a>
      </div>
    </div>
  );
};

export default OrgTree;
