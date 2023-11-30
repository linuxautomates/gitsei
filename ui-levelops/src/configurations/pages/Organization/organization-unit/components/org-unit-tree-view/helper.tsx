import React from "react";
import { orgUnitJSONType } from "configurations/configuration-types/OUTypes";
import { get } from "lodash";

export const getParentId = (node: any) => {
  let parent = get(node, ["props", "eventKey"], null);
  let isShowMore = false;
  if (parent && parent.includes("show-more")) {
    parent = parent.split("_")[0];
    isShowMore = true;
  }
  return { parent, isShowMore };
};
export const createDataTree = (dataset: Array<orgUnitJSONType>, reverseState: boolean = false, hashTable: any = {}) => {
  dataset.forEach(
    (aData: orgUnitJSONType) =>
      (hashTable[aData?.id || ""] = { ...aData, key: aData.id, value: aData.id, title: aData.name, children: [] })
  );
  const dataTree: Array<orgUnitJSONType> = [];
  dataset.forEach((aData: orgUnitJSONType) => {
    if (aData.parent_ref_id) {
      if (hashTable[aData?.parent_ref_id]?.children) {
        hashTable[aData?.parent_ref_id]?.children.push(hashTable[aData?.id || ""]);
      }
    } else {
      dataTree.push(hashTable[aData?.id || ""]);
    }
  });
  return dataTree;
};

export const getParentKey = (
  key: React.Key,
  parents: any = [],
  hashTable: Record<string, any>,
  rootId: string
): any => {
  if (hashTable[key] && hashTable[key]?.id !== rootId) {
    parents.push(hashTable[key]?.id);
    getParentKey(hashTable[key]?.parent_ref_id, parents, hashTable, rootId);
  }
  return parents;
};

/**
 * @function getAncestor
 *
 * @params records - All Records
 * @params value -  Search string
 * @params hashtable - object maintianing id as keys - hashtable helps in searching, as object search is having O(1) time complexcity
 * @params rootId - recursive function when we find rootId in tree
 *
 * @returns parents variable object - which contains all the parents id's from which found in search helps to EXPAND tree keys it's object as it's helps searching while creating tree
 * */
export const getAncestor = (
  records: Array<orgUnitJSONType>,
  value: string,
  hashTable: Record<string, any>,
  rootId: string
) => {
  const parents: Record<string, any> = {};
  records.forEach((item: Record<string, any>) => {
    if (item && item.name.toLocaleLowerCase().indexOf(value) > -1) {
      if (parents[item.id] === undefined || parents[item.parent_ref_id] === undefined) {
        getParentKey(item.parent_ref_id, [item.id], hashTable, rootId).reduce(
          (acc: Record<string, any>, curr: any) => ((acc[curr] = ""), acc),
          parents
        );
      }
    }
  });
  return parents;
};

export const dashboardLevelSearch = (
  reverseState: Array<orgUnitJSONType>,
  records: Array<orgUnitJSONType>,
  hashTable: Record<string, any>,
  rootId: string
) => {
  let ancestor: Record<string, any> = {};
  records.forEach((item: any) => {
    if (reverseState.find((revState: orgUnitJSONType) => revState.id === item.id)) {
      ancestor = getAncestor(records, `/${item.name}`, hashTable, rootId);
    }
  });
  return ancestor;
};
