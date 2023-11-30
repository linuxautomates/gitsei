import React, { useState, useCallback } from "react";
import { DndProvider } from "react-dnd";
import Backend from "react-dnd-html5-backend";
import { PropTypes } from "prop-types";
import { List } from "antd";

import ReorderableListItem from "./reorderable-list-item.component";

const ReorderableList = props => {
  // Destructure to remove stuff
  const { moveCard: moveCardProp, ...otherProps } = props;

  // `draggedItem` holds the id of the item currently being dragged, if any. It's `null` otherwise.
  // this is used for setting styles
  const [draggedItem, setDraggedItem] = useState(null);

  // eslint-disable-next-line react-hooks/exhaustive-deps
  const moveCard = useCallback((dragIndex, hoverIndex) => {
    props.moveCard(dragIndex, hoverIndex);
    // const dragCard = cards[dragIndex];
    // const newCards = update(cards, {
    //     $splice: [
    //         [dragIndex, 1],
    //         [hoverIndex, 0, dragCard],
    //     ],
    // });
    // setCards(
    //     newCards,
    // );
    // props.onReorder(newCards);
  });

  const onDrag = id => {
    setDraggedItem(id);
  };

  const onDrop = () => {
    setDraggedItem(null);
  };

  return (
    <DndProvider backend={Backend}>
      <List
        {...otherProps}
        renderItem={(item, index) => {
          return (
            <ReorderableListItem
              key={index}
              id={index}
              index={index}
              moveCard={moveCard}
              onDrag={onDrag}
              onDrop={onDrop}
              draggedItem={draggedItem}>
              {props.renderItem(item, index)}
            </ReorderableListItem>
          );
        }}
      />
    </DndProvider>
  );
};

ReorderableList.propTypes = {
  //itemIds: PropTypes.arrayOf(PropTypes.number).isRequired,
  onReorder: PropTypes.func
};

export default ReorderableList;
