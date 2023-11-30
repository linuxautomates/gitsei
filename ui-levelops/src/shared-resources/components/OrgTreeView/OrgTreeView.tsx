import React, { useState, useEffect, useCallback } from "react";
import { ITreeNode, Tree } from "@blueprintjs/core";
import { transformCollectionsToTreeSctruct } from "./OrgTreeView.utils";

interface OrgTreeViewProps {
  searchTerm: string;
  selectedData: string[];
  onSelectChange: (items: string[]) => void;
  collections: any[];
}

function OrgTreeView({ searchTerm, selectedData, onSelectChange, collections }: OrgTreeViewProps): JSX.Element {
  const [navContent, setNavContent] = useState<ITreeNode<any>[]>([]);
  const [expandedNodes, setExpandedNodes] = useState<string[]>([]);
  const updateSelection = useCallback(
    (nodeId: string, action: "add" | "remove") => {
      let newSelection = [];
      if (action === "add") {
        newSelection = [...selectedData, nodeId];
      } else {
        newSelection = selectedData.filter(id => id !== nodeId);
      }
      onSelectChange(newSelection);
    },
    [selectedData, onSelectChange]
  );
  useEffect(
    () =>
      setNavContent(
        transformCollectionsToTreeSctruct(collections, selectedData, expandedNodes, updateSelection, searchTerm)
      ),
    [collections, expandedNodes, selectedData, updateSelection, searchTerm]
  );

  return (
    <Tree
      contents={navContent}
      onNodeExpand={node => {
        setExpandedNodes([...expandedNodes, node.id.toString()]);
      }}
      onNodeCollapse={node => {
        setExpandedNodes(expandedNodes.filter(expandedNodeId => expandedNodeId !== node.id.toString()));
      }}
    />
  );
}

export default OrgTreeView;
