import React, { useRef } from "react";
import { useDrag, useDrop } from "react-dnd";
import { PropTypes } from "prop-types";

import { SvgIconComponent } from "../svg-icon/svg-icon.component";
import "./reorderable-list-item.style.scss";

const ReorderableListItem = props => {
  const row = useRef(null);
  const { id, index, moveCard, onDrag, onDrop, draggedItem, kids } = props;

  const [, drop] = useDrop({
    accept: "card",
    hover(item, monitor) {
      if (!row.current) {
        return;
      }
      const dragIndex = item.index;
      const hoverIndex = index;
      // Don't replace items with themselves
      if (dragIndex === hoverIndex) {
        return;
      }

      const hoverBoundingRect = row.current.getBoundingClientRect();
      const hoverMiddleY = (hoverBoundingRect.bottom - hoverBoundingRect.top) / 2;
      const clientOffset = monitor.getClientOffset();
      const hoverClientY = clientOffset.y - hoverBoundingRect.top;

      //return if it's not dragged past halfway
      if (dragIndex < hoverIndex && hoverClientY < hoverMiddleY) {
        return;
      }
      if (dragIndex > hoverIndex && hoverClientY > hoverMiddleY) {
        return;
      }

      moveCard(dragIndex, hoverIndex);
      onDrag(item.id);
      item.index = hoverIndex;
    },
    drop(item) {
      onDrop(item.id);
    }
  });

  const [{ isDragging }, drag] = useDrag({
    item: { type: "card", index, id },
    collect: monitor => ({
      isDragging: monitor.isDragging()
    })
  });

  //when a card is being dragged, hide it temporarily (the preview will be visible)
  const opacity = isDragging ? 0 : 1;

  drag(drop(row));

  return (
    <div ref={row} style={{ opacity }} className={"row" + (draggedItem ? "" : " activeRow")}>
      <span className="handle">
        <SvgIconComponent icon="dragHandle" />
      </span>
      {/* {props.id + '.)  '}    */}
      {kids.filter(kid => kid.key == id)}
    </div>
  );
};

ReorderableListItem.propTypes = {
  id: PropTypes.number.isRequired, // the actual id of the card
  index: PropTypes.number.isRequired, // the current position of the card in the list
  moveCard: PropTypes.func.isRequired
};

export default ReorderableListItem;
