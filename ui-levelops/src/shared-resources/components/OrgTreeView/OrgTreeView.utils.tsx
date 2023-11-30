import React from "react";
import { ITreeNode } from "@blueprintjs/core";
import { Checkbox } from "@harness/uicore";
import "./OrgTreeView.scss";

const transformOrgToTreeNode = (
  org: any,
  selectedNodes: string[],
  expandedNodes: string[],
  updateSelection: (nodeId: string, action: "add" | "remove") => void
): ITreeNode<any> => {
  const isNodeSelected = selectedNodes.includes(org.id);
  const isExpanded = expandedNodes.includes(org.id);
  const nodeData = {
    ...org,
    updateSelection
  };
  return {
    id: org.id,
    isExpanded,
    label: (
      <Checkbox
        labelElement={org.name}
        className={"checkbox"}
        checked={isNodeSelected}
        onChange={e => {
          updateSelection(org.id, e.currentTarget.checked ? "add" : "remove");
        }}
      />
    ),
    // disabled: allowedOUs && allowedOUs.length ? !allowedOUs.includes(org?.id) : false,
    nodeData
  };
};

const getAllChildren = (root: ITreeNode<any>, childs: ITreeNode<any>[]): ITreeNode<any>[] | undefined => {
  if (!root || childs?.length === 0 || root === undefined) {
    return undefined;
  }
  const rootChilds = childs
    .filter((ou: ITreeNode<any>) => ou.nodeData.parent_ref_id == root.id)
    .sort(stringSortComparer);
  const nonrootChilds = childs.filter((ou: ITreeNode<any>) => ou.nodeData.parent_ref_id != root.id);
  const newRoot = { ...root };
  newRoot.childNodes = rootChilds;
  (newRoot.childNodes || [])?.forEach((element: ITreeNode<any>) => {
    const children = getAllChildren(element, nonrootChilds);
    if (children && children?.length) {
      element.childNodes = children;
    }
  });

  return newRoot.childNodes;
};

const stringSortComparer = (a: ITreeNode<any>, b: ITreeNode<any>): number => {
  const key1 = a.nodeData.name.toString().toLocaleLowerCase();
  const key2 = b.nodeData.name.toString().toLocaleLowerCase();
  if (key1 < key2) return -1;
  if (key1 > key2) return 1;
  return 0;
};

const orgSearchFilter = (org: any, searchTerm: any) => {
  return org.name.toLocaleLowerCase().includes(searchTerm.toLocaleLowerCase());
};

export const transformCollectionsToTreeSctruct = (
  orgs: any[],
  selectedNodes: string[],
  expandedNodes: string[],
  updateSelection: (nodeId: string, action: "add" | "remove") => void,
  searchTerm?: string
): ITreeNode<any>[] => {
  const filteredOUs = searchTerm ? orgs.filter(org => orgSearchFilter(org, searchTerm)) : orgs;
  const transformedOU = filteredOUs.map(org =>
    transformOrgToTreeNode(org, selectedNodes, expandedNodes, updateSelection)
  );
  const allRootOUs = transformedOU.filter((ou: ITreeNode<any>) => !ou.nodeData.parent_ref_id).sort(stringSortComparer);
  const filteredChildOUS = transformedOU.filter((ou: ITreeNode<any>) => ou.nodeData.parent_ref_id);
  const treeNodes = allRootOUs.map((RootOu: ITreeNode<any>) => {
    const children = getAllChildren(RootOu, filteredChildOUS);
    if (children && Array.isArray(children) && children?.length) {
      RootOu.childNodes = children.sort(stringSortComparer);
    }
    return RootOu;
  });
  return treeNodes;
};
