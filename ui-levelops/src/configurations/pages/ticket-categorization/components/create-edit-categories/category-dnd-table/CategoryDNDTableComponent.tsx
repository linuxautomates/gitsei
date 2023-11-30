import React, { useState, useCallback, useEffect, useRef } from "react";
import { DndProvider } from "react-dnd";
import HTML5Backend from "react-dnd-html5-backend";
import update from "immutability-helper";
import CategoryDNDSortingRowComponent from "./CategoryDNDTableRowComponent";
import { isEqual } from "lodash";
import { List } from "antd";
import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";

interface CategoryDNDSortingTableProps {
  profile: RestTicketCategorizationScheme;
  categories: any[];
  handleCategoriesUpdate: (updatedCategories: any[]) => void;
}
const CategoryDNDSortingTable: React.FC<CategoryDNDSortingTableProps> = ({
  categories,
  profile,
  handleCategoriesUpdate
}) => {
  const [categoriesData, setCategoryData] = useState(categories);
  const prevCategoriesData = useRef<any[]>(categoriesData);

  useEffect(() => {
    if (!isEqual(categories, categoriesData)) {
      prevCategoriesData.current = categories;
      setCategoryData(categories);
    }
  }, [categories]);

  useEffect(() => {
    if (!isEqual(prevCategoriesData.current, categoriesData)) {
      prevCategoriesData.current = categoriesData;
      handleCategoriesUpdate(categoriesData);
    }
  }, [categoriesData]);

  const moveRow = useCallback(
    (dragIndex, hoverIndex) => {
      const dragRow = categoriesData[dragIndex];
      setCategoryData(
        update(categoriesData, {
          $splice: [
            [dragIndex, 1],
            [hoverIndex, 0, dragRow]
          ]
        })
      );
    },
    [categoriesData]
  );

  return (
    <DndProvider backend={HTML5Backend}>
      <List
        dataSource={categoriesData}
        renderItem={(item, index) => (
          <CategoryDNDSortingRowComponent index={index} record={item} moveRow={moveRow} profile={profile} />
        )}
      />
    </DndProvider>
  );
};

export default CategoryDNDSortingTable;
