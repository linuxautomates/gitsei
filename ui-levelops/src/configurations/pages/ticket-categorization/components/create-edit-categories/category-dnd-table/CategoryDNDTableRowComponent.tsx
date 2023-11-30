import { RestTicketCategorizationScheme } from "classes/RestTicketCategorizationScheme";
import React, { useRef } from "react";
import { useDrag, useDrop } from "react-dnd";
import CategoryCard from "../../scheme-drilldown/category-card/CategoryCard";
import "./category-dnd-row.styles.scss";
const type = "DragableBodyRow";

interface CategoryDNDSortingRowComponentProps {
  profile: RestTicketCategorizationScheme;
  index: number;
  record: any;
  moveRow: (dragIndex: any, hoverIndex: any) => void;
}
const CategoryDNDSortingRowComponent: React.FC<CategoryDNDSortingRowComponentProps> = ({
  index,
  record,
  profile,
  moveRow
}) => {
  const ref = useRef(null);

  const [, drop] = useDrop({
    accept: type,
    collect: (monitor: any) => {
      const { index: dragIndex } = monitor.getItem() || {};
      if (dragIndex === index) {
        return {};
      }
      return {
        isOver: monitor.isOver()
      };
    },
    drop: (item: any) => {
      moveRow(item.index, index);
    }
  });

  const [, drag] = useDrag({
    item: { type, index },
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  });

  drop(drag(ref));

  return (
    <div ref={ref} key={record?.id || record?.name} className={`dnd-row `}>
      <CategoryCard category={record} rank={index} profile={profile} />
    </div>
  );
};

export default CategoryDNDSortingRowComponent;
