import { toTitleCase } from "../../../utils/stringUtils";
import { flatten } from "lodash";

const getParentNodeFromChildNodes = (parent: any, childNodes: string[]): any => {
  if (!parent) {
    return null;
  }
  if (childNodes.length === 1) {
    return parent.children?.find((node: any) => node.key === childNodes[0]);
  }
  const newParent = parent.children?.find((node: any) => node.key === childNodes[0]);
  return getParentNodeFromChildNodes(newParent, childNodes.slice(1));
};

export const convertArrayToTree = (data: any[]) => {
  let arrMap = new Map(
    data
      .filter(item => !item.parent_key)
      .map(item => [
        item.value,
        {
          ...item,
          title: item.key || item.value,
          key: item.key || item.value
        }
      ])
  );
  let tree = [];

  for (let i = 0; i < data.length; i++) {
    let item = data[i];
    const updatedItem = {
      ...item,
      title: item.key || item.value,
      key: item.key || item.value
    };
    if (item.parent_key) {
      let parentItem = arrMap.get(item.parent_key);
      if (parentItem) {
        let { children } = parentItem;
        const isAlreadyPresent = children && children.find((child: any) => child.value === item.value);
        if (!isAlreadyPresent) {
          if (children) {
            parentItem.children.push(updatedItem);
          } else {
            parentItem.children = [updatedItem];
          }
        }
      } else {
        // Check if any child element is parent of item
        const parentNodes = item.parent_key.split("\\");
        if (parentNodes.length > 1 && arrMap.get(parentNodes[0])) {
          const final_parent = getParentNodeFromChildNodes(arrMap.get(parentNodes[0]), parentNodes.slice(1));
          if (final_parent) {
            let { children } = final_parent;
            const isAlreadyPresent = children && children.find((child: any) => child.value === item.value);
            if (!isAlreadyPresent) {
              if (children) {
                final_parent.children.push(updatedItem);
              } else {
                final_parent.children = [updatedItem];
              }
            }
          } else {
            const newItem: any = {
              key: item.parent_key,
              value: item.parent_key,
              title: item.parent_key
            };
            newItem.children = [updatedItem];
            arrMap.set(item.parent_key, newItem);
            tree.push(newItem);
          }
        } else {
          const newItem: any = {
            key: item.parent_key,
            value: item.parent_key,
            title: item.parent_key
          };
          newItem.children = [updatedItem];
          arrMap.set(item.parent_key, newItem);
          tree.push(newItem);
        }
      }
    } else {
      const _item = arrMap.get(item.value);
      tree.push(_item);
    }
  }
  return tree;
};

export const convertTreeToArray = (data: any, parent_key?: string) => {
  const arr = data.map((item: any) => {
    const key = parent_key ? `${parent_key}/${item.key}` : item.key;
    const updatedData = {
      ...item,
      key: key
    };
    if (item.children) {
      return [updatedData, ...convertTreeToArray(item.children, key)];
    }
    return [updatedData];
  });
  return flatten(arr);
};
