import React, { useState, useCallback, useEffect } from "react";
import { DndProvider } from "react-dnd";
import Backend from "react-dnd-html5-backend";
import update from "immutability-helper";
import { concat, difference, pullAll } from "lodash";
import { PropTypes } from "prop-types";

import ReorderableListItem from "./reorderable-list-item.component";

const ReorderableList = ({ itemIds, onReorder, children }) => {
  //`cards` is an array of numeric IDs.
  //`setCards()` is used to update it when needed.
  const [cards, setCards] = useState(itemIds);

  // `draggedItem` holds the id of the item currently being dragged, if any. It's `null` otherwise.
  // this is used for setting styles
  const [draggedItem, setDraggedItem] = useState(null);

  const moveCard = useCallback((dragIndex, hoverIndex) => {
    const dragCard = cards[dragIndex];
    const newCards = update(cards, {
      $splice: [
        [dragIndex, 1],
        [hoverIndex, 0, dragCard]
      ]
    });
    setCards(newCards);
    onReorder(newCards);
  });

  const calculateNewOrder = (oldIds, newIds) => {
    const oldLen = oldIds.length;
    const newLen = newIds.length;
    if (oldLen == newLen) {
      return oldIds;
    } else if (oldLen > newLen) {
      // questions that got removed are pulled
      let removed = difference(oldIds, newIds);
      return pullAll(oldIds, removed);
    } else {
      // new questions that got added are annexed to the end
      let added = difference(newIds, oldIds);
      return concat(oldIds, added);
    }
  };

  // whenever `items` is updated, (ie, ADD button is clicked), update the cards
  useEffect(() => {
    let newOrder = calculateNewOrder(cards, itemIds);
    setCards([...newOrder]);
    //this.props.onReorder(newOrder)
  }, [itemIds]);

  const onDrag = id => {
    setDraggedItem(id);
  };

  const onDrop = () => {
    setDraggedItem(null);
  };

  return (
    <DndProvider backend={Backend}>
      <div style={{ flexBasis: "40%", paddingRight: "20px" }}>
        {cards.map((cardId, i) => (
          <ReorderableListItem
            key={cardId}
            id={cardId}
            index={i}
            moveCard={moveCard}
            onDrag={onDrag}
            onDrop={onDrop}
            draggedItem={draggedItem}
            kids={children}
          />
        ))}
      </div>
    </DndProvider>
  );
};

ReorderableList.propTypes = {
  itemIds: PropTypes.arrayOf(PropTypes.number).isRequired,
  onReorder: PropTypes.func.isRequired
};

export default ReorderableList;
